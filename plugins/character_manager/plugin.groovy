import classes.BrowserAdapter
import classes.CharacterManager
import classes.Character
import classes.Localization
import classes.Settings


// Тэги для пунктов меню и сохранения настроек.
final String TAG_DELAY_MESSAGES = 'delay-between-messages'
final String TAG_CHOSEN_CHARACTER = 'chosen-character'
// Задержка между случайными сообщениями по умолчанию.
final int DEFAULT_DELAY = 10

// Используется для получения заголовков пунктов меню на языке системы.
// Сейчас поддерживаются русский, а для всех остальных отображается английский.
Localization localization = Localization.getInstance()

// Получаем путь к директории, выделенной для хранения информации, сохраняющейся между обновлениями билдов приложения.
CharacterManager.setDataDir(getDataDirPath())

// Получаем ранее выбранного или случайного персонажа из папки resources/characters.
// Под персонажем подразумевается папка, внутри которой располагаются папки sprites и phrases.
String storedCharacterName = Settings.getInstance().get('chosen-character')
Character character = (storedCharacterName != null) ? new Character(storedCharacterName) : CharacterManager.getRandomCharacter()

// Получаем параметры и запускаем таймер случайных сообщений.
String storedDelay = Settings.getInstance().get(TAG_DELAY_MESSAGES)
int delayBetweenMessages = (storedDelay != null) ? Integer.parseInt(storedDelay) : DEFAULT_DELAY
Timer messageShowTimer = initMessageTimer(character, delayBetweenMessages)

// Поскольку у персонажа есть до 4 спрайтов и наборов фраз, которые устанавливаются в зависимости от времени суток
// (normal, night, morning и evening), так что каждый час плагин проверяет, не пришло ли время обновить эти данные.
Timer skinUpdateTimer = new Timer()
skinUpdateTimer.schedule({ ->
    if (character.reloadRequired())
        refreshCharacter(character)
}, 3600000, 3600000)
refreshCharacter(character)


// При выгрузке плагина останавливаем таймеры и сохраняем состояние персонажа.
addCleanupHandler({
    skinUpdateTimer.cancel()
    skinUpdateTimer.purge()
    messageShowTimer.cancel()
    messageShowTimer.purge()
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
    BrowserAdapter.openWebpage("http://deskchan.info/")
})


// Обработчик кликов по персонажу.
addMessageListener('gui-events:character-left-click', { sender, tag, data ->
    showMessage(character.getClickPhrase())
})

// Обработчик сохранения настроек.
addMessageListener('character_manager:save-settings', { sender, tag, data ->
    Settings settings = Settings.getInstance()

    if (data.containsKey(TAG_DELAY_MESSAGES)) {
        settings.put(TAG_DELAY_MESSAGES, (String) data[TAG_DELAY_MESSAGES], false)
        messageShowTimer = initMessageTimer(character, (int) data[TAG_DELAY_MESSAGES])
    }

    if (data.containsKey(TAG_CHOSEN_CHARACTER) && data[TAG_CHOSEN_CHARACTER] != null) {
        character = CharacterManager.getCharacterById((int) data[TAG_CHOSEN_CHARACTER])
        settings.put(TAG_CHOSEN_CHARACTER, character.getName(), false)
        refreshCharacter(character)
    }

    settings.save()
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
    ],
    [
        type: 'ComboBox', id: TAG_CHOSEN_CHARACTER, label: localization.get('settings-character'),
        values: Arrays.asList(CharacterManager.getCharacterList()), value: CharacterManager.getIdOfCharacter(character)
    ]
]])


// ФУНКЦИИ

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
    int delayTime = minutes * 60000
    Timer timer = new Timer()
    timer.schedule({ ->
        showMessage(character.getRandomPhrase())
    }, delayTime, delayTime)
    return timer
}