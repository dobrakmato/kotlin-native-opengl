package bf

import io.ByteBuffer
import kotlin.math.max

/* image flags */
const val BF_IMAGE_FLAG_LZ4: UByte = 1u
const val BF_IMAGE_FLAG_LZ4_HC: UByte = 2u
const val BF_IMAGE_FLAG_DXT: UByte = 4u
const val BF_IMAGE_FLAG_FLOAT: UByte = 8u
const val BF_IMAGE_FLAG_16_BIT: UByte = 16u
const val BF_IMAGE_FLAG_VERTICAL_FLIP: UByte = 32u
const val BF_IMAGE_FLAG_GAMMA_SRGB: UByte = 64u
const val BF_IMAGE_FLAG_SKYBOX: UByte = 128u

inline class BfImageFlags(val value: UByte) {
    inline fun lz4() = (value and BF_IMAGE_FLAG_LZ4) == BF_IMAGE_FLAG_LZ4
    inline fun lz4hc() = (value and BF_IMAGE_FLAG_LZ4_HC) == BF_IMAGE_FLAG_LZ4_HC
    inline fun dxt() = (value and BF_IMAGE_FLAG_DXT) == BF_IMAGE_FLAG_DXT
    inline fun float() = (value and BF_IMAGE_FLAG_FLOAT) == BF_IMAGE_FLAG_FLOAT
    inline fun is16bit() = (value and BF_IMAGE_FLAG_16_BIT) == BF_IMAGE_FLAG_16_BIT
    inline fun verticallyFlipped() = (value and BF_IMAGE_FLAG_VERTICAL_FLIP) == BF_IMAGE_FLAG_VERTICAL_FLIP
    inline fun srgb() = (value and BF_IMAGE_FLAG_GAMMA_SRGB) == BF_IMAGE_FLAG_GAMMA_SRGB
    inline fun skybox() = (value and BF_IMAGE_FLAG_SKYBOX) == BF_IMAGE_FLAG_SKYBOX
}

inline class BfImageExtra(val value: UByte) {
    inline fun hasMipmaps() = (value and 8u) == 8u.toUByte()
    inline fun includedMipmaps() = max(1, (value.toInt() and 0b11110000) shr 4)
    inline fun numberOfChannels() = (value.toInt() and 0b00000111)
}

/* extend ByteBuffer to allow storing image data structures */
fun ByteBuffer.writeImageFlags(flags: BfImageFlags) = writeUByte(flags.value)

fun ByteBuffer.readImageFlags(): BfImageFlags = BfImageFlags(readUByte())