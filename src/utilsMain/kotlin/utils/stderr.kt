package utils

import platform.posix.fflush
import platform.posix.fprintf

val COLORED_STDERR by lazy { getenv("COLOR", "YES") == "YES" }

val STDERR = platform.posix.fdopen(2, "w")

fun printErr(message: String) {
    if (COLORED_STDERR) {
        fprintf(STDERR, "\u001B[1;91m$message\u001B[0m\n")
    } else {
        fprintf(STDERR, message)
    }
    fflush(STDERR)
}