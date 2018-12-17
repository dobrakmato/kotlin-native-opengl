package utils

enum class LogLevel(val level: Int) {
    DEBUG(0),
    INFO(1),
    WARN(2),
    ERROR(3)
}

class Logger(private val name: String) {

    var logLevel: LogLevel = LogLevel.DEBUG

    inline fun debug(message: String) = log(LogLevel.DEBUG, message)
    inline fun info(message: String) = log(LogLevel.INFO, message)
    inline fun warn(message: String) = log(LogLevel.WARN, message)
    inline fun error(message: String) = log(LogLevel.ERROR, message)

    fun log(level: LogLevel, message: String) {
        if (level.level >= logLevel.level) {
            println(format(level, message))
        }
    }

    private fun format(level: LogLevel, message: String): String {
        return "[${level.name}] [$name] $message"
    }
}

private val globalLogger = Logger("Global")

fun debug(message: String) = globalLogger.debug(message)
fun info(message: String) = globalLogger.info(message)
fun warn(message: String) = globalLogger.warn(message)
fun error(message: String) = globalLogger.error(message)
