package sample

import kotlinx.cinterop.*
import platform.posix.*

fun readFileText(path: String): String {
    return memScoped {
        val f = fopen(path, "r") ?: throw Exception("Cannot open file $path because: ${posix_errno()}")

        fseek(f, 0, SEEK_END)
        val size = ftell(f)
        fseek(f, 0, SEEK_SET)
        val contents = allocArray<ByteVar>(size)
        fread(contents, 1, size.toULong(), f)
        fclose(f)
        contents.toKString()
    }
}


fun readFileBinary(path: String): CArrayPointer<UByteVar> {
    val f = fopen(path, "r") ?: throw Exception("Cannot open file $path because: ${posix_errno()}")

    fseek(f, 0, SEEK_END)
    val size = ftell(f)
    fseek(f, 0, SEEK_SET)
    val contents = nativeHeap.allocArray<UByteVar>(size)
    fread(contents, 1, size.toULong(), f)
    fclose(f)
    return contents
}