package classes

import org.apache.commons.io.FileUtils
import org.json.JSONObject

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class Settings {
    private final static Path settingsFile = Paths.get(Settings.class.protectionDomain.codeSource.location.path)
        .getParent().resolve("resources").resolve("settings.json")

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

    String get(String key) {
        if (settings.containsKey(key))
            return settings[key]
        else
            return null
    }

    void put(String key, String value, boolean writeOnDisk) {
        settings.put(key, value)
        if (writeOnDisk)
            save()
    }

    void put(String key, String value) {
        put(key, value, true)
    }

    void save() {
        String json = new JSONObject(settings).toString()
        FileUtils.writeStringToFile(settingsFile.toFile(), json)
    }
}