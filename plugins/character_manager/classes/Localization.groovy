package classes

import exceptions.WrongLocalizationException

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.regex.Matcher
import java.util.regex.Pattern

// Класс, предоставляющий простую абстракцию над файлами resources/localization-**_**.txt.
class Localization {
    private final static Path resourcesPath = Paths.get(Settings.class.protectionDomain.codeSource.location.path)
            .getParent().resolve("resources")
    // По умолчанию используется файл localization.txt. Если его не будет в папке, вылетит WrongLocalizationException!
    private final static Path defaultLocalization = resourcesPath.resolve("localization.txt")

    private static Localization instance

    private Path localizationFile
    private Map<String,String> strings = new HashMap<>()

    private Localization() {
        def language = System.getProperty("user.language")
        def country = System.getProperty("user.country")

        localizationFile = resourcesPath.resolve("localization-${language}_${country}.txt")
        if (!Files.isReadable(localizationFile))
            localizationFile = defaultLocalization

        if (!Files.isReadable(localizationFile))
            throw new WrongLocalizationException("No localization!")

        Files.lines(localizationFile).forEach({ line ->
            Pattern p = Pattern.compile("([A-Za-z_\\-]+)\\s*=\\s*(.+)")
            Matcher m = p.matcher(line)
            while (m.find())
                strings.put(m.group(1), m.group(2))
        })
    }

    static Localization getInstance() {
        if (instance == null)
            instance = new Localization()

        return instance
    }

    // Геттер.
    String get(String key) {
        return strings.getOrDefault(key, '')
    }
}
