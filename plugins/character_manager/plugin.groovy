package character_manager

import character_manager.logic.BrowserAdapter
import character_manager.logic.CharacterManager
import character_manager.logic.Character

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


addMessageListener('tamagochi_chan:feed', { sender, tag, data ->
    sendMessage('DeskChan:say', [text: character.feed()])
})

addMessageListener('tamagochi_chan:naughty', { sender, tag, data ->
    sendMessage('DeskChan:say', [text: character.doNaughtyThings()])
})

addMessageListener('gui:left-click', { sender, tag, data ->
    sendMessage('DeskChan:say', [text: character.getClickPhrase()])
})

addMessageListener('character_manager:about', {sender, tag, data ->
    BrowserAdapter.openWebpage("https://2ch.hk/s/res/1936557.html")
})

sendMessage('DeskChan:register-simple-action', [name: 'Покормить', 'msgTag': 'tamagochi_chan:feed'])
sendMessage('DeskChan:register-simple-action', [name: 'Пошалить', 'msgTag': 'tamagochi_chan:naughty'])
sendMessage('DeskChan:register-simple-action', [name: 'Страница проекта', 'msgTag': 'character_manager:about'])