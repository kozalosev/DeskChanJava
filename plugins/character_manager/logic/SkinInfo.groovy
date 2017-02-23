package character_manager.logic

import java.nio.file.Path

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