bus.sendMessage("DeskChan:say", {'text': 'Something...'})
bus.sendMessage("DeskChan:register-simple-action", {'name': 'Test', 'msgTag': "test_python:test"})

bus.addMessageListener("test_python:test", lambda sender, tag, data:
    bus.sendMessage("DeskChan:say", {'text': 'Finally, it works!'})
)