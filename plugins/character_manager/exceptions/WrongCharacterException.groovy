package exceptions

class WrongCharacterException extends RuntimeException {
    WrongCharacterException(String string) {
        super(string)
    }

    WrongCharacterException() {
        super()
    }
}
