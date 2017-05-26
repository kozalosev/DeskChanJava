import classes.CharacterManager
import classes.Character
import classes.Localization
import classes.Settings
import exceptions.WrongCharacterException
import exceptions.WrongLocalizationException


// Тэги для пунктов меню и сохранения настроек.
final String TAG_PLUGIN = 'character_manager'
final String TAG_DELAY_MESSAGES = 'delay-between-messages'
final String TAG_CHOSEN_CHARACTER = 'chosen-character'
final String TAG_MUTE = 'mute'
// Задержка между случайными сообщениями по умолчанию.
final int DEFAULT_DELAY = 10

// Используется для получения заголовков пунктов меню на языке системы.
// Сейчас поддерживаются русский, а для всех остальных отображается английский.
Localization localization
try {
    localization = Localization.getInstance()
} catch (WrongLocalizationException e) {
    log(e)
    sendMessage('gui:show-notification', [name: 'Localization error', text: e.getLocalizedMessage()])
    return
}

// Получаем путь к директории, выделенной для хранения информации, сохраняющейся между обновлениями билдов приложения.
CharacterManager.setDataDir(getDataDirPath())

// Получаем ранее выбранного или случайного персонажа из папки resources/characters.
// Под персонажем подразумевается папка, внутри которой располагаются папки sprites и phrases.
String storedCharacterName = Settings.getInstance().get(TAG_CHOSEN_CHARACTER)
Character character
try {
    character = (storedCharacterName != null) ? new Character(storedCharacterName) : CharacterManager.getRandomCharacter()
} catch (WrongCharacterException e) {
    log(e)
    sendMessage('gui:show-notification', [name: localization.get('no-character-title'), text: e.getLocalizedMessage()])
    return
}

// Получаем параметры и запускаем таймер случайных сообщений.
String storedDelay = Settings.getInstance().get(TAG_DELAY_MESSAGES)
int delayBetweenMessages = (storedDelay != null) ? Integer.parseInt(storedDelay) : DEFAULT_DELAY
initMessageTimer(character, delayBetweenMessages)

// Переключатель отключения звуков и музыки.
String storedMuteStatus = Settings.getInstance().get(TAG_MUTE)
if (storedMuteStatus != null)
    character.mute = Boolean.parseBoolean(storedMuteStatus)

// Поскольку у персонажа есть до 4 спрайтов и наборов фраз, которые устанавливаются в зависимости от времени суток
// (normal, night, morning и evening), так что каждый час плагин проверяет, не пришло ли время обновить эти данные.
initSkinUpdateTimer(character)
refreshCharacter(character)


// При выгрузке плагина останавливаем таймеры и сохраняем состояние персонажа.
addCleanupHandler({
    character.unload()
})


// Добавляем обработчики пунктов меню.
addMessageListener("$TAG_PLUGIN:feed", { sender, tag, data ->
    showMessage(character.feed())
})

addMessageListener("$TAG_PLUGIN:naughty", { sender, tag, data ->
    showMessage(character.doNaughtyThings())
})

addMessageListener("$TAG_PLUGIN:walk", { sender, tag, data ->
    showMessage(character.walk())
})

addMessageListener("$TAG_PLUGIN:play", { sender, tag, data ->
    showMessage(character.play())
})

addMessageListener("$TAG_PLUGIN:watch", { sender, tag, data ->
    showMessage(character.watch())
})

addMessageListener("$TAG_PLUGIN:listen-to", { sender, tag, data ->
    if (character.mute) {
        sendMessage('gui:show-notification', [
                name: localization.get('sound-disabled-title'),
                text: localization.get('sound-disabled-text')]
        )
        return
    }

    if (!character.listenToMusic()) {
        sendMessage('gui:choose-files', [
                title: "${localization.get('chooser-title')}:".toString(),
                filters: [[description: localization.get('music-files'), extensions: ['*.mp3']]]
        ], { s, d ->
            if (d['path'] != null)
                character.listenTo(d['path'].toString())
        })
    }
})


// Обработчик кликов по персонажу.
addMessageListener('gui-events:character-left-click', { sender, tag, data ->
    showMessage(character.getClickPhrase())
})

