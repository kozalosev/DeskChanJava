package classes

import enums.PhraseAction
import enums.PhraseCondition
import enums.TimeOfDay
import exceptions.WrongCharacterException

import java.nio.file.DirectoryStream
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.regex.Matcher
import java.util.regex.Pattern

// Класс, использующийся для загрузки спрайтов и наборов фраз.
abstract class ResourcesLoader {
    final private static Path CHARACTERS_PATH = CharacterManager.getDataDir().resolve("characters")


    // Функция для получения информации обо всех спрайтах.
    static SkinInfo[] readSkins(String characterName) throws WrongCharacterException
    {
        Path directoryPath = CHARACTERS_PATH.resolve(characterName).resolve("sprites")
        if (!Files.isDirectory(directoryPath))
            throw new WrongCharacterException("Character not found!")

        ArrayList<SkinInfo> list = new ArrayList<>()
        try {
            DirectoryStream<Path> directoryStream = Files.newDirectoryStream(directoryPath)
            for (Path skinPath : directoryStream) {
                if (!Files.isDirectory(skinPath))
                    list.add(new SkinInfo(skinPath))
            }
        } catch (IOException e) {
            e.printStackTrace()
        }

        SkinInfo[] resultArray = new SkinInfo[list.size()]
        resultArray = list.toArray(resultArray)
        return resultArray
    }


    // Эта функция считывает и возвращает только набор фраз для текущего времени суток.
    static PhrasesSet readPhrases(String characterName) throws WrongCharacterException {
        Path directoryPath = CHARACTERS_PATH.resolve(characterName).resolve("phrases")
        if (!Files.isDirectory(directoryPath))
            throw new WrongCharacterException("Character not found!")

        PhrasesSet set = new PhrasesSet()
        set.concat(readPhrasesFile(directoryPath.resolve("default.txt")))

        switch (Clock.getTimeOfDay()) {
            case TimeOfDay.DAY:
                set.concat(readPhrasesFile(directoryPath.resolve("day.txt")))
                break
            case TimeOfDay.NIGHT:
                set.concat(readPhrasesFile(directoryPath.resolve("night.txt")))
                break
            case TimeOfDay.MORNING:
                set.concat(readPhrasesFile(directoryPath.resolve("morning.txt")))
                break
            case TimeOfDay.EVENING:
                set.concat(readPhrasesFile(directoryPath.resolve("evening.txt")))
                break
        }

        return set
    }


    // Вспомогательная функция для чтения файлов с фразами
    private static PhrasesSet readPhrasesFile(Path filePath) {
        PhrasesSet set = new PhrasesSet()
        PhraseAction phraseAction = PhraseAction.MESSAGE
        PhraseCondition phraseCondition = PhraseCondition.DEFAULT

        try {
            if (!Files.isReadable(filePath))
                return set

            Files.lines(filePath).forEach({ line ->
                if (line.trim() == "" || line[0] == '#')
                    return

                Pattern p = Pattern.compile("\\[([A-Za-z_]*):?([A-Za-z_]*)]")
                Matcher m = p.matcher(line)
                while (m.find()) {
                    String action = m.group(1).toUpperCase()
                    String condition = m.group(2).toUpperCase()

                    if (action == "WELCOME")
                        phraseAction = PhraseAction.WELCOME
                    else if (action == "CLICK")
                        phraseAction = PhraseAction.CLICK
                    else if (action == "FEED")
                        phraseAction = PhraseAction.FEED
                    else if (action == "NAUGHTY")
                        phraseAction = PhraseAction.NAUGHTY

                    if (condition == "FED")
                        phraseCondition = PhraseCondition.FED
                    else if (condition == "HUNGRY")
                        phraseCondition = PhraseCondition.HUNGRY
                    else if (condition == "SEXUALLY_SATISFIED")
                        phraseCondition = PhraseCondition.SEXUALLY_SATISFIED
                    else if (condition == "SEXUALLY_HUNGRY")
                        phraseCondition = PhraseCondition.SEXUALLY_HUNGRY
                    else
                        phraseCondition = PhraseCondition.DEFAULT

                    return
                }

                set.add(phraseAction, phraseCondition, line)
            })
        } catch (IOException e) {
            e.printStackTrace()
        }

        return set
    }
}