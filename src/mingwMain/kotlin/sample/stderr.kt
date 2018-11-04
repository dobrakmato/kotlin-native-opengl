package sample

import platform.posix.fflush
import platform.posix.fprintf

val STDERR = platform.posix.fdopen(2, "w")
fun printErr(message: String) {
    fprintf(STDERR, message + "\n")
    fflush(STDERR)
}