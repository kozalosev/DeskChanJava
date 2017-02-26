import classes.CharacterManager
import classes.Character

import javax.swing.Timer
import java.awt.event.ActionEvent

Character character = CharacterManager.getRandomCharacter()

Timer skinUpdateTimer = new Timer(3600000, { ActionEvent actionEvent ->
    if (character.reloadRequired()) {
        sendMessage('gui:change-skin', character.getSkin())
        character.reloadPhrases()
        sendMessage('DeskChan:say', [text: character.getWelcomePhrase()])
    }
})
skinUpdateTimer.initialDelay = 0

Timer messageShowTimer = new Timer(600000, { ActionEvent actionEvent ->
    String phrase = character.getRandomPhrase()
    if (phrase != null && phrase != "")
        sendMessage('DeskChan:say', [text: phrase])
})

skinUpdateTimer.start()
messageShowTimer.start()

addCleanupHandler({
    skinUpdateTimer.stop()
    messageShowTimer.stop()
})


addMessageListener('character_manager:feed', { sender, tag, data ->
    sendMessage('DeskChan:say', [text: character.feed()])
})

addMessageListener('character_manager:naughty', { sender, tag, data ->
    sendMessage('DeskChan:say', [text: character.doNaughtyThings()])
})

addMessageListener('gui-events:character-left-click', { sender, tag, data ->
    sendMessage('DeskChan:say', [text: character.getClickPhrase()])
})

addMessageListener('character_manager:about', {sender, tag, data ->
    BrowserAdapter.openWebpage("https://2ch.hk/s/res/1936557.html")
})

sendMessage('DeskChan:register-simple-action', [name: 'Покормить', 'msgTag': 'character_manager:feed'])
sendMessage('DeskChan:register-simple-action', [name: 'Пошалить', 'msgTag': 'character_manager:naughty'])
sendMessage('DeskChan:register-simple-action', [name: 'Страница проекта', 'msgTag': 'character_manager:about'])