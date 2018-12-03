package io

import kotlinx.cinterop.*
import platform.posix.*

/* paths */

class Path(val path: String) {
    val extension
        get() = path.substring(0, path.lastIndexOf('.'))

    fun isFile(): Boolean {
        return memScoped {
            (stat(this).st_mode and 61440u).toUInt() == 32768u
        }
    }

    fun isDirectory(): Boolean {
        return memScoped {
            (stat(this).st_mode and 61440u).toUInt() == 16384u
        }
    }

    fun size(): Int {
        return memScoped {
            stat(this).st_size
        }
    }

    fun basename(): String {
        return basename(path.cstr)!!.toKString() // unneeded allocations
    }

    fun dirname(): String {
        return dirname(path.cstr)!!.toKString() // unneeded allocations
    }

    private fun stat(memScope: MemScope): stat {
        val stat = memScope.alloc<stat>()
        stat(path, stat.ptr)
        return stat
    }

}

fun Path.withExtension(newExtension: String): Path {
    val idx = path.lastIndexOf(':')
    if (idx == -1) {
        throw IllegalArgumentException("Path $path has no extension!")
    }
    return Path(path.substring(0, idx) + (if (newExtension.startsWith('.')) "" else '.') + newExtension)
}

/* file data */

data class FileData(val size: Long, val data: CArrayPointer<ByteVar>)

inline fun FileData.byteAt(pos: Int): Byte = data[pos]
inline fun FileData.uByteAt(pos: Int): UByte = data[pos].toUByte()
inline fun FileData.shortAt(pos: Int): Short {
    val a = data[pos]
    val b = data[pos + 1]

    return (a.toInt() or
            (b.toInt() shl 8)).toShort()
}

inline fun FileData.uShortAt(pos: Int): UShort = this.shortAt(pos).toUShort()

inline fun FileData.intAt(pos: Int): Int {
    val a = data[pos]
    val b = data[pos + 1]
    val c = data[pos + 2]
    val d = data[pos + 3]

    return a.toInt() or
            (b.toInt() shl 8) or
            (c.toInt() shl 16) or
            (d.toInt() shl 24)
}

inline fun FileData.uIntAt(pos: Int): UInt = this.intAt(pos).toUInt()

inline fun FileData.longAt(pos: Int): Long {
    val a = data[pos]
    val b = data[pos + 1]
    val c = data[pos + 2]
    val d = data[pos + 3]
    val e = data[pos + 4]
    val f = data[pos + 5]
    val g = data[pos + 6]
    val h = data[pos + 7]

    return a.toLong() or
            (b.toLong() shl 8) or
            (c.toLong() shl 16) or
            (d.toLong() shl 24) or
            (e.toLong() shl 32) or
            (f.toLong() shl 40) or
            (g.toLong() shl 48) or
            (h.toLong() shl 56)
}

inline fun FileData.uLongAt(pos: Int): ULong = this.longAt(pos).toULong()

inline fun FileData.floatAt(pos: Int): Float = Float.fromBits(intAt(pos))
inline fun FileData.doubleAt(pos: Int): Double = Double.fromBits(longAt(pos))

inline fun FileData.create(bytes: Long): FileData {
    return FileData(bytes, nativeHeap.allocArray(bytes))
}

inline fun FileData.free() {
    nativeHeap.free(data)
}

/* file data buffer */

class FileDataBuffer(private val fileData: FileData) {
    private var pos: Int = 0

    fun readByte(): Byte = fileData.byteAt(pos++)
    fun readShort(): Short {
        val r = fileData.shortAt(pos)
        pos += 2
        return r
    }

    fun readInt(): Int {
        val r = fileData.intAt(pos)
        pos += 4
        return r
    }

    fun readLong(): Long {
        val r = fileData.longAt(pos)
        pos += 8
        return r
    }

    fun readFloat(): Float {
        val r = fileData.floatAt(pos)
        pos += 4
        return r
    }

    fun readDouble(): Double {
        val r = fileData.doubleAt(pos)
        pos += 8
        return r
    }

    fun readUByte(): UByte = readByte().toUByte()
    fun readUShort(): UShort = readShort().toUShort()
    fun readUInt(): UInt = readInt().toUInt()
    fun readULong(): ULong = readLong().toULong()
}


/* read functions */

fun readTextFile(path: String): String {
    return memScoped {
        val f = fopen(path, "rb") ?: throw Exception("Cannot open file $path because: ${posix_errno()}")

        fseek(f, 0, SEEK_END)
        val size = ftell(f)
        fseek(f, 0, SEEK_SET)
        val contents = allocArray<ByteVar>(size)
        fread(contents, 1, size.toULong(), f)
        fclose(f)
        contents.toKString()
    }
}

fun readBinaryFile(path: String): FileData {
    val f = fopen(path, "rb") ?: throw Exception("Cannot open file $path because: ${posix_errno()}")

    fseek(f, 0, SEEK_END)
    val size = ftell(f)
    fseek(f, 0, SEEK_SET)
    val contents = nativeHeap.allocArray<ByteVar>(size)
    fread(contents, 1, size.toULong(), f)
    fclose(f)
    return FileData(size.toLong(), contents)
}