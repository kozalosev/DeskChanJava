addMessageListener("gui-events:character-scroll", { sender, tag, data ->
    sendMessage("gui:change-skin-opacity", [relative: (float) -data['delta'] / 10])
})