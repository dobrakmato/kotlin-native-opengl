package sample

import kotlinx.cinterop.*
import platform.posix.*

inline class Path(private val path: String) {
    fun isAbsolute() = false
    fun basename() = platform.posix.basename(path.cstr)?.toKString()
    fun exists() = platform.posix.stat(path, null) == 0
    fun dirname() = platform.posix.dirname(path.cstr)?.toKString()
}

@Target(AnnotationTarget.TYPE, AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.PROPERTY)
@MustBeDocumented
annotation class Mallocated


typealias FileData = @Mallocated CArrayPointer<UByteVar>

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


fun readFileBinary(path: String): Pair<FileData, Int> {
    val f = fopen(path, "rb") ?: throw Exception("Cannot open file $path because: ${posix_errno()}")

    fseek(f, 0, SEEK_END)
    val size = ftell(f)
    fseek(f, 0, SEEK_SET)
    val contents = nativeHeap.allocArray<UByteVar>(size)
    fread(contents, 1, size.toULong(), f)
    fclose(f)
    return Pair(contents, size)
}

/* ArrayPointer extension methods. */

fun FileData.integerAt(pos: Int): Int {
    val a = this[pos]
    val b = this[pos + 1]
    val c = this[pos + 2]
    val d = this[pos + 3]

    return a.toInt() or
            (b.toInt() shl 8) or
            (c.toInt() shl 16) or
            (d.toInt() shl 24)
}