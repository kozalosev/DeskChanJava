addMessageListener("gui-events:character-scroll", { sender, tag, data ->
    sendMessage("gui:resize-character", [zoom: (float) -data['delta'] / 10])
})
