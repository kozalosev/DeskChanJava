package character_manager.logic

import character_manager.exceptions.WrongCharacterException

import java.nio.file.DirectoryStream
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.regex.Matcher
import java.util.regex.Pattern

abstract class ResourcesLoader {
    final private static String CHARACTERS_PATH = "resources/characters/"

    static SkinInfo[] readSkins(String characterName) throws WrongCharacterException
    {
        Path directoryPath
        if (Files.isDirectory(Paths.get(CHARACTERS_PATH + characterName + "/sprites")))
            directoryPath = Paths.get(CHARACTERS_PATH + characterName + "/sprites")
        else if (Files.isDirectory(Paths.get("../" + CHARACTERS_PATH + characterName + "/sprites")))
            directoryPath = Paths.get("../" + CHARACTERS_PATH + characterName + "/sprites")

        ArrayList<SkinInfo> list = new ArrayList<>()
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

    static PhrasesSet readPhrases(String characterName) throws WrongCharacterException {
        Path directoryPath
        if (Files.isDirectory(Paths.get(CHARACTERS_PATH + characterName + "/phrases")))
            directoryPath = Paths.get(CHARACTERS_PATH + characterName + "/phrases")
        else if (Files.isDirectory(Paths.get("../" + CHARACTERS_PATH + characterName + "/phrases")))
            directoryPath = Paths.get("../" + CHARACTERS_PATH + characterName + "/phrases")

        PhrasesSet set = new PhrasesSet()
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
                            set.dayPhrases.concat(phrases)
                            set.nightPhrases.concat(phrases)
                            set.morningPhrases.concat(phrases)
                            set.eveningPhrases.concat(phrases)
                            break
                        case "day":
                            set.dayPhrases.concat(phrases)
                            break
                        case "night":
                            set.nightPhrases.concat(phrases)
                            break
                        case "morning":
                            set.morningPhrases.concat(phrases)
                            break
                        case "evening":
                            set.eveningPhrases.concat(phrases)
                            break
                        case "feed":
                            set.feedPhrases.concat(phrases)
                            break
                        case "naughty":
                            set.naughtyPhrases.concat(phrases)
                    }
                }
            } catch (IOException e) {
                e.printStackTrace()
            }
        }
        else
            throw new WrongCharacterException("Character not found!")

        return set
    }
}