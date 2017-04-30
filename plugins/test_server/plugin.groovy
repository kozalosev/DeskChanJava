final def PLUGIN_TAG = 'test_server'

// Helper function
def say(text) {
    sendMessage("DeskChan:say", [text: text.toString()])
}


// Listeners
addMessageListener("$PLUGIN_TAG:greetings", { sender, tag, data ->
    say("О! Так вот как тебя зовут по-настоящему, ${data['name']}!")
})

addMessageListener("$PLUGIN_TAG:curiosity", { sender, tag, data ->
    say("${data['name']}... А кто это?")
})
