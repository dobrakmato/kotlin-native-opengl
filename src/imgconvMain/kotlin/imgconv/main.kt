package imgconv

import kotlinx.cinterop.*
import lz4.*
import bfinfo.*
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

interface Step {
    val name: String
    operator fun invoke(input: Pair<BFImageHeader, CArrayPointer<UByteVar>>?): Pair<BFImageHeader, CArrayPointer<UByteVar>>
}

interface S<I, O> {
    val name: String
    operator fun invoke(input: I): O
}

class Load : S<String, Pair<BFImageHeader, CPointer<UByteVar>>> {
    override val name = "load"
    override fun invoke(input: String): Pair<BFImageHeader, CPointer<UByteVar>> {
        val width = nativeHeap.alloc<IntVar>()
        val height = nativeHeap.alloc<IntVar>()
        val bpp = nativeHeap.alloc<IntVar>()
        val desiredChannels = 3

        val pixelData = stbi_load(input, width.ptr, height.ptr, bpp.ptr, desiredChannels)
        val pixelDataSize = (width.value * height.value * desiredChannels)

        val header = BFImageHeader(
            BF_MAGIC,
            BF_VERSION,
            BF_FILE_IMAGE,
            BFImageHeaderFlags(0),
            BFImageHeaderExtra(0),
            width.value.toShort(),
            height.value.toShort(),
            pixelDataSize
        )

        return Pair(header, pixelData!!)
    }
}

class LoadImageStep : Step {
    override val name = "load"
    override fun invoke(input: Pair<BFImageHeader, CArrayPointer<UByteVar>>?): Pair<BFImageHeader, CArrayPointer<UByteVar>> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

}

typealias Pipeline = List<Step>

fun runPipeline(pipeline: Pipeline) {
    val times = hashMapOf<String, Long>()
    var result: Pair<BFImageHeader, CArrayPointer<UByteVar>>? = null
    for (step in pipeline) {
        val start = getTimeMillis()
        result = step(result)
        val total = getTimeMillis() - start
        times[step.name] = total
    }
}

