package sample

import platform.posix.fflush
import platform.posix.fprintf

const val COLORED_STDERR = true

val STDERR = platform.posix.fdopen(2, "w")
fun printErr(message: String) {
    if (COLORED_STDERR) {
        fprintf(STDERR, "\u001B[1;91m$message\u001B[0m\n")
    } else {
        fprintf(STDERR, message)
    }
    fflush(STDERR)
}