package utils

const val ANSI_RESET = "\u001B[0m"

enum class LogLevel(val level: Int, val ansiColorCode: String) {
    DEBUG(0, "\u001B[0;36m"),
    INFO(1, "\u001B[0;37m"),
    WARN(2, "\u001B[0;33m"),
    ERROR(3, "\u001B[0;31m")
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
        if (Logger.COLORS) {
            return "${level.ansiColorCode}[${level.name}] [$name] $message$ANSI_RESET"
        }
        return "[${level.name}] [$name] $message$ANSI_RESET"
    }

    companion object {
        val COLORS by lazy { getenv("COLORS", "YES") == "YES" }
    }
}

private val globalLogger = Logger("Global")

fun debug(message: String) = globalLogger.debug(message)
fun info(message: String) = globalLogger.info(message)
fun warn(message: String) = globalLogger.warn(message)
fun error(message: String) = globalLogger.error(message)
