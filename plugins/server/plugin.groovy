import org.json.JSONException
import org.json.JSONObject

import javax.xml.bind.DatatypeConverter
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.regex.Pattern


final int DEFAULT_PORT = 6867   // ASCII code for "DC"
final int SPARE_PORT = 10099    // dc


ServerSocket serverSocket
addCleanupHandler({ -> serverSocket?.close() })

new Thread({ ->

    if ((serverSocket = tryGetSocket(DEFAULT_PORT)) == null)
        if ((serverSocket = tryGetSocket(SPARE_PORT)) == null)
            return

    log "Starting listening on port ${serverSocket.getLocalPort()}..."
    while (true) {
        // The SocketException will be thrown when we close the socket while the unloading process.
        try {
            serverSocket.accept { clientSocket ->
                log "Accept a connection from ${clientSocket.getLocalSocketAddress()}"

                clientSocket.withStreams { inputStream, outputStream ->
                    BufferedReader reader = inputStream.newReader()
                    String line = reader.readLine()

                    // If we encountered with a WebSocket, we need to perform a handshake and decode its request.
                    if (line.substring(0, 3) == "GET") {
                        websocketHandshake(reader, outputStream)
                        line = websocketDecode(inputStream)
                    }

                    if (line == null) {
                        log 'Empty request!'
                        return
                    }

                    JSONObject jsonRoot
                    String msgTag
                    Map<String, Object> msgData
                    try {
                        jsonRoot = new JSONObject(line)
                        msgTag = jsonRoot.getString('msgTag')
                        msgData = jsonRoot.getJSONObject('msgData').toMap()
                    } catch (JSONException e) {
                        log "Wrong JSON: $line"
                        e.printStackTrace()
                        return
                    }

                    sendMessage(msgTag, msgData)

                    log "Connection with ${clientSocket.getLocalSocketAddress()} is closed"
                }
            }
        } catch (SocketException e) {
            break
        }
    }
    log 'Listening is stopped'

}).start()


ServerSocket tryGetSocket(int port) {
    ServerSocket serverSocket = null
    try {
        serverSocket = new ServerSocket(port)
    } catch (BindException e) {
        log "Failed to connect to port $port"
    }
    return serverSocket
}

// https://developer.mozilla.org/en-US/docs/Web/API/WebSockets_API/Writing_a_WebSocket_server_in_Java
static void websocketHandshake(BufferedReader reader, OutputStream outputStream) {
    String line, websocketKey
    while ((line = reader.readLine()) != null) {
        def matcher = Pattern.compile('Sec-WebSocket-Key:\\s*(\\S+)').matcher(line)
        if (matcher.matches()) {
            websocketKey = matcher.group(1)
        }

        if (line == '' || line == '\r\n')
            break
    }

    if (websocketKey == null)
        throw new ProtocolException("Unknown protocol")

    def response = ("HTTP/1.1 101 Switching Protocols\r\n"
            + "Connection: Upgrade\r\n"
            + "Upgrade: websocket\r\n"
            + "Sec-WebSocket-Accept: "
            + DatatypeConverter
            .printBase64Binary(
            MessageDigest
                .getInstance("SHA-1")
                .digest((websocketKey + "258EAFA5-E914-47DA-95CA-C5AB0DC85B11")
                .getBytes("UTF-8")))
            + "\r\n\r\n")

    def writer = outputStream.newPrintWriter()
    writer << response
    writer.flush()
}

// http://stackoverflow.com/a/8125509
// https://tools.ietf.org/html/draft-ietf-hybi-thewebsocketprotocol-17#section-5.2https://tools.ietf.org/html/draft-ietf-hybi-thewebsocketprotocol-17#section-5.2
String websocketDecode(InputStream inputStream) {
    def dataStream = new DataInputStream(new BufferedInputStream(inputStream))

    int fistByte = dataStream.readUnsignedByte()
    if (fistByte != 129) {
        log "Wrong format! 129 had been expected but $fistByte was gotten"
        return null
    }

    def length = dataStream.readUnsignedByte() & 127
    if (length == 126)
        length = dataStream.readUnsignedShort()
    else if (length == 127)
        length = dataStream.readLong()

    byte[] mask = [dataStream.readByte(), dataStream.readByte(), dataStream.readByte(), dataStream.readByte()]
    byte[] decoded = new byte[length]
    for (def i = 0; i < length; i++)
        decoded[i] = dataStream.readUnsignedByte() ^ mask[i % 4]

    return new String(decoded, StandardCharsets.UTF_8)
}
