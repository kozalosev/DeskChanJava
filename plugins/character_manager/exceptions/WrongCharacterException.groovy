package exceptions

// Исключение, которое выбрасывается в случае проблем с чтением файлов персонажа.
// Например, у него может не быть фраз, использующихся по умолчанию или спрайта normal.png.
class WrongCharacterException extends RuntimeException {
    WrongCharacterException(String string) {
        super(string)
    }

    WrongCharacterException() {
        super()
    }
}
