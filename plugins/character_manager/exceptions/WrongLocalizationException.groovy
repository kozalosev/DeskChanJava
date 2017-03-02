package exceptions

// Исключение, выбрасывающееся в случае проблем с загрузкой файлов локализации.
// Например, когда нет ни файла под текущую систему, ни файла локализации по умолчанию.
class WrongLocalizationException extends RuntimeException {
    WrongLocalizationException(String string) {
        super(string)
    }

    WrongLocalizationException() {
        super()
    }
}