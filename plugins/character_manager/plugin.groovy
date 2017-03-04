import classes.BrowserAdapter
import classes.CharacterManager
import classes.Character
import classes.Localization
import classes.Settings

import javax.swing.Timer
import java.awt.event.ActionEvent
import java.nio.file.Paths

// Тэг для пункта меню и сохранения настроек.
final String TAG_DELAY_MESSAGES = 'delay-between-messages'
// Задержка между случайными сообщениями по умолчанию.
final int DEFAULT_DELAY = 10

// Используется для получения заголовков пунктов меню на языке системы.
// Сейчас поддерживаются русский, а для всех остальных отображается английский.
Localization localization = Localization.getInstance()

// Получаем путь к директории, выделенной для хранения информации, сохраняющейся между обновлениями билдов приложения.
sendMessage('core:get-plugin-data-dir', null, { sender, data ->
    CharacterManager.setDataDir(Paths.get(((Map) data).get('path').toString()))
})

// Получаем случайного персонажа из папки resources/characters.
// Под персонажем подразумевается папка, внутри которой располагаются папки sprites и phrases.
Character character = CharacterManager.getRandomCharacter()

// Получаем параметры и запускаем таймер случайных сообщений.
String storedDelay = Settings.getInstance().get(TAG_DELAY_MESSAGES)
int delayBetweenMessages = (storedDelay != null) ? Integer.parseInt(storedDelay) : DEFAULT_DELAY
Timer messageShowTimer = initMessageTimer(character, delayBetweenMessages)

// Поскольку у персонажа есть до 4 спрайтов и наборов фраз, которые устанавливаются в зависимости от времени суток
// (normal, night, morning и evening), так что каждый час плагин проверяет, не пришло ли время обновить эти данные.
Timer skinUpdateTimer = new Timer(3600000, { ActionEvent actionEvent ->
    if (character.reloadRequired())
        refreshCharacter(character)
})
refreshCharacter(character)
skinUpdateTimer.start()


// При выгрузке плагина останавливаем таймеры и сохраняем состояние персонажа.
addCleanupHandler({
    skinUpdateTimer.stop()
    messageShowTimer.stop()
    character.saveState()
})


// Добавляем обработчики пунктов меню.
addMessageListener('character_manager:feed', { sender, tag, data ->
    showMessage(character.feed())
})

addMessageListener('character_manager:naughty', { sender, tag, data ->
    showMessage(character.doNaughtyThings())
})

addMessageListener('character_manager:about', {sender, tag, data ->
    BrowserAdapter.openWebpage("http://eternal-search.com/deskchan/")
})


// Обработчик кликов по персонажу.
addMessageListener('gui-events:character-left-click', { sender, tag, data ->
    showMessage(character.getClickPhrase())
})

// Обработчик изменения настроек.
addMessageListener('character_manager:save-settings', { sender, tag, data ->
    if (data.containsKey(TAG_DELAY_MESSAGES)) {
        Settings.getInstance().put((String) TAG_DELAY_MESSAGES, (String) data[TAG_DELAY_MESSAGES])
        messageShowTimer = initMessageTimer(character, (int) data[TAG_DELAY_MESSAGES])
    }
})


// Добавляем пункты в меню.
sendMessage('DeskChan:register-simple-action', [name: localization.get('feed'), 'msgTag': 'character_manager:feed'])
sendMessage('DeskChan:register-simple-action', [name: localization.get('naughty'), 'msgTag': 'character_manager:naughty'])
sendMessage('DeskChan:register-simple-action', [name: localization.get('about'), 'msgTag': 'character_manager:about'])

// Добавляем вкладку с настройками.
sendMessage('gui:add-options-tab', [name: 'character_manager', msgTag: 'character_manager:save-settings', controls: [
    [
        type: 'Spinner', id: TAG_DELAY_MESSAGES, label: localization.get('settings-delay'),
        value: delayBetweenMessages, min: 5, max: 180, step: 5
    ]
]])


// Вспомогательная функция, отображающая текст только при условии, что это не пустая строка или null.
def showMessage(String message) {
    if (message != null && message.trim() != "")
        sendMessage('DeskChan:say', [text: message])
}


// Функция для обновления списка фраз и спрайта персонажа в соответствии с текущим временем суток.
def refreshCharacter(Character character) {
    sendMessage('gui:change-skin', character.getSkin())
    character.reloadPhrases()
    showMessage(character.getWelcomePhrase())
}

// Функция (пере-)инициализации таймера.
// Используется при загрузке плагина и в случае изменения настроек.
Timer initMessageTimer(Character character, int minutes) {
    Timer timer = new Timer(minutes * 60000, { ActionEvent actionEvent ->
        showMessage(character.getRandomPhrase())
    })
    timer.start()
    return timer
}