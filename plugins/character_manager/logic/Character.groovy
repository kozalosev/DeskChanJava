package character_manager.logic

import com.eternal_search.deskchan.core.Utils
import character_manager.exceptions.WrongCharacterException

import java.nio.file.DirectoryStream
import java.nio.file.Files
import java.nio.file.Path
import java.util.regex.Matcher
import java.util.regex.Pattern

class Character {
    private enum TimeOfDay {
        DAY, NIGHT, MORNING, EVENING
    }

    private String name
    private SkinInfo defaultSkin, nightSkin, morningSkin, eveningSkin
    private TimeOfDay currentTimeOfDay

    private Phrases dayPhrases = new Phrases()
    private Phrases nightPhrases = new Phrases()
    private Phrases morningPhrases = new Phrases()
    private Phrases eveningPhrases = new Phrases()
    private Phrases feedPhrases = new Phrases()
    private Phrases naughtyPhrases = new Phrases()
    private int lastRandomPhraseNumber = -1

    Character(String name) throws WrongCharacterException
    {
        this.name = name

        SkinInfo[] skins = readSkins(name)
        for (SkinInfo info : skins) {
            switch (info.name.substring(0, info.name.length() - 4)) {
                case "default":
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
        return getRandomPhrase(feedPhrases)
    }

    String doNaughtyThings() {
        return getRandomPhrase(naughtyPhrases)
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
        loadPhrases(name)
    }

    private static SkinInfo[] readSkins(String path) throws WrongCharacterException
    {
        ArrayList<SkinInfo> list = new ArrayList<>()
        Path directoryPath = Utils.getResourcePath("characters/" + path + "/sprites")
        if (directoryPath != null) {
            try {
                DirectoryStream<Path> directoryStream = Files.newDirectoryStream(directoryPath)
                for (Path skinPath : directoryStream) {
                    if (!Files.isDirectory(skinPath)) {
                        list.add(new SkinInfo(skinPath))
                    }
                }
            } catch (IOException e) {
                e.printStackTrace()
            }
        }
        else
            throw new WrongCharacterException("Character not found!")

        SkinInfo[] resultArray = new SkinInfo[list.size()]
        resultArray = list.toArray(resultArray)
        return resultArray
    }

    private void loadPhrases(String path) throws WrongCharacterException {
        Path directoryPath = Utils.getResourcePath("characters/" + path + "/phrases")
        if (directoryPath != null) {
            try {
                DirectoryStream<Path> directoryStream = Files.newDirectoryStream(directoryPath)
                for (Path filePath : directoryStream) {
                    if (Files.isDirectory(filePath))
                        continue

                    Phrases phrases = new Phrases()

                    Files.lines(filePath).forEach({ line ->
                        Pattern p = Pattern.compile("([A-Z]+)\\s*=\\s*(.*)")
                        Matcher m = p.matcher(line)
                        while (m.find()) {
                            String key = m.group(1)
                            String value = m.group(2)

                            if (key == "WELCOME")
                                phrases.welcomeMessage = value
                            else if (key == "CLICK")
                                phrases.clickMessage = value

                            return
                        }

                        phrases.phrases.add(line)
                    })

                    String filename = filePath.getFileName()
                    switch(filename.substring(0, filename.length() - 4)) {
                        case "default":
                            dayPhrases.concat(phrases)
                            nightPhrases.concat(phrases)
                            morningPhrases.concat(phrases)
                            eveningPhrases.concat(phrases)
                            break
                        case "day":
                            dayPhrases.concat(phrases)
                            break
                        case "night":
                            nightPhrases.concat(phrases)
                            break
                        case "morning":
                            morningPhrases.concat(phrases)
                            break
                        case "evening":
                            eveningPhrases.concat(phrases)
                            break
                        case "feed":
                            feedPhrases.concat(phrases)
                            break
                        case "naughty":
                            naughtyPhrases.concat(phrases)
                    }
                }
            } catch (IOException e) {
                e.printStackTrace()
            }
        }
        else
            throw new WrongCharacterException("Character not found!")
    }

    private Phrases getPhrases() {
        switch(getTimeOfDay()) {
            case TimeOfDay.MORNING:
                return morningPhrases
            case TimeOfDay.NIGHT:
                return nightPhrases
            case TimeOfDay.EVENING:
                return eveningPhrases
            default:
                return dayPhrases
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

class SkinInfo implements Comparable<SkinInfo> {

    String name
    Path path

    SkinInfo(String name, Path path) {
        this.name = name
        this.path = path
    }

    SkinInfo(Path path) {
        this(path.getFileName().toString(), path)
    }

    @Override
    int compareTo(SkinInfo o) {
        return name <=> o.name
    }

    @Override
    String toString() {
        return name
    }

}

class Phrases {
    String welcomeMessage = null
    String clickMessage = null
    List<String> phrases = new ArrayList<String>()

    void concat(Phrases another) {
        if (welcomeMessage == null)
            welcomeMessage = another.welcomeMessage
        if (clickMessage == null)
            clickMessage = another.clickMessage

        phrases.addAll(another.phrases)
    }
}