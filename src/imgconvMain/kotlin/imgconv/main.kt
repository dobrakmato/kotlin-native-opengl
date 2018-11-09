package imgconv

import bfinfo.*
import kotlinx.cinterop.*
import lz4.LZ4F_compressionLevel_max
import lz4.LZ4_compress
import lz4.LZ4_compressBound
import lz4.LZ4_compress_HC
import platform.posix.exit
import platform.posix.fclose
import platform.posix.fopen
import platform.posix.fwrite
import stb.*
import kotlin.system.getTimeMillis

fun replaceExtensionWith(name: String, newExtension: String): String {
    return "${name.substring(0, name.lastIndexOf('.'))}.$newExtension"
}

fun determineRequiredChannels(switches: String): Int {
    return when {
        switches.contains('a') -> 4
        switches.contains('g') -> 2
        switches.contains('r') -> 1
        else -> 3
    }
}

class Timer {
    private var start: Long = 0
    fun begin() {
        start = getTimeMillis()
    }
    fun end(): Long = getTimeMillis() - start
}

fun main(args: Array<String>) {
    if (args.size < 2) {
        printHelp()
    }

    val timer = Timer()
    val switches = args[0]
    val from = args[1]
    val to = if (args.size > 2) args[2] else replaceExtensionWith(args[1], BF_EXTENSION)

    if (!switches.contains('v')) {
        stbi_set_flip_vertically_on_load(1) // usually we want this
    }
    val sourceChannels = determineRequiredChannels(switches)

    memScoped {
        val width = alloc<IntVar>()
        val height = alloc<IntVar>()
        val bpp = alloc<IntVar>()

        /* decode source image */
        timer.begin()
        var pixelData = readInputFile(from, width, height, bpp, sourceChannels)
        var pixelDataSize = (width.value * height.value * sourceChannels)
        val rawDataSize = pixelDataSize
        val ioLoadTime = timer.end()

        /* do DXT compression transparently to pixelData */
        timer.begin()
        if (switches.contains('d')) {
            val pair = doDXTCompression(this, switches, pixelDataSize, sourceChannels, height, width, pixelData)
            pixelData = pair.first
            pixelDataSize = pair.second
        }
        val dxtTime = timer.end()

        val uncompressedSize = pixelDataSize

        /* do LZ4 compression if requested */
        timer.begin()
        if (switches.contains('l')) {
            val (dest, compressedSize) = compressLZ4(this, switches, pixelDataSize, pixelData)
            pixelData = dest.reinterpret()
            pixelDataSize = compressedSize
        }
        val lz4Time = timer.end()

        /* create header */
        val header = BFImageHeader(
            BF_MAGIC,
            BF_VERSION,
            BF_FILE_IMAGE,
            BFImageHeaderFlags(
                combineFlags(
                    if (switches.contains('l')) BF_IMAGE_FLAG_LZ4 else 0,
                    if (switches.contains('h')) BF_IMAGE_FLAG_LZ4_HC else 0,
                    if (!switches.contains('v')) BF_IMAGE_FLAG_VERTICAL_FLIP else 0,
                    if (switches.contains('d')) BF_IMAGE_FLAG_DXT else 0
                )
            ),
            createImageExtraHeader(bpp.value, 0),
            width.value.toShort(),
            height.value.toShort(),
            uncompressedSize
        )
        val cHeader = createBFHeader(this, header)

        /* write to file */
        timer.begin()
        val f = fopen(to, "wb") // wb because windows would insert \n characters
        fwrite(cHeader, 1, BF_HEADER_IMAGE_SIZE.toULong(), f)
        fwrite(pixelData, 1, pixelDataSize.toULong(), f)
        fclose(f)
        val ioSaveTime = timer.end()

        /* report results */
        val times = "load=${ioLoadTime}ms dxt=${dxtTime}ms lz4=${lz4Time}ms save=${ioSaveTime}ms"
        println("imgconv ${pixelDataSize.toFloat() * 100 / rawDataSize}% ${header.width}x${header.height} ${header.extra.numberOfChannels()}bpp $times")
    }

}

private fun printHelp() {
    println("imgconv.exe -lhdqtfvrga16s INPUT_FILE [OUTPUT_FILE]")
    println("\nOptions:")
    println("    -l             : Enable LZ4 compression")
    println("    -h             : Use highest LZ4 compression level (slow)")
    println("    -d             : Use DXT compression")
    println("    -q             : Use DXT HQ compression (40% slower)")
    println("    -t             : Use DXT Dithering (do not use on normal textures)")
    println("    -f             : Encode as floating-point texture")
    println("    -v             : Do not flip texture vertically on load")
    println("    -16            : Treat file as 16bit texture")
    println("    -r             : Set channels to R")
    println("    -g             : Set channels to RG")
    println("    -a             : Set channels to RGBA")
    println("    -s             : Use gamma for RGB channels (SRGB)")
    println("\nAt least empty switch '-' is required for first argument.")
    exit(1)
}

private fun readInputFile(
    from: String,
    width: IntVar,
    height: IntVar,
    bpp: IntVar,
    sourceChannels: Int
): CPointer<stbi_ucVar>? {
    val pixelData = stbi_load(from, width.ptr, height.ptr, bpp.ptr, sourceChannels)

    if (pixelData == null) {
        println("imgconv Cannot load specified input file!")
        exit(2)
    }
    return pixelData
}

private fun compressLZ4(
    memScope: MemScope,
    switches: String,
    pixelDataSize: Int,
    pixelData: CPointer<stbi_ucVar>?
): Pair<CArrayPointer<ByteVar>, Int> {
    val maxCompressedSize = LZ4_compressBound(pixelDataSize)
    val dest = memScope.allocArray<ByteVar>(maxCompressedSize)

    val data: CPointer<ByteVar> = pixelData!!.reinterpret()
    val maxCompressionLevel = LZ4F_compressionLevel_max()
    val compressedSize =
        if (switches.contains('h')) LZ4_compress_HC(data, dest, pixelDataSize, maxCompressedSize, maxCompressionLevel)
        else LZ4_compress(data, dest, pixelDataSize)
    return Pair(dest, compressedSize)
}

private fun doDXTCompression(
    memScope: MemScope,
    switches: String,
    pixelDataSize: Int,
    sourceChannels: Int,
    height: IntVar,
    width: IntVar,
    pixelData: CPointer<stbi_ucVar>?
): Pair<CPointer<stbi_ucVar>?, Int> {
    var pixelDataSize1 = pixelDataSize
    var pixelData1 = pixelData

    val BLOCK_SIZE = 64

    val useAlpha = if (switches.contains('a')) 1 else 0
    val useDither = switches.contains('t')
    val useHQ = switches.contains('q')

    val flags = combineFlags(
        if (useDither) STB_DXT_DITHER else 0,
        if (useHQ) STB_DXT_HIGHQUAL else 0
    ).toInt()

    val dest = memScope.allocArray<UByteVar>(pixelDataSize1 / 6)
    val block = memScope.allocArray<UByteVar>(BLOCK_SIZE)

    fun extractBlock(
        from: CArrayPointer<UByteVar>, to: CArrayPointer<UByteVar>,
        blockX: Int, blockY: Int, width: Int
    ) {
        // [R0 G0 B0   R1 G1 B1   R2 G2 B2   R3 G3 B3]
        // [R4 G4 B4   R5 G5 B5   R6 G6 B6   R7 G7 B7]
        // ...

        val sourceRowLength = width * sourceChannels
        val sourceFirstByteInBlock = 4 * blockY * sourceRowLength + 4 * blockX * sourceChannels

        var dstIdx = 0
        repeat(4) { rowIdx ->
            val rowFirstByte = sourceFirstByteInBlock + (rowIdx * sourceRowLength)

            repeat(4) { colIdx ->
                to[dstIdx++] = from[rowFirstByte + sourceChannels * colIdx + 0] // r
                to[dstIdx++] = from[rowFirstByte + sourceChannels * colIdx + 1] // g
                to[dstIdx++] = from[rowFirstByte + sourceChannels * colIdx + 2] // b
                if (sourceChannels == 4) {
                    to[dstIdx++] = from[rowFirstByte + sourceChannels * colIdx + 3] // a
                } else {
                    to[dstIdx++] = 255.toUByte() // alpha is ignored
                }
            }
        }
    }

    // TODO: Do stuff multithreaded
    var destination = dest
    repeat(height.value / 4) { y ->
        repeat(width.value / 4) { x ->
            extractBlock(pixelData1!!.reinterpret(), block, x, y, width.value)
            stb_compress_dxt_block(destination, block, useAlpha, flags)
            destination = (destination.toLong() + 8).toCPointer()!!
        }
    }

    stbi_image_free(pixelData1)

    pixelData1 = dest
    pixelDataSize1 /= 6
    return Pair(pixelData1, pixelDataSize1)
}