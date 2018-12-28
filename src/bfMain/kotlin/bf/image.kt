package bf

import io.ByteBuffer
import platform.posix.pow
import kotlin.math.max

/* image flags */
typealias BfImageFlag = UByte

const val BF_IMAGE_FLAG_LZ4: BfImageFlag = 1u
const val BF_IMAGE_FLAG_LZ4_HC: BfImageFlag = 2u
const val BF_IMAGE_FLAG_DXT: BfImageFlag = 4u
const val BF_IMAGE_FLAG_FLOAT: BfImageFlag = 8u
const val BF_IMAGE_FLAG_16_BIT: BfImageFlag = 16u
const val BF_IMAGE_FLAG_VERTICAL_FLIP: BfImageFlag = 32u
const val BF_IMAGE_FLAG_GAMMA_SRGB: BfImageFlag = 64u
const val BF_IMAGE_FLAG_SKYBOX: BfImageFlag = 128u

/* image data structures */
inline class BfImageFlags(val value: UByte) {
    inline fun lz4() = (value and BF_IMAGE_FLAG_LZ4) == BF_IMAGE_FLAG_LZ4
    inline fun lz4hc() = (value and BF_IMAGE_FLAG_LZ4_HC) == BF_IMAGE_FLAG_LZ4_HC
    inline fun dxt() = (value and BF_IMAGE_FLAG_DXT) == BF_IMAGE_FLAG_DXT
    inline fun float() = (value and BF_IMAGE_FLAG_FLOAT) == BF_IMAGE_FLAG_FLOAT
    inline fun is16bit() = (value and BF_IMAGE_FLAG_16_BIT) == BF_IMAGE_FLAG_16_BIT
    inline fun verticallyFlipped() = (value and BF_IMAGE_FLAG_VERTICAL_FLIP) == BF_IMAGE_FLAG_VERTICAL_FLIP
    inline fun srgb() = (value and BF_IMAGE_FLAG_GAMMA_SRGB) == BF_IMAGE_FLAG_GAMMA_SRGB
    inline fun skybox() = (value and BF_IMAGE_FLAG_SKYBOX) == BF_IMAGE_FLAG_SKYBOX

    companion object {
        inline val SIZE_BYTES: Int
            get() {
                return 1
            }

        fun create() = BfImageFlags(0u)
    }
}

fun BfImageFlags.with(flag: UByte) = BfImageFlags(this.value or flag)

inline class BfImageExtra(val value: UByte) { // [_ _ _ _] mipmap levels [_] inline mipmaps [_ _ _] channels
    inline fun hasMipmaps() = (value and 8u) == 8u.toUByte()
    inline fun includedMipmaps() = ((value and 0b11110000u).toUInt() shr 4).toInt()
    inline fun numberOfChannels() = (value and 0b00000111u).toInt()

    companion object {
        inline val SIZE_BYTES: Int
            get() {
                return 1
            }

        fun create(numberOfChannels: Int, includedMipmaps: Int): BfImageExtra {
            if (numberOfChannels <= 0 || numberOfChannels > 4) throw IllegalArgumentException("Invalid number of channels. $numberOfChannels is not from (0; 4>")
            if (includedMipmaps < 0 || includedMipmaps > 15) throw IllegalArgumentException("Invalid number of mipmaps. $includedMipmaps is not from <0; 15>")

            var result = 0u

            result = result or (numberOfChannels.toUInt() and 0b00000111u)
            if (includedMipmaps > 0) {
                result = result or (0b00001000u) // set mipmaps flag
                result = result or ((0b00001111u and includedMipmaps.toUInt()) shl 4)
            }

            return BfImageExtra(result.toUByte())
        }
    }
}

data class BfImageHeader(
    val header: BfHeader,
    val flags: BfImageFlags,
    val extra: BfImageExtra,

    val width: UShort,
    val height: UShort
) {
    companion object {
        inline val SIZE_BYTES: Int
            get() {
                return BfHeader.SIZE_BYTES + BfImageFlags.SIZE_BYTES + BfImageExtra.SIZE_BYTES + 2 + 2
            }
    }

}

fun computeBfImagePayloadSize(bfImageHeader: BfImageHeader): Int {
    /*
     * 1 = original
     * 2 = half size
     * 3 = quarter size
     */
    fun mipmap(level: Int): Int {
        val pixels = (bfImageHeader.width / (level.toUInt())) * (bfImageHeader.height / (level.toUInt()))
        var result = pixels * bfImageHeader.extra.numberOfChannels().toUInt()
        if (bfImageHeader.flags.dxt()) result /= if (bfImageHeader.extra.numberOfChannels() == 3) 6u else 4u
        return result.toInt()
    }

    if (!bfImageHeader.extra.hasMipmaps()) return mipmap(1)
    var sum = 0
    for (lvl in 0..bfImageHeader.extra.includedMipmaps()) {
        sum += mipmap(pow(2.0, lvl.toDouble()).toInt())
    }
    return sum
}

fun ByteBuffer.writeBfImageHeader(bfImageHeader: BfImageHeader) {
    writeBfHeader(bfImageHeader.header)
    writeBfImageFlags(bfImageHeader.flags)
    writeBfImageExtra(bfImageHeader.extra)
    writeUShort(bfImageHeader.width)
    writeUShort(bfImageHeader.height)
}

fun ByteBuffer.readBfImageHeader(): BfImageHeader {
    val bfHeader = readBfHeader()
    val bfImageFlags = readBfImageFlags()
    val bfImageExtra = readBfImageExtra()
    val width = readUShort()
    val height = readUShort()

    return BfImageHeader(bfHeader, bfImageFlags, bfImageExtra, width, height)
}

/* extend ByteBuffer to allow storing image data structures */
fun ByteBuffer.writeBfImageFlags(flags: BfImageFlags) = writeUByte(flags.value)

fun ByteBuffer.readBfImageFlags(): BfImageFlags = BfImageFlags(readUByte())

fun ByteBuffer.writeBfImageExtra(extra: BfImageExtra) = writeUByte(extra.value)

fun ByteBuffer.readBfImageExtra(): BfImageExtra = BfImageExtra(readUByte())