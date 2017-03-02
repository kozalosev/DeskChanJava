package exceptions

class WrongLocalizationException extends RuntimeException {
    WrongLocalizationException(String string) {
        super(string)
    }

    WrongLocalizationException() {
        super()
    }
}