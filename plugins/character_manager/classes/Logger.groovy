package classes


// Данный статический класс нужен для сохранения и переноса ссылки на метод GroovyPlugin.log между остальными классами.
abstract class Logger {
    private static Closure<String> strLogger
    private static Closure<Throwable> errLogger

    // Без вызова этого метода логироваться ничего не будет!
    static def init(Closure<String> strLogger, Closure<Throwable> errLogger) {
        this.strLogger = strLogger
        this.errLogger = errLogger
    }

    // Заносит сообщение в лог.
    static def log(String text) {
        if (strLogger != null)
            strLogger(text)
    }

    // Заносит исключение в лог.
    static def log(Throwable err) {
        if (errLogger != null)
            errLogger(err)
    }

    // Позволяет передавать объекты, не приводя их к строкам.
    static def log(Object obj) {
        log(obj.toString())
    }
}