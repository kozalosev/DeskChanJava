package classes

import exceptions.WrongCharacterException

import java.nio.file.DirectoryStream
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths


// Класс для управления персонажами.
abstract class CharacterManager {
    // По умолчанию данные складывались бы в папку data внутри директории с плагином,
    // но вообще значение этой переменной стоит сменить перед созданием персонажей.
    static Path dataDir = Paths.get(new File(CharacterManager.class.protectionDomain.codeSource.location.path).getPath())
        .getParent().resolve("data")
    // Список персонажей заполняется один раз при первом обращении.
    private static Map<Integer, String> characters = null

    // Позволяет получить список доступных в папке персонажей...
    static String[] getCharacterList() {
        checkMapIntegrity()
        return characters.values()
    }

    // а также получить случайного персонажа из этого списка.
    static Character getRandomCharacter() {
        checkMapIntegrity()

        Random random = new Random()
        int id = random.nextInt(characters.size())
        return getCharacterById(id)
    }

    // Сопоставляет имена в списке и возвращает порядковый номер (от нуля) нужного персонажа.
    // Если персонаж не найден, то возвращается -1.
    static int getIdOfCharacter(Character character) {
        checkMapIntegrity()

        for (int i = 0; i < characters.size(); i++)
            if (characters[i] == character.getName())
                return i
        return -1
    }

    // Позволяет получать персонажа не только, создавая его напрямую по имени, но и используя порядковый номер (от нуля).
    static Character getCharacterById(int id) {
        checkMapIntegrity()

        String name = characters.getOrDefault(id, null)
        return (name != null) ? new Character(name) : null
    }

    // Используется для сканирования директории с персонажами и заполнения списка.
    private static Map<Integer, String> readCharacters() {
        Map<Integer, String> characters = new HashMap<>()
        int i = 0

        Path directoryPath = dataDir.resolve("characters")
        if (Files.isDirectory(directoryPath)) {
            try {
                DirectoryStream<Path> directoryStream = Files.newDirectoryStream(directoryPath)
                for (Path characterPath : directoryStream) {
                    if (Files.isDirectory(characterPath) && Files.isDirectory(characterPath.resolve("sprites")) && Files.isDirectory(characterPath.resolve("phrases")))
                        characters.put(i++, characterPath.getFileName().toString())
                }
            } catch (IOException e) {
                e.printStackTrace()
            }
        }
        else
            throw new WrongCharacterException("Directory for characters not found at all!")

        return characters
    }

    // Заполняет список персонажей, если это ещё не было сделано.
    private static void checkMapIntegrity() {
        if (characters == null)
            characters = readCharacters()
    }
}