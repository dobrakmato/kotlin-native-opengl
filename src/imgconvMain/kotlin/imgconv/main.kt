package imgconv

import kotlinx.cinterop.*
import lz4.*
import bfinfo.*
import platform.posix.exit
import platform.posix.fclose
import platform.posix.fopen
import platform.posix.fwrite
import stb.stbi_image_free
import stb.stbi_load
import stb.stbi_set_flip_vertically_on_load
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

fun main(args: Array<String>) {
    if (args.size < 2) {
        println("imgconv.exe -lhdfvrga16s INPUT_FILE [OUTPUT_FILE]")
        println("    -l             : Enable LZ4 compression")
        println("    -h             : Use highest LZ4 compression level (slow)")
        println("    -d             : Use DXT compression")
        println("    -f             : Encode as floating-point texture")
        println("    -v             : Do not flip texture vertically on load")
        println("    -16            : Treat file as 16bit texture")
        println("    -r             : Set channels to R")
        println("    -g             : Set channels to RG")
        println("    -a             : Set channels to RGBA")
        println("    -s             : Use gamma for RGB channels (SRGB)")
        println("At least empty switch '-' is required for first argument.")
        exit(1)
    }

    val switches = args[0]
    val from = args[1]
    val to = if (args.size > 2) args[2] else replaceExtensionWith(args[1], BF_EXTENSION)

    if (!switches.contains('v')) {
        stbi_set_flip_vertically_on_load(1) // usually we want this
    }
    val channels = determineRequiredChannels(switches)

    memScoped {
        val start = getTimeMillis()
        val width = alloc<IntVar>()
        val height = alloc<IntVar>()
        val bpp = alloc<IntVar>()

        val pixelData = stbi_load(from, width.ptr, height.ptr, bpp.ptr, channels)

        if (pixelData == null) {
            println("imgconv Cannot load specified input file!")
            exit(2)
        }

        val pixelDataSize = (width.value * height.value * channels)

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
                            if (!switches.contains('v')) BF_IMAGE_FLAG_VERTICAL_FLIP else 0
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
            stbi_image_free(pixelData)
            println("imgconv lz4" + (if (hc) "hc " else " ") + (100 * compressedSize.toFloat() / pixelDataSize) + "% " + width.value + "x" + height.value + " " + bpp.value + "bpp " + (compressStart - start) + "ms " + (saveStart - compressStart) + "ms " + (getTimeMillis() - saveStart) + "ms ")
        } else {
            val saveStart = getTimeMillis()
            val f = fopen(to, "wb") // wb because windows would insert \n characters
            fwrite(pixelData, 1, pixelDataSize.toULong(), f)
            fclose(f)
            stbi_image_free(pixelData)
            println("imgconv raw " + width.value + "x" + height.value + " " + bpp.value + "bpp " + (saveStart - start) + "ms " + (getTimeMillis() - saveStart) + "ms ")
        }
    }

}