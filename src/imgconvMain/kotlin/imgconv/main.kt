package imgconv

import kotlinx.cinterop.*
import lz4.*
import platform.posix.exit
import platform.posix.fclose
import platform.posix.fopen
import platform.posix.fwrite
import stb.stbi_image_free
import stb.stbi_load
import stb.stbi_set_flip_vertically_on_load
import kotlin.system.getTimeMillis

class BFHeader {
    val version: Byte = 1
    val fileType: Byte = 1 // 1 = image, 2 = geometry, 3 = audio, 4 = material, 5 = vfs, 6 = compiled shader
    val flags: Byte =
        0x1 // 1 = lz4, 2 = hc, 4 = dxt, 8 = floating point, 16 = 16 bit texture, 32 = vertically flipped, 64 = gamma (SRGB), 128 = skybox
    val extra: Byte = 3 // [_ _ _ _] mipmap levels [_] inline mipmaps [_ _ _] channels

    val compressionSettings: Byte = 0 // 1 = lz4, 2 = lizard
    val uncompressedSize: Int = 0

    val width: UShort = 0u
    val height: UShort = 0u

    val data: Array<Char> = arrayOf()
}

const val BF_EXTENSION = "bf"

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
        println("imgconv.exe -lhdfvrga16 INPUT_FILE [OUTPUT_FILE]")
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
                if (hc) LZ4_compressHC2(data.toKString(), dest, pixelDataSize, maxCompressionLevel) else LZ4_compress(
                    data.toKString(),
                    dest,
                    pixelDataSize
                )

            val saveStart = getTimeMillis()
            val f = fopen(to, "wb") // wb because windows would insert \n characters

            println("Raw size: $pixelDataSize")
            println("Raw toKString(): ${data.toKString().length}")
            println("Dest size: $maxCompressedSize")
            println("Compressed size: $compressedSize")

            val written = fwrite(dest, 1, compressedSize.toULong(), f)
            println("Written $written")

            fclose(f)
            stbi_image_free(pixelData)

            println("imgconv lz4" + (if (hc) "hc " else " ") + (100*compressedSize.toFloat()/pixelDataSize) + "% " + width.value + "x" + height.value + " " + bpp.value + "bpp " + (compressStart - start) + "ms " + (saveStart - compressStart) + "ms " + (getTimeMillis() - saveStart) + "ms ")
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