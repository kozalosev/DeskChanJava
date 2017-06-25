package classes

import javafx.scene.media.Media
import javafx.scene.media.MediaException

import java.nio.file.Files
import java.nio.file.Path

import static classes.ResourcesLoader.CHARACTERS_PATH


// Этот класс предназначен для загрузки нужных звуков, если они есть.
class SoundLoader {
    private Path soundDirPath
    private int lastPlayedId = 0

    SoundLoader(String characterName) {
        soundDirPath = CHARACTERS_PATH.resolve(characterName).resolve('sounds')
    }

    Media tryGetSound(String soundName) {
        Path[] filepaths = Files.list(soundDirPath).filter({ path ->
            path.getFileName().toString().matches('^' + soundName + '(-[0-9]+)?\\.mp3$') \
                && Files.isReadable(path)
        }).toArray({ size -> new Path[size] })

        int id = 0
        if (filepaths.length == 0) {
            return null
        } else if (filepaths.length > 1) {
            Random rand = new Random()
            int fuse = 50
            id = lastPlayedId
            while (id == lastPlayedId && fuse > 0) {
                id = rand.nextInt(filepaths.length)
                fuse--
            }
            lastPlayedId = id
        }

        Media sound = null
        try {
            sound = new Media(filepaths[id].toUri().toString())
        } catch (MediaException | IOException e) {
            e.printStackTrace()
        }
        return sound
    }
}
