package classes

import enums.PhraseAction
import enums.TimeOfDay
import exceptions.WrongCharacterException
import javafx.scene.media.Media
import javafx.scene.media.MediaException
import javafx.scene.media.MediaPlayer

import java.nio.file.Path
import java.nio.file.Paths

// Класс персонажа.
class Character {
    // Некоторые параметры.
    final private static int MAX_SATIETY = 100
    final private static int MAX_PLEASURE = 100
    final private static int MAX_OXYGEN_SATURATION = 100
    final private static int SATIETY_ACCRETION = 10
    final private static int PLEASURE_ACCRETION = 10
    final private static int OXYGEN_SATURATION_ACCRETION = 10
    // Задежка между началом "глажения" персонажа и выдачей фразы.
    final private static int MAX_PET_COUNTER = 750

    private String name
    // Для каждого персонажа можно задать до 4 спрайтов, отображаемых в зависимости от времени суток.
    private SkinInfo defaultSkin, nightSkin, morningSkin, eveningSkin
    private TimeOfDay currentTimeOfDay
    // Для фраз также можно задавать различные наборы.
    private PhrasesSet phrases
    private String lastRandomPhrase = ""
    private CharacterInfo characterInfo = null
    // Поля, нужные для воспроизведения музыки...
    private List<Media> musicList = null
    private MediaPlayer mediaPlayer = null
    private int lastSongId = -1
    // ...и звуков.
    private SoundLoader soundLoader

    // Состояние персонажа при запуске считывается из файла настроек
    // или выставляется по умолчанию в половину максимального.
    private int satiety
    private int pleasure
    private int oxygenSaturation
    // Параметры, отвечающие за возможность погладить персонажа.
    private int petCounter = MAX_PET_COUNTER
    private int petMultiplier = 1

    // Конструктор проверяет наличие необходимых ресурсов в resources/characters/%name%.
    // Если там нет sprites/normal.png или phrases/default.txt, то выбрасывается WrongCharacterException.
    // Также он читает спрайты и фразы для текущего времени суток.
    Character(String name) throws WrongCharacterException
    {
        this.name = name

        SkinInfo[] skins = ResourcesLoader.readSkins(name)
        for (SkinInfo info : skins) {
            String skinName = (info.isSet) ? info.name : info.name.substring(0, info.name.length() - 4)
            switch (skinName) {
                case "normal":
                    defaultSkin = info
                    break
                case "night":
                    nightSkin = info
                    break
                case "morning":
                    morningSkin = info
                    break
                case "evening":
                    eveningSkin = info
                    break
            }
        }

        if (defaultSkin == null)
            throw new WrongCharacterException("No default skin!")

        reloadPhrases()

        Settings settings = Settings.getInstance()
        String storedSatiety = settings.get("satiety")
        String storedPleasure = settings.get("pleasure")
        String storedOxygenSaturation = settings.get("oxygen-saturation")

        satiety = (storedSatiety != null) ? Integer.parseInt(storedSatiety) : Math.ceil(MAX_SATIETY / 2)
        pleasure = (storedPleasure != null) ? Integer.parseInt(storedPleasure) : Math.ceil(MAX_PLEASURE / 2)
        oxygenSaturation = (storedOxygenSaturation != null) ? Integer.parseInt(storedOxygenSaturation) : Math.ceil(MAX_OXYGEN_SATURATION / 2)

        characterInfo = ResourcesLoader.readCharacterInfo(name)
        musicList = ResourcesLoader.readCharacterMusic(name)
        soundLoader = new SoundLoader(name)
    }

    String getName() { return name }

    // Возвращает спрайт для текущего времени суток.
    Path getSkin() {
        TimeOfDay timeOfDay = Clock.getTimeOfDay()
        currentTimeOfDay = timeOfDay

        switch (timeOfDay) {
            case TimeOfDay.MORNING:
                return morningSkin?.path ?: defaultSkin.path
            case TimeOfDay.NIGHT:
                return nightSkin?.path ?: defaultSkin.path
            case TimeOfDay.EVENING:
                return eveningSkin?.path ?: defaultSkin.path
            default:
                return defaultSkin.path
        }
    }

    // Возвращает true, если время суток изменилось и требуется перезагрузка фраз и устан.
    boolean reloadRequired() {
        return currentTimeOfDay != Clock.getTimeOfDay()
    }

    // Следующие методы возвращают нужные фразы...
    String getWelcomePhrase() {
        return getRandomPhrase(getPhrases(PhraseAction.WELCOME))
    }

    String getClickPhrase() {
        increasePleasure()
        decreaseOxygenSaturation()
        tryPlayback(soundLoader.tryGetSound('click-reaction'))
        return getRandomPhrase(getPhrases(PhraseAction.CLICK))
    }

    // ...а некоторые ещё изменяют параметры персонажа.
    String feed() {
        increaseSatiety()
        decreaseOxygenSaturation()
        return getRandomPhrase(getPhrases(PhraseAction.FEED))
    }

    String doNaughtyThings() {
        if (getValueCharacteristic(pleasure, MAX_PLEASURE) <= 0) {
            increasePleasure()
            increasePleasure()
        }
        decreaseSatiety()
        decreaseOxygenSaturation()
        return getRandomPhrase(getPhrases(PhraseAction.NAUGHTY))
    }

    String walk() {
        if (getValueCharacteristic(oxygenSaturation, MAX_OXYGEN_SATURATION) <= 0) {
            increasePleasure()
        }
        decreaseSatiety()
        decreaseSatiety()
        increaseOxygenSaturation()
        return getRandomPhrase(getPhrases(PhraseAction.WALK))
    }

    String play() {
        increasePleasure()
        decreaseSatiety()
        decreaseOxygenSaturation()
        decreaseOxygenSaturation()

        URI gameURI = characterInfo.getRandomSteamId()
        if (gameURI != null) {
            BrowserAdapter.openWebpage(gameURI)
            return null
        } else {
            return getRandomPhrase(getPhrases(PhraseAction.PLAY))
        }
    }

    String watch() {
        increasePleasure()
        decreaseSatiety()
        decreaseOxygenSaturation()

        URL websiteURL = characterInfo.getRandomAnimeWebsite()
        if (websiteURL != null) {
            BrowserAdapter.openWebpage(websiteURL)
            return null
        } else {
            return getRandomPhrase(getPhrases(PhraseAction.WATCH))
        }
    }

    // Следующие 3 метода позволяют гладить персонажа курсором ^_^
    String pet() {
        if (petCounter > 0) {
            petCounter--
            return null
        } else {
            petMultiplier++
            resetPetCounter()
            tryPlayback(soundLoader.tryGetSound('petting-reaction'))
            return getRandomPhrase(getPhrases(PhraseAction.PET))
        }
    }

    void resetPetCounter() {
        petCounter = MAX_PET_COUNTER * petMultiplier
    }

    void resetPetMultiplier() {
        petMultiplier = 1
        resetPetCounter()
    }

    // Следующие 4 метода обеспечивают воспроизведение музыки и звуков.
    boolean listenToMusic() {
        increasePleasure()
        increasePleasure()

        if (musicList.size() == 0)
            return false

        int i = 0
        if (musicList.size() > 1) {
            i = lastSongId
            int endlessLoopFuse = 100
            Random rand = new Random()
            while (i == lastSongId && endlessLoopFuse > 0) {
                i = rand.nextInt(musicList.size())
                endlessLoopFuse--
            }
        }

        listenTo(musicList[i])
        return true
    }

    void listenTo(Media media) {
        mediaPlayer?.stop()
        mediaPlayer = new MediaPlayer(media)
        mediaPlayer.play()
    }

    void listenTo(String mediaPath) {
        Path path = Paths.get(mediaPath)
        Media media
        try {
            media = new Media(path.toUri().toString())
        } catch (MediaException e) {
            e.printStackTrace()
            return
        }
        listenTo(media)
    }

    void tryPlayback(Media media) {
        if (media != null)
            listenTo(media)
    }

    String getRandomPhrase() {
        decreaseSatiety()
        decreasePleasure()
        return getRandomPhrase(getPhrases(PhraseAction.MESSAGE))
    }

    String getRandomPhrase(Set sourceList) {
        Random random = new Random()
        int count = sourceList.size()

        if (count > 1) {
            String currentPhrase = lastRandomPhrase
            int endlessLoopDetector = 100
            while (currentPhrase == lastRandomPhrase) {
                int i = random.nextInt(sourceList.size())
                currentPhrase = sourceList[i]

                if (--endlessLoopDetector <= 0)
                    break
            }
            lastRandomPhrase = currentPhrase

            return currentPhrase
        }
        else if (count > 0)
            return sourceList[0]
        else
            return null
    }

    // Перезагружает фразы, чтоб они соответствовали текущему времени суток.
    void reloadPhrases() {
        if (reloadRequired()) {
            phrases = ResourcesLoader.readPhrases(name)
            currentTimeOfDay = Clock.getTimeOfDay()
        }
    }

    // Сохраняет состояние персонажа в файл настроек.
    void saveState() {
        Settings settings = Settings.getInstance()
        settings.put('satiety', Integer.toString(satiety), false)
        settings.put('pleasure', Integer.toString(pleasure), false)
        settings.put('oxygen-saturation', Integer.toString(oxygenSaturation), false)
        settings.save()
    }

    // Используется при выгрузке плагина.
    // Сохраняет состояние персонажа и останавливает воспроизведение музыки.
    void unload() {
        saveState()
        mediaPlayer?.stop()
    }

    // Возвращает фразы для указанного действия.
    private Set<String> getPhrases(PhraseAction action) {
        int satietyCharacteristic = getValueCharacteristic(satiety, MAX_SATIETY)
        int pleasureCharacteristic = getValueCharacteristic(pleasure, MAX_PLEASURE)
        int oxygenSaturationCharacteristic = getValueCharacteristic(oxygenSaturation, MAX_OXYGEN_SATURATION)

        Set<String> gotPhrases
        if (satietyCharacteristic < 0) {
            gotPhrases = phrases.getHungryPhrases(action)
            if (gotPhrases != null)
                return gotPhrases
        }
        if (satietyCharacteristic > 0) {
            gotPhrases = phrases.getFullPhrases(action)
            if (gotPhrases != null)
                return gotPhrases
        }
        if (pleasureCharacteristic < 0) {
            gotPhrases = phrases.getSexuallyHungryPhrases(action)
            if (gotPhrases != null)
                return gotPhrases
        }
        if (pleasureCharacteristic > 0) {
            gotPhrases = phrases.getSexuallySatisfiedPhrases(action)
            if (gotPhrases != null)
                return gotPhrases
        }
        if (oxygenSaturationCharacteristic < 0) {
            gotPhrases = phrases.getWannaGoOutsidePhrases(action)
            if (gotPhrases != null)
                return gotPhrases
        }
        if (oxygenSaturationCharacteristic > 0) {
            gotPhrases = phrases.getWannaSitHomePhrases(action)
            if (gotPhrases != null)
                return gotPhrases
        }
        return phrases.getDefaultPhrases(action)
    }

    // Возвращает -1, если параметр в зоне недостатка; 1, если пресыщение; 0, если он в норме.
    private int getValueCharacteristic(int parameter, final int maxValue) {
        int parameterMeasure = Math.ceil(maxValue / 3)
        if (parameter < parameterMeasure)
            return -1
        if (parameter > parameterMeasure * 2)
            return 1
        return 0
    }

    // Для изменения параметров лучше использовать специальные методы, которые проверяют границы.
    private decreaseSatiety() {
        if (satiety > 0)
            satiety--
    }

    private decreasePleasure() {
        if (pleasure > 0)
            pleasure--
    }

    private decreaseOxygenSaturation() {
        if (oxygenSaturation > 0)
            oxygenSaturation--
    }

    private increaseSatiety() {
        if (satiety + SATIETY_ACCRETION <= MAX_SATIETY)
            satiety += SATIETY_ACCRETION
    }

    private increasePleasure() {
        if (pleasure + PLEASURE_ACCRETION <= MAX_PLEASURE)
            pleasure += PLEASURE_ACCRETION
    }

    private increaseOxygenSaturation() {
        if (oxygenSaturation + OXYGEN_SATURATION_ACCRETION <= MAX_OXYGEN_SATURATION)
            oxygenSaturation += OXYGEN_SATURATION_ACCRETION
    }
}