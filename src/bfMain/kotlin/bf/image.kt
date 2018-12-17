package bf

import io.ByteBuffer
import kotlin.math.max

/* image flags */
const val BF_IMAGE_FLAG_LZ4 = 1
const val BF_IMAGE_FLAG_LZ4_HC = 2
const val BF_IMAGE_FLAG_DXT = 4
const val BF_IMAGE_FLAG_FLOAT = 8
const val BF_IMAGE_FLAG_16_BIT = 16
const val BF_IMAGE_FLAG_VERTICAL_FLIP = 32
const val BF_IMAGE_FLAG_GAMMA_SRGB = 64
const val BF_IMAGE_FLAG_SKYBOX = 128

inline class BfImageFlags(val value: UByte) {
    inline fun lz4() = (value.toInt() and BF_IMAGE_FLAG_LZ4) == BF_IMAGE_FLAG_LZ4
    inline fun lz4hc() = (value.toInt() and BF_IMAGE_FLAG_LZ4_HC) == BF_IMAGE_FLAG_LZ4_HC
    inline fun dxt() = (value.toInt() and BF_IMAGE_FLAG_DXT) == BF_IMAGE_FLAG_DXT
    inline fun float() = (value.toInt() and BF_IMAGE_FLAG_FLOAT) == BF_IMAGE_FLAG_FLOAT
    inline fun is16bit() = (value.toInt() and BF_IMAGE_FLAG_16_BIT) == BF_IMAGE_FLAG_16_BIT
    inline fun verticallyFlipped() = (value.toInt() and BF_IMAGE_FLAG_VERTICAL_FLIP) == BF_IMAGE_FLAG_VERTICAL_FLIP
    inline fun srgb() = (value.toInt() and BF_IMAGE_FLAG_GAMMA_SRGB) == BF_IMAGE_FLAG_GAMMA_SRGB
    inline fun skybox() = (value.toInt() and BF_IMAGE_FLAG_SKYBOX) == BF_IMAGE_FLAG_SKYBOX
}

inline class BfImageExtra(val value: Byte) {
    fun hasMipmaps() = (value.toInt() and 8) == 8
    fun includedMipmaps() = max(1, (value.toInt() and 0b11110000) shr 4)
    fun numberOfChannels() = (value.toInt() and 0b00000111)
}

/* extend ByteBuffer to allow storing image data structures */
fun ByteBuffer.writeImageFlags(flags: BfImageFlags) = writeUByte(flags.value)

fun ByteBuffer.readImageFlags(): BfImageFlags = BfImageFlags(readUByte())