fun main(args: Array<String>) {
    if (args.size < 2) {
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

    val switches = args[0]
    val from = args[1]
    val to = if (args.size > 2) args[2] else replaceExtensionWith(args[1], BF_EXTENSION)

    if (!switches.contains('v')) {
        stbi_set_flip_vertically_on_load(1) // usually we want this
    }
    val sourceChannels = determineRequiredChannels(switches)

    memScoped {
        val start = getTimeMillis()
        val width = alloc<IntVar>()
        val height = alloc<IntVar>()
        val bpp = alloc<IntVar>()

        /* decode source image */
        var pixelData = stbi_load(from, width.ptr, height.ptr, bpp.ptr, sourceChannels)

        if (pixelData == null) {
            println("imgconv Cannot load specified input file!")
            exit(2)
        }

        var pixelDataSize = (width.value * height.value * sourceChannels)

        /* do DXT compression transparently to pixelData */
        if (switches.contains('d')) {
            val BLOCK_SIZE = 64
            val useAlpha = if (switches.contains('a')) 1 else 0
            val useDither = switches.contains('t')
            val useHQ = switches.contains('q')

            val flags = combineFlags(
                if (useDither) STB_DXT_DITHER else 0,
                if (useHQ) STB_DXT_HIGHQUAL else 0
            ).toInt()

            val dest = allocArray<UByteVar>(pixelDataSize / 6)
            val block = allocArray<UByteVar>(BLOCK_SIZE)

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

                        //print("(${rowFirstByte + sourceChannels * colIdx + 0}/${to[dstIdx - 4]} ${rowFirstByte + sourceChannels * colIdx + 1}/${to[dstIdx - 3]} ${rowFirstByte + sourceChannels * colIdx + 2}/${to[dstIdx - 2]} ${to[dstIdx - 1]}) ")
                    }
                    //println()
                }
            }

            // Do stuff multithreaded
            var destination = dest
            repeat(height.value / 4) { y ->
                repeat(width.value / 4) { x ->
                    extractBlock(pixelData!!.reinterpret(), block, x, y, width.value)
                    stb_compress_dxt_block(destination, block, useAlpha, flags)
                    destination = (destination.toLong() + 8).toCPointer()!!
                }
            }

            stbi_image_free(pixelData)

            pixelData = dest
            pixelDataSize /= 6
        }

        /* write target compressed or uncompressed */
        if (switches.contains('l')) {
            val compressStart = getTimeMillis()
            val hc = switches.contains('h')
            val maxCompressedSize = LZ4_compressBound(pixelDataSize)
            val dest = allocArray<ByteVar>(maxCompressedSize)

            val data: CPointer<ByteVar> = pixelData!!.reinterpret()
            val maxCompressionLevel = LZ4F_compressionLevel_max()
            val compressedSize =
                if (hc) LZ4_compress_HC(data, dest, pixelDataSize, maxCompressedSize, maxCompressionLevel)
                else LZ4_compress(data, dest, pixelDataSize)

            val saveStart = getTimeMillis()
            val f = fopen(to, "wb") // wb because windows would insert \n characters
            val header = createBFHeader(
                this, BFImageHeader(
                    BF_MAGIC,
                    BF_VERSION,
                    BF_FILE_IMAGE,
                    BFImageHeaderFlags(
                        combineFlags(
                            BF_IMAGE_FLAG_LZ4,
                            if (hc) BF_IMAGE_FLAG_LZ4_HC else 0,
                            if (!switches.contains('v')) BF_IMAGE_FLAG_VERTICAL_FLIP else 0,
                            if (switches.contains('d')) BF_IMAGE_FLAG_DXT else 0
                        )
                    ),
                    createImageExtraHeader(bpp.value, 0),
                    width.value.toShort(),
                    height.value.toShort(),
                    pixelDataSize
                )
            )
            fwrite(header, 1, BF_HEADER_IMAGE_SIZE.toULong(), f)
            fwrite(dest, 1, compressedSize.toULong(), f)
            fclose(f)
            if (!switches.contains('d')) {
                stbi_image_free(pixelData)
            }
            println("imgconv lz4" + (if (hc) "hc " else " ") + (if (switches.contains('d')) "dxt " else " ") + (100 * compressedSize.toFloat() / pixelDataSize) + "% " + width.value + "x" + height.value + " " + bpp.value + "bpp " + (compressStart - start) + "ms " + (saveStart - compressStart) + "ms " + (getTimeMillis() - saveStart) + "ms ")
        } else {
            val saveStart = getTimeMillis()
            val header = createBFHeader(
                this, BFImageHeader(
                    BF_MAGIC,
                    BF_VERSION,
                    BF_FILE_IMAGE,
                    BFImageHeaderFlags(
                        combineFlags(
                            if (!switches.contains('v')) BF_IMAGE_FLAG_VERTICAL_FLIP else 0,
                            if (switches.contains('d')) BF_IMAGE_FLAG_DXT else 0
                        )
                    ),
                    createImageExtraHeader(bpp.value, 0),
                    width.value.toShort(),
                    height.value.toShort(),
                    pixelDataSize
                )
            )
            val f = fopen(to, "wb") // wb because windows would insert \n characters
            fwrite(header, 1, BF_HEADER_IMAGE_SIZE.toULong(), f)
            fwrite(pixelData, 1, pixelDataSize.toULong(), f)
            fclose(f)
            if (!switches.contains('d')) {
                stbi_image_free(pixelData)
            }
            println("imgconv raw " + (if (switches.contains('d')) "dxt " else " ") + width.value + "x" + height.value + " " + bpp.value + "bpp " + (saveStart - start) + "ms " + (getTimeMillis() - saveStart) + "ms ")
        }
    }

}