// Обработчик сохранения настроек.
addMessageListener("$TAG_PLUGIN:save-settings", { sender, tag, data ->
    Settings settings = Settings.getInstance()

    settings.put(TAG_DELAY_MESSAGES, (String) data[TAG_DELAY_MESSAGES], false)
    initMessageTimer(character, (int) data[TAG_DELAY_MESSAGES])

    if (data[TAG_CHOSEN_CHARACTER] != null && data[TAG_CHOSEN_CHARACTER] != CharacterManager.getIdOfCharacter(character)) {
        character.unload()
        character = CharacterManager.getCharacterById((int) data[TAG_CHOSEN_CHARACTER])
        settings.put(TAG_CHOSEN_CHARACTER, character.getName(), false)
        refreshCharacter(character)
    }

    settings.put(TAG_MUTE, (String) data[TAG_MUTE], false)
    character.mute = (boolean) data[TAG_MUTE]

    settings.save()
})

// Запускает обработку поглаживаний персонажа курсором.
addMessageListener('gui-events:character-mouse-moved', { sender, tag, data ->
    showMessage(character.pet())
})
initResetPetTimer(character)
initResetPetMultiplierTimer(character)


// Добавляем пункты в меню.
sendMessage('DeskChan:register-simple-actions', [
    [name: localization.get('feed'), msgTag: "$TAG_PLUGIN:feed".toString()],
    [name: localization.get('naughty'), msgTag: "$TAG_PLUGIN:naughty".toString()],
    [name: localization.get('walk'), msgTag: "$TAG_PLUGIN:walk".toString()],
    [name: localization.get('play'), msgTag: "$TAG_PLUGIN:play".toString()],
    [name: localization.get('watch'), msgTag: "$TAG_PLUGIN:watch".toString()],
    [name: localization.get('listen-to'), msgTag: "$TAG_PLUGIN:listen-to".toString()]
])

// Добавляем вкладку с настройками.
sendMessage('gui:setup-options-tab', [name: localization.get('plugin-name'), msgTag: "$TAG_PLUGIN:save-settings".toString(), controls: [
    [
        type: 'Spinner', id: TAG_DELAY_MESSAGES, label: localization.get('settings-delay'),
        value: delayBetweenMessages, min: 5, max: 180, step: 5
    ],
    [
        type: 'ComboBox', id: TAG_CHOSEN_CHARACTER, label: localization.get('settings-character'),
        values: Arrays.asList(CharacterManager.getCharacterList()), value: CharacterManager.getIdOfCharacter(character)
    ],
    [
        type: 'CheckBox', id: TAG_MUTE, label: localization.get('settings-mute'),
        value: character.mute
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


// Функции (пере-)инициализации таймеров.
// Эта используется при загрузке плагина и в случае изменения настроек.
def initMessageTimer(Character character, int minutes) {
    int delayTime = minutes * 60000
    sendMessage('core-utils:notify-after-delay', [delay: delayTime, seq: 'message-timer'], {sender, data ->
        showMessage(character.getRandomPhrase())
        initMessageTimer(character, minutes)
    })
}
// А эта и последующие -- только при загрузке, но функции нужны для переинициализации таймеров после срабатывания.
def initSkinUpdateTimer(Character character) {
    sendMessage('core-utils:notify-after-delay', [delay: 3600000, seq: 'skin-update-timer'], {sender, data ->
        if (character.reloadRequired())
            refreshCharacter(character)
        initSkinUpdateTimer(character)
    })
}

// Следующие два таймера нужны для "системы поглаживания"
def initResetPetTimer(Character character) {
    sendMessage('core-utils:notify-after-delay', [delay: 30000, seq: 'pet-reset-timer'], {sender, data ->
        character.resetPetCounter()
        initResetPetTimer(character)
    })
}
def initResetPetMultiplierTimer(Character character) {
    sendMessage('core-utils:notify-after-delay', [delay: 600000, seq: 'pet-reset-multiplier-timer'], {sender, data ->
        character.resetPetMultiplier()
        initResetPetMultiplierTimer(character)
    })
}