package bf

import io.ByteBuffer
import kotlinx.cinterop.*
import lz4.*

inline fun LZ4MaxCompressedSize(inputSize: Int) = LZ4_compressBound(inputSize)
inline fun LZ4MaxCompressionLevel() = LZ4F_compressionLevel_max()

private inline fun failDestinationBufferTooSmall(): Nothing =
    throw IllegalStateException("Compression failed! ByteBuffer was too small to complete the LZ4 compression! It's contents are now undefined - exiting.")

fun ByteBuffer.readLZ4Decompressed(compressedSize: Int, decompressedSize: Int): ByteBuffer {
    val buffer = ByteBuffer.create(decompressedSize.toLong())

    val src = (this.data.rawValue + this.pos.toLong()).toLong().toCPointer<ByteVar>()

    val decompressed = LZ4_decompress_safe(src, buffer.data.reinterpret(), compressedSize, decompressedSize)
    if (decompressed > 0 && decompressedSize != decompressed) throw IllegalStateException("decompressedSize value differs from actual decompressed bytes!")
    if (decompressed <= 0) throw IllegalStateException("Decompression failed!")

    pos += compressedSize

    return buffer
}

fun ByteBuffer.writeLZ4Compressed(
    input: CPointer<UByteVar>,
    inputSize: Int
): Int {
    val capacityLeft = this.size - this.pos
    val compressedSize =
        LZ4_compress_default(input.reinterpret(), this.pointerToPosition().reinterpret(), inputSize, capacityLeft.toInt())
    if (compressedSize == 0) failDestinationBufferTooSmall()

    pos += compressedSize

    return compressedSize
}

fun ByteBuffer.writeLZ4HCCompressed(
    input: CPointer<UByteVar>,
    inputSize: Int,
    compressionLevel: Int = LZ4MaxCompressionLevel()
): Int {
    val capacityLeft = this.size - this.pos
    val compressedSize =
        LZ4_compress_HC(input.reinterpret(), this.pointerToPosition().reinterpret(), inputSize, capacityLeft.toInt(), compressionLevel)
    if (compressedSize == 0) failDestinationBufferTooSmall()

    pos += compressedSize

    return compressedSize
}