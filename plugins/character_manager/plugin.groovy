import classes.BrowserAdapter
import classes.CharacterManager
import classes.Character

import javax.swing.Timer
import java.awt.event.ActionEvent

Character character = CharacterManager.getRandomCharacter()

Timer skinUpdateTimer = new Timer(3600000, { ActionEvent actionEvent ->
    if (character.reloadRequired()) {
        sendMessage('gui:change-skin', character.getSkin())
        character.reloadPhrases()
        showMessage(character.getWelcomePhrase())
    }
})
skinUpdateTimer.initialDelay = 0

Timer messageShowTimer = new Timer(600000, { ActionEvent actionEvent ->
    showMessage(character.getRandomPhrase())
})

skinUpdateTimer.start()
messageShowTimer.start()

addCleanupHandler({
    skinUpdateTimer.stop()
    messageShowTimer.stop()
})


addMessageListener('character_manager:feed', { sender, tag, data ->
    showMessage(character.feed())
})

addMessageListener('character_manager:naughty', { sender, tag, data ->
    showMessage(character.doNaughtyThings())
})

addMessageListener('gui-events:character-left-click', { sender, tag, data ->
    showMessage(character.getClickPhrase())
})

addMessageListener('character_manager:about', {sender, tag, data ->
    BrowserAdapter.openWebpage("https://2ch.hk/s/res/1936557.html")
})

sendMessage('DeskChan:register-simple-action', [name: 'Покормить', 'msgTag': 'character_manager:feed'])
sendMessage('DeskChan:register-simple-action', [name: 'Пошалить', 'msgTag': 'character_manager:naughty'])
sendMessage('DeskChan:register-simple-action', [name: 'Страница проекта', 'msgTag': 'character_manager:about'])


def showMessage(String message) {
    if (message != null && message.trim() != "")
        sendMessage('DeskChan:say', [text: message])
}