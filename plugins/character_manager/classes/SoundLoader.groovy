package classes

import javafx.scene.media.Media
import javafx.scene.media.MediaException

import java.nio.file.Files
import java.nio.file.Path

import static classes.ResourcesLoader.CHARACTERS_PATH


// Этот класс предназначен для загрузки нужных звуков, если они есть.
class SoundLoader {
    private Path soundDirPath

    SoundLoader(String characterName) {
        soundDirPath = CHARACTERS_PATH.resolve(characterName).resolve('sounds')
    }

    Media tryGetSound(String soundName) {
        Path soundPath = soundDirPath.resolve(soundName + ".mp3")
        Media sound = null
        if (Files.isReadable(soundPath)) {
            try {
                sound = new Media(soundPath.toUri().toString())
            } catch (MediaException | IOException e) {
                e.printStackTrace()
            }
        }
        return sound
    }
}
