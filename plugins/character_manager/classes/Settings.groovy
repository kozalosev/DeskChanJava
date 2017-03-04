package classes

import org.apache.commons.io.FileUtils
import org.json.JSONObject

import java.nio.file.Files
import java.nio.file.Path

// Синглтон, предоставляющий простую абстракцию над файлом resources/settings.json.
class Settings {
    private final static Path settingsFile = CharacterManager.getDataDir().resolve("settings.json")

    private Map<String, String> settings = new HashMap<>()
    static Settings instance

    private Settings() {
        String fileContent = (Files.isReadable(settingsFile)) ? new String(Files.readAllBytes(settingsFile)) : "{}"
        JSONObject obj = new JSONObject(fileContent.trim())
        Iterator<?> keys = obj.keys()

        while (keys.hasNext()) {
            String key = (String) keys.next()
            String value = obj.getString(key)
            settings.put(key, value)
        }
    }

    static Settings getInstance() {
        if (instance == null)
            instance = new Settings()

        return instance
    }

    // Геттер.
    String get(String key) {
        if (settings.containsKey(key))
            return settings[key]
        else
            return null
    }

    // Сеттер.
    void put(String key, String value, boolean writeOnDisk) {
        settings.put(key, value)
        if (writeOnDisk)
            save()
    }

    // Без явного отключения, все изменения автоматически сохраняются в файл.
    void put(String key, String value) {
        put(key, value, true)
    }

    // Сохраняет настройки в файл.
    void save() {
        String json = new JSONObject(settings).toString()
        FileUtils.writeStringToFile(settingsFile.toFile(), json)
    }
}