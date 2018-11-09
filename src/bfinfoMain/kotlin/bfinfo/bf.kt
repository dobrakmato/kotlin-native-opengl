package bfinfo

import kotlinx.cinterop.*
import kotlin.experimental.and
import kotlin.math.max


const val BF_MAGIC = 1766222146
const val BF_EXTENSION = "bf"
const val BF_VERSION: Byte = 1

const val BF_HEADER_SIZE = 8
const val BF_HEADER_IMAGE_SIZE = BF_HEADER_SIZE + 4 + 4

const val BF_FILE_IMAGE: Byte = 1
const val BF_FILE_GEOMETRY: Byte = 2
const val BF_FILE_AUDIO: Byte = 3
const val BF_FILE_MATERIAL: Byte = 4
const val BF_FILE_FILESYSTEM: Byte = 5
const val BF_FILE_COMPILED_SHADER: Byte = 6

const val BF_IMAGE_FLAG_LZ4 = 1
const val BF_IMAGE_FLAG_LZ4_HC = 2
const val BF_IMAGE_FLAG_DXT = 4
const val BF_IMAGE_FLAG_FLOAT = 8
const val BF_IMAGE_FLAG_16_BIT = 16
const val BF_IMAGE_FLAG_VERTICAL_FLIP = 32
const val BF_IMAGE_FLAG_GAMMA_SRGB = 64
const val BF_IMAGE_FLAG_SKYBOX = 128

data class BFImageHeader(
    val magic: Int = BF_MAGIC,

    val version: Byte,
    val fileType: Byte, // 1 = image, 2 = geometry, 3 = audio, 4 = material, 5 = vfs, 6 = compiled shader
    val flags: BFImageHeaderFlags, // 1 = lz4, 2 = hc, 4 = dxt, 8 = floating point, 16 = 16 bit texture, 32 = vertically flipped, 64 = gamma (SRGB), 128 = skybox
    val extra: BFImageHeaderExtra, // [_ _ _ _] mipmap levels [_] inline mipmaps [_ _ _] channels

    val width: Short,
    val height: Short,

    val uncompressedSize: Int
)

inline class BFImageHeaderFlags(val value: Byte) {
    inline fun lz4() = (value.toInt() and BF_IMAGE_FLAG_LZ4) == BF_IMAGE_FLAG_LZ4
    inline fun lz4hc() = (value.toInt() and BF_IMAGE_FLAG_LZ4_HC) == BF_IMAGE_FLAG_LZ4_HC
    inline fun dxt() = (value.toInt() and BF_IMAGE_FLAG_DXT) == BF_IMAGE_FLAG_DXT
    inline fun float() = (value.toInt() and BF_IMAGE_FLAG_FLOAT) == BF_IMAGE_FLAG_FLOAT
    inline fun is16bit() = (value.toInt() and BF_IMAGE_FLAG_16_BIT) == BF_IMAGE_FLAG_16_BIT
    inline fun verticallyFlipped() = (value.toInt() and BF_IMAGE_FLAG_VERTICAL_FLIP) == BF_IMAGE_FLAG_VERTICAL_FLIP
    inline fun srgb() = (value.toInt() and BF_IMAGE_FLAG_GAMMA_SRGB) == BF_IMAGE_FLAG_GAMMA_SRGB
    inline fun skybox() = (value.toInt() and BF_IMAGE_FLAG_SKYBOX) == BF_IMAGE_FLAG_SKYBOX
}

inline class BFImageHeaderExtra(val value: Byte) {
    fun hasMipmaps() = (value.toInt() and 8) == 8
    fun includedMipmaps() = max(1, (value.toInt() and 0b11110000) shr 4)
    fun numberOfChannels() = (value.toInt() and 0b00000111)
}

fun createImageExtraHeader(numberOfChannels: Int, includedMipmaps: Int): BFImageHeaderExtra {
    var result: Int = 0

    result = result or (numberOfChannels and 0b00000111)
    if (includedMipmaps > 0) {
        result = result or (0b00001000)
        result = result or ((0b00001111 and includedMipmaps) shr 4)
    }

    return BFImageHeaderExtra(result.toByte())
}

fun combineFlags(vararg flags: Int): Byte {
    var result = 0
    for (flag in flags) {
        result = result or flag
    }
    return result.toByte()
}


fun readBFHeader(arr: CArrayPointer<ByteVar>): BFImageHeader {
    val buff = ByteBuffer(arr)

    val magic = buff.readInt()

    if (magic != BF_MAGIC) {
        throw Exception("Not a bf file!")
    }

    val version = buff.readByte()
    val fileType = buff.readByte()
    val flags = BFImageHeaderFlags(buff.readByte())
    val extra = BFImageHeaderExtra(buff.readByte())

    val width = buff.readShort()
    val height = buff.readShort()

    val uncompressedSize = if (flags.lz4()) buff.readInt() else -1

    return BFImageHeader(BF_MAGIC, version, fileType, flags, extra, width, height, uncompressedSize)
}

fun createBFHeader(memScope: MemScope, header: BFImageHeader): CArrayPointer<ByteVar> {
    val arr = memScope.allocArray<ByteVar>(BF_HEADER_IMAGE_SIZE)
    val buff = ByteBuffer(arr)

    buff.writeInt(BF_MAGIC)

    buff.writeByte(header.version)
    buff.writeByte(header.fileType)
    buff.writeByte(header.flags.value)
    buff.writeByte(header.extra.value)

    buff.writeShort(header.width)
    buff.writeShort(header.height)

    if (header.flags.lz4()) {
        buff.writeInt(header.uncompressedSize)
    }

    return arr
}


class ByteBuffer(private val ptr: CArrayPointer<ByteVar>) {

    private var index: Int = 0

    fun readByte(): Byte = ptr[index++].toByte()
    private fun readUByte(): UByte = ptr[index++].toUByte()

    fun writeByte(value: Byte) {
        ptr[index++] = value
    }

    fun readShort(): Short {
        val a = readByte()
        val b = readByte()

        return (a.toInt() or
                (b.toInt() shl 8)).toShort()
    }

    fun writeShort(value: Short) {
        val a = value and 0xFF
        val b = (value.toInt() shr 8) and 0xFF

        writeByte(a.toByte())
        writeByte(b.toByte())
    }

    fun readInt(): Int {
        val a = readUByte()
        val b = readUByte()
        val c = readUByte()
        val d = readUByte()

        return (a.toUInt() or
                (b.toUInt() shl 8) or
                (c.toUInt() shl 16) or
                (d.toUInt() shl 24)).toInt()
    }

    fun writeInt(value: Int) {
        val a = value and 0xFF
        val b = (value shr 8) and 0xFF
        val c = (value shr 16) and 0xFF
        val d = (value shr 24) and 0xFF

        writeByte(a.toByte())
        writeByte(b.toByte())
        writeByte(c.toByte())
        writeByte(d.toByte())
    }

}
