package io

import kotlinx.cinterop.*
import platform.posix.*
import kotlin.experimental.and

/* paths */

class Path(val path: String) {
    private val directorySeparator = '/'
    val extension
        get(): String {
            val lastDot = path.lastIndexOf('.')
            if (path.indexOf(directorySeparator, lastDot) >= 0) {
                return ""
            }
            return path.substring(lastDot + 1)
        }

    val filename
        get() : String {
            return path.substring(path.lastIndexOf(directorySeparator) + 1)
        }

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

    fun exists(): Boolean {
        return memScoped {
            val stat = memScope.alloc<stat>()
            stat(path, stat.ptr)
        } == 0
    }

    private fun stat(memScope: MemScope): stat {
        val stat = memScope.alloc<stat>()
        stat(path, stat.ptr)
        return stat
    }

    fun relativize(path: Path): Path {
        val cleanLeft = this.path.trimEnd(directorySeparator)
        val cleanRight = path.path.trimStart(directorySeparator)

        return Path(cleanLeft + directorySeparator + cleanRight)
    }

    override fun toString(): String {
        return path
    }

    companion object {
        fun join(vararg paths: Path): Path {
            var r = paths[0]

            for (i in 1 until paths.size) {
                r = r.relativize(paths[i])
            }
            return r
        }
    }
}

inline fun Path.withExtension(newExtension: String): Path {
    val idx = path.lastIndexOf('.')
    if (idx == -1) {
        throw IllegalArgumentException("Path $path has no extension!")
    }
    return Path(path.substring(0, idx) + (if (newExtension.startsWith('.')) "" else '.') + newExtension)
}

/* file data */

data class ByteBuffer(val size: Long, val data: CArrayPointer<UByteVar>) {
    var pos: Int = 0

    override fun equals(other: Any?): Boolean {
        if (other is ByteBuffer) {
            if (other.size != size) return false
            return memcmp(data, other.data, size.toULong()) == 0
        }
        return false
    }

    override fun hashCode(): Int {
        return data.hashCode()
    }

    inline fun readByte(): Byte = byteAt(pos++)
    inline fun readShort(): Short {
        val r = shortAt(pos)
        pos += 2
        return r
    }

    inline fun readInt(): Int {
        val r = intAt(pos)
        pos += 4
        return r
    }

    inline fun readLong(): Long {
        val r = longAt(pos)
        pos += 8
        return r
    }

    inline fun readFloat(): Float {
        val r = floatAt(pos)
        pos += 4
        return r
    }

    inline fun readDouble(): Double {
        val r = doubleAt(pos)
        pos += 8
        return r
    }

    inline fun writeByte(value: Byte) = byteAt(pos++, value)

    inline fun writeShort(value: Short) {
        shortAt(pos, value)
        pos += 2
    }

    inline fun writeInt(value: Int) {
        intAt(pos, value)
        pos += 4
    }

    inline fun writeLong(value: Long) {
        longAt(pos, value)
        pos += 8
    }

    inline fun writeFloat(value: Float) {
        floatAt(pos, value)
        pos += 4
    }

    inline fun writeDouble(value: Double) {
        doubleAt(pos, value)
        pos += 8
    }

    inline fun writeUByte(value: UByte) = writeByte(value.toByte())
    inline fun writeUShort(value: UShort) = writeShort(value.toShort())
    inline fun writeUInt(value: UInt) = writeInt(value.toInt())
    inline fun writeULong(value: ULong) = writeLong(value.toLong())

    inline fun readUByte(): UByte = readByte().toUByte()
    inline fun readUShort(): UShort = readShort().toUShort()
    inline fun readUInt(): UInt = readInt().toUInt()
    inline fun readULong(): ULong = readLong().toULong()

    inline fun writeString(value: String) {
        val utf8 = value.toUtf8OrThrow()
        writeInt(utf8.size)
        checkBounds(pos + utf8.size - 1) // check the position of last byte

        /* create offset pointer for memcpy */
        val dst = (data + pos.toLong())
        memcpy(dst, utf8.refTo(0), utf8.size.toULong())
        pos += utf8.size
    }

    inline fun readString(): String {
        val size = readInt()
        checkBounds(pos + size - 1) // check the position of last byte
        val utf8 = ByteArray(size)

        val src = pointerToPosition()
        val dst = utf8.refTo(0)
        memcpy(dst, src, size.toULong())
        pos += size

        return utf8.stringFromUtf8()
    }

    inline fun writeBytes(src: CArrayPointer<UByteVar>, length: Int) {
        checkBounds(pos + length - 1)
        memcpy(pointerToPosition(), src, length.toULong())
    }

    inline fun readBytes(dest: CArrayPointer<UByteVar>, length: Int) {
        checkBounds(pos + length -1)
        memcpy(dest, pointerToPosition(), length.toULong())
    }

    inline fun pointerTo(index: Int): CPointer<UByteVarOf<UByte>> {
        if (index < 0) throw IllegalArgumentException("Index must be grater or equal to zero.")
        return (this.data + index.toLong())!!
    }

    inline fun pointerToPosition() = pointerTo(pos)

    companion object {
        inline fun create(bytes: Long): ByteBuffer {
            return ByteBuffer(bytes, nativeHeap.allocArray(bytes))
        }
    }
}

inline fun ByteBuffer.checkBounds(idx: Int) {
    if (idx < 0 || idx >= size) throw IllegalArgumentException("Index $idx will be out of bounds <0, $size)!")
}

inline fun ByteBuffer.byteAt(pos: Int): Byte {
    checkBounds(pos)
    return data[pos].toByte()
}

inline fun ByteBuffer.byteAt(pos: Int, value: Byte) {
    checkBounds(pos)
    data[pos] = value.toUByte()
}

inline fun ByteBuffer.uByteAt(pos: Int): UByte {
    checkBounds(pos)
    return data[pos].toUByte()
}

inline fun ByteBuffer.uByteAt(pos: Int, value: UByte) = this.byteAt(pos, value.toByte())

inline fun ByteBuffer.shortAt(pos: Int): Short {
    checkBounds(pos + 1) // check the position of last byte
    val a = data[pos]
    val b = data[pos + 1]

    return (a.toInt() or
            (b.toInt() shl 8)).toShort()
}

inline fun ByteBuffer.shortAt(pos: Int, value: Short) {
    checkBounds(pos + 1) // check the position of last byte
    val a = value and 0xFF
    val b = (value.toInt() shr 8) and 0xFF

    data[pos] = a.toUByte()
    data[pos + 1] = b.toUByte()
}

inline fun ByteBuffer.uShortAt(pos: Int): UShort = this.shortAt(pos).toUShort()
inline fun ByteBuffer.uShortAt(pos: Int, value: UShort) = this.shortAt(pos, value.toShort())


inline fun ByteBuffer.intAt(pos: Int): Int {
    checkBounds(pos + 3) // check the position of last byte
    val a = data[pos]
    val b = data[pos + 1]
    val c = data[pos + 2]
    val d = data[pos + 3]

    return a.toInt() or
            (b.toInt() shl 8) or
            (c.toInt() shl 16) or
            (d.toInt() shl 24)
}

inline fun ByteBuffer.intAt(pos: Int, value: Int) {
    checkBounds(pos + 3) // check the position of last byte
    val a = value and 0xFF
    val b = (value shr 8) and 0xFF
    val c = (value shr 16) and 0xFF
    val d = (value shr 24) and 0xFF

    data[pos] = a.toUByte()
    data[pos + 1] = b.toUByte()
    data[pos + 2] = c.toUByte()
    data[pos + 3] = d.toUByte()
}

inline fun ByteBuffer.uIntAt(pos: Int): UInt = this.intAt(pos).toUInt()
inline fun ByteBuffer.uIntAt(pos: Int, value: UInt) = this.intAt(pos, value.toInt())

inline fun ByteBuffer.longAt(pos: Int): Long {
    checkBounds(pos + 7) // check the position of last byte
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

inline fun ByteBuffer.longAt(pos: Int, value: Long) {
    checkBounds(pos + 7) // check the position of last byte
    val a = value and 0xFF
    val b = (value shr 8) and 0xFF
    val c = (value shr 16) and 0xFF
    val d = (value shr 24) and 0xFF
    val e = (value shr 32) and 0xFF
    val f = (value shr 40) and 0xFF
    val g = (value shr 48) and 0xFF
    val h = (value shr 56) and 0xFF

    data[pos] = a.toUByte()
    data[pos + 1] = b.toUByte()
    data[pos + 2] = c.toUByte()
    data[pos + 3] = d.toUByte()
    data[pos + 4] = e.toUByte()
    data[pos + 5] = f.toUByte()
    data[pos + 6] = g.toUByte()
    data[pos + 7] = h.toUByte()
}

inline fun ByteBuffer.uLongAt(pos: Int): ULong = this.longAt(pos).toULong()
inline fun ByteBuffer.uLongAt(pos: Int, value: ULong) = this.longAt(pos, value.toLong())

inline fun ByteBuffer.floatAt(pos: Int): Float = Float.fromBits(intAt(pos))
inline fun ByteBuffer.floatAt(pos: Int, value: Float) = this.intAt(pos, value.toBits())
inline fun ByteBuffer.doubleAt(pos: Int): Double = Double.fromBits(longAt(pos))
inline fun ByteBuffer.doubleAt(pos: Int, value: Double) = this.longAt(pos, value.toBits())

inline fun ByteBuffer.free() {
    nativeHeap.free(data)
}

/* file read & write functions */

fun writeUTF8TextFile(path: String, data: String) {
    val f = fopen(path, "wb") ?: throw Exception("Cannot open file $path because: ${posix_errno()}")
    val utf8 = data.toUtf8OrThrow()

    fwrite(utf8.refTo(0), 1, utf8.size.toULong(), f)
    fclose(f)
}

fun readUTF8TextFile(path: String): String {
    val f = fopen(path, "rb") ?: throw Exception("Cannot open file $path because: ${posix_errno()}")

    fseek(f, 0, SEEK_END)
    val size = ftell(f)
    fseek(f, 0, SEEK_SET)
    val utf8 = ByteArray(size)
    fread(utf8.refTo(0), 1, size.toULong(), f)
    fclose(f)
    return utf8.stringFromUtf8OrThrow()
}

fun readBinaryFile(path: String): ByteBuffer {
    val f = fopen(path, "rb") ?: throw Exception("Cannot open file $path because: ${posix_errno()}")

    fseek(f, 0, SEEK_END)
    val size = ftell(f)
    fseek(f, 0, SEEK_SET)
    val contents = nativeHeap.allocArray<UByteVar>(size)
    fread(contents, 1, size.toULong(), f)
    fclose(f)
    return ByteBuffer(size.toLong(), contents)
}

fun writeBinaryFile(path: String, data: ByteBuffer) {
    val f = fopen(path, "wb") ?: throw Exception("Cannot open file $path because: ${posix_errno()}")
    fwrite(data.data, 1UL, data.size.toULong(), f)
    fclose(f)
}
