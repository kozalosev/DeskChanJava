package classes

import enums.PhraseAction
import enums.TimeOfDay
import exceptions.WrongCharacterException

import java.nio.file.Path

// Класс персонажа.
class Character {
    // Некоторые параметры.
    final private static int MAX_SATIETY = 100
    final private static int MAX_PLEASURE = 100
    final private static int MAX_OXYGEN_SATURATION = 100
    final private static int SATIETY_ACCRETION = 10
    final private static int PLEASURE_ACCRETION = 10
    final private static int OXYGEN_SATURATION_ACCRETION = 10

    private String name
    // Для каждого персонажа можно задать до 4 спрайтов, отображаемых в зависимости от времени суток.
    private SkinInfo defaultSkin, nightSkin, morningSkin, eveningSkin
    private TimeOfDay currentTimeOfDay
    // Для фраз также можно задавать различные наборы.
    private PhrasesSet phrases
    private String lastRandomPhrase = ""
    private CharacterInfo characterInfo = null

    // Состояние персонажа при запуске считывается из файла настроек
    // или выставляется по умолчанию в половину максимального.
    private int satiety
    private int pleasure
    private int oxygenSaturation

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
        return getRandomPhrase(getPhrases(PhraseAction.CLICK))
    }

    // ...а некоторые ещё изменяют параметры персонажа.
    String feed() {
        increaseSatiety()
        decreaseOxygenSaturation()
        return getRandomPhrase(getPhrases(PhraseAction.FEED))
    }

    String doNaughtyThings() {
        increasePleasure()
        increasePleasure()
        decreaseSatiety()
        decreaseOxygenSaturation()
        return getRandomPhrase(getPhrases(PhraseAction.NAUGHTY))
    }

    String walk() {
        increasePleasure()
        decreaseSatiety()
        decreaseSatiety()
        increaseOxygenSaturation()
        return getRandomPhrase(getPhrases(PhraseAction.WALK))
    }

    void play() {
        increasePleasure()
        decreaseSatiety()
        decreaseOxygenSaturation()
        decreaseOxygenSaturation()

        URI gameURI = characterInfo.getRandomSteamId()
        if (gameURI != null)
            BrowserAdapter.openWebpage(gameURI)
    }

    void watch() {
        increasePleasure()
        decreaseSatiety()
        decreaseOxygenSaturation()

        URL websiteURL = characterInfo.getRandomAnimeWebsite()
        if (websiteURL != null)
            BrowserAdapter.openWebpage(websiteURL)
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
        phrases = ResourcesLoader.readPhrases(name)
        currentTimeOfDay = Clock.getTimeOfDay()
    }

    // Сохраняет состояние персонажа в файл настроек.
    void saveState() {
        Settings settings = Settings.getInstance()
        settings.put('satiety', Integer.toString(satiety), false)
        settings.put('pleasure', Integer.toString(pleasure), false)
        settings.save()
    }

    // Возвращает фразы для указанного действия.
    private Set<String> getPhrases(PhraseAction action) {
        int satietyMeasure = Math.ceil(MAX_SATIETY / 3)
        int pleasureMeasure = Math.ceil(MAX_PLEASURE / 3)
        int oxygenSaturationMeasure = Math.ceil(MAX_OXYGEN_SATURATION / 3)

        if (satiety < satietyMeasure)
            return phrases.getHungryPhrases(action)
        else if (satiety > satietyMeasure * 2)
            return phrases.getFullPhrases(action)
        else if (pleasure < pleasureMeasure)
            return phrases.getSexuallyHungryPhrases(action)
        else if (pleasure > pleasureMeasure * 2)
            return phrases.getSexuallySatisfiedPhrases(action)
        else if (oxygenSaturation < oxygenSaturationMeasure)
            return phrases.getWannaGoOutsidePhrases(action)
        else if (oxygenSaturation > oxygenSaturationMeasure * 2)
            return phrases.getWannaSitHomePhrases(action)
        else
            return phrases.getDefaultPhrases(action)
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