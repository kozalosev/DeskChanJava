package classes

import exceptions.WrongCharacterException

import java.nio.file.Path

class Character {

    private enum TimeOfDay {
        DAY, NIGHT, MORNING, EVENING
    }

    private String name
    private SkinInfo defaultSkin, nightSkin, morningSkin, eveningSkin
    private TimeOfDay currentTimeOfDay

    private PhrasesSet phrases
    private int lastRandomPhraseNumber = -1

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
        TimeOfDay timeOfDay = getTimeOfDay()

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
        return currentTimeOfDay != getTimeOfDay()
    }

    String getWelcomePhrase() {
        return getPhrases().welcomeMessage
    }

    String getClickPhrase() {
        return getPhrases().clickMessage
    }

    String feed() {
        return getRandomPhrase(phrases.feedPhrases)
    }

    String doNaughtyThings() {
        return getRandomPhrase(phrases.naughtyPhrases)
    }

    String getRandomPhrase() {
        getRandomPhrase(getPhrases())
    }

    String getRandomPhrase(Phrases sourceList) {
        Random random = new Random()
        int count = sourceList.phrases.size()

        if (count > 1) {
            int i = lastRandomPhraseNumber
            while (i == lastRandomPhraseNumber)
                i = random.nextInt(sourceList.phrases.size() - 1)
            lastRandomPhraseNumber = i

            return sourceList.phrases[i]
        }
        else if (count > 0)
            return sourceList.phrases[0]
        else
            return null
    }

    void reloadPhrases() {
        phrases = ResourcesLoader.readPhrases(name)
    }

    private Phrases getPhrases() {
        switch(getTimeOfDay()) {
            case TimeOfDay.MORNING:
                return phrases.morningPhrases
            case TimeOfDay.NIGHT:
                return phrases.nightPhrases
            case TimeOfDay.EVENING:
                return phrases.eveningPhrases
            default:
                return phrases.dayPhrases
        }
    }

    private TimeOfDay getTimeOfDay() {
        Calendar currentTime = Calendar.getInstance()
        int currentHour = currentTime.get(Calendar.HOUR_OF_DAY)

        if (currentHour < 12 && currentHour > 6)
            return TimeOfDay.MORNING
        else if (currentHour < 17 && currentHour > 6)
            return TimeOfDay.DAY
        else if (currentHour < 23 && currentHour > 6)
            return TimeOfDay.EVENING
        else
            return TimeOfDay.NIGHT
    }
}