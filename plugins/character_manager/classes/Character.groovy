package classes

import enums.PhraseAction
import enums.TimeOfDay
import exceptions.WrongCharacterException

import java.nio.file.Path

class Character {
    final private static int MAX_SATIETY = 100
    final private static int MAX_PLEASURE = 100
    final private static int SATIETY_ACCRETION = 10
    final private static int PLEASURE_ACCRETION = 10

    private String name
    private SkinInfo defaultSkin, nightSkin, morningSkin, eveningSkin
    private TimeOfDay currentTimeOfDay

    private PhrasesSet phrases
    private String lastRandomPhrase = ""

    private int satiety = Math.ceil(MAX_SATIETY / 2)
    private int pleasure = Math.ceil(MAX_PLEASURE / 2)

    Character(String name) throws WrongCharacterException
    {
        this.name = name

        SkinInfo[] skins = ResourcesLoader.readSkins(name)
        for (SkinInfo info : skins) {
            switch (info.name.substring(0, info.name.length() - 4)) {
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
    }

    String getName() { return name }

    Path getSkin() {
        TimeOfDay timeOfDay = Clock.getTimeOfDay()

        switch (timeOfDay) {
            case TimeOfDay.MORNING:
                currentTimeOfDay = TimeOfDay.MORNING
                return (morningSkin != null) ? morningSkin.path : defaultSkin.path
            case TimeOfDay.NIGHT:
                currentTimeOfDay = TimeOfDay.NIGHT
                return (nightSkin != null) ? nightSkin.path : defaultSkin.path
            case TimeOfDay.EVENING:
                currentTimeOfDay = TimeOfDay.EVENING
                return (eveningSkin != null) ? eveningSkin.path : defaultSkin.path
            default:
                currentTimeOfDay = TimeOfDay.DAY
                return defaultSkin.path
        }
    }

    boolean reloadRequired() {
        return currentTimeOfDay != Clock.getTimeOfDay()
    }

    String getWelcomePhrase() {
        return getRandomPhrase(getPhrases(PhraseAction.WELCOME))
    }

    String getClickPhrase() {
        increasePleasure()
        return getRandomPhrase(getPhrases(PhraseAction.CLICK))
    }

    String feed() {
        increaseSatiety()
        return getRandomPhrase(getPhrases(PhraseAction.FEED))
    }

    String doNaughtyThings() {
        increasePleasure()
        decreaseSatiety()
        return getRandomPhrase(getPhrases(PhraseAction.NAUGHTY))
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
            while (currentPhrase == lastRandomPhrase) {
                int i = random.nextInt(sourceList.size() - 1)
                currentPhrase = sourceList[i]
            }
            lastRandomPhrase = currentPhrase

            return currentPhrase
        }
        else if (count > 0)
            return sourceList[0]
        else
            return null
    }

    void reloadPhrases() {
        phrases = ResourcesLoader.readPhrases(name)
    }

    private Set<String> getPhrases(PhraseAction action) {
        int satietyMeasure = Math.ceil(MAX_SATIETY / 3)
        int pleasureMeasure = Math.ceil(MAX_PLEASURE / 3)

        if (satiety < satietyMeasure)
            return phrases.getHungryPhrases(action)
        else if (satiety > satietyMeasure * 2)
            return phrases.getFullPhrases(action)
        else if (pleasure < pleasureMeasure)
            return phrases.getSexuallyHungryPhrases(action)
        else if (pleasure > pleasureMeasure * 2)
            return phrases.getSexuallySatisfiedPhrases(action)
        else
            return phrases.getDefaultPhrases(action)
    }

    private decreaseSatiety() {
        if (satiety > 0)
            satiety--
    }

    private decreasePleasure() {
        if (pleasure > 0)
            pleasure--
    }

    private increaseSatiety() {
        if (satiety + SATIETY_ACCRETION <= MAX_SATIETY)
            satiety += SATIETY_ACCRETION
    }

    private increasePleasure() {
        if (pleasure + PLEASURE_ACCRETION <= MAX_PLEASURE)
            pleasure += PLEASURE_ACCRETION
    }
}