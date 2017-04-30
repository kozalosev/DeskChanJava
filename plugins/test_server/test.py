#!python

def try_connect(port):
    import socket

    sock = None
    try:
        sock = socket.create_connection(('localhost', port))
    except socket.error:
        print "Couldn't connect to port %i." % port
    return sock

sock = try_connect(6867)
if not sock:
    sock = try_connect(10099)
if not sock:
    print "The server may be not running!"
    exit()

sock.send('{"msgTag": "DeskChan:say", "msgData": {"text": "Hello, world!"}}\n')
data = sock.recv(1024)
sock.close()
print data
