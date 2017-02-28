import classes.BrowserAdapter
import classes.CharacterManager
import classes.Character
import classes.Localization
import classes.Settings

import javax.swing.Timer
import java.awt.event.ActionEvent

final String MENU_DELAY_MESSAGES = 'delay-between-messages'
final String PROPERTY_DELAY_MESSAGES = 'character_manager:delay-between-messages'
final int DEFAULT_DELAY = 10

Localization localization = Localization.getInstance()
Character character = CharacterManager.getRandomCharacter()

String storedDelay = Settings.getInstance().get(PROPERTY_DELAY_MESSAGES)
int delayBetweenMessages = (storedDelay != null) ? Integer.parseInt(storedDelay) : DEFAULT_DELAY
Timer messageShowTimer = initMessageTimer(character, delayBetweenMessages)

Timer skinUpdateTimer = new Timer(3600000, { ActionEvent actionEvent ->
    if (character.reloadRequired()) {
        sendMessage('gui:change-skin', character.getSkin())
        character.reloadPhrases()
        showMessage(character.getWelcomePhrase())
    }
})
skinUpdateTimer.initialDelay = 0
skinUpdateTimer.start()


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

sendMessage('DeskChan:register-simple-action', [name: localization.get('feed'), 'msgTag': 'character_manager:feed'])
sendMessage('DeskChan:register-simple-action', [name: localization.get('naughty'), 'msgTag': 'character_manager:naughty'])
sendMessage('DeskChan:register-simple-action', [name: localization.get('about'), 'msgTag': 'character_manager:about'])

addMessageListener('character_manager:save-settings', { sender, tag, data ->
    if (data.containsKey(MENU_DELAY_MESSAGES)) {
        Settings.getInstance().put((String) PROPERTY_DELAY_MESSAGES, (String) data[MENU_DELAY_MESSAGES])
        messageShowTimer = initMessageTimer(character, (int) data[MENU_DELAY_MESSAGES])
    }
})

sendMessage('gui:add-options-tab', [name: 'character_manager', msgTag: 'character_manager:save-settings', controls: [
    [
        type: 'Spinner', id: MENU_DELAY_MESSAGES, label: localization.get('settings-delay'),
        value: delayBetweenMessages, min: 5, max: 180, step: 5
    ]
]])


def showMessage(String message) {
    if (message != null && message.trim() != "")
        sendMessage('DeskChan:say', [text: message])
}

Timer initMessageTimer(Character character, int minutes) {
    Timer timer = new Timer(minutes * 60000, { ActionEvent actionEvent ->
        showMessage(character.getRandomPhrase())
    })
    timer.start()
    return timer
}