package classes

import java.nio.file.Path

// Заимствованный и модифицированный класс Ивана Колесникова (KivApple) для хранения информации о спрайтах.
class SkinInfo implements Comparable<SkinInfo> {

    String name
    Path path
    boolean isSet

    SkinInfo(String name, Path path, boolean isSet) {
        this.name = name
        this.path = path
        this.isSet = isSet
    }

    SkinInfo(String name, Path path) {
        this(name, path, false)
    }

    SkinInfo(Path path, boolean isSet) {
        this(path.getFileName().toString(), path, isSet)
    }

    SkinInfo(Path path) {
        this(path, false)
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