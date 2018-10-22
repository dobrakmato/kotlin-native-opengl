package imgconv

import kotlinx.cinterop.*
import platform.posix.exit
import platform.posix.fclose
import platform.posix.fopen
import platform.posix.fwrite
import stb.freeImage
import stb.loadImage
import stb.setFlipVerticallyOnLoad
import kotlin.system.getTimeMillis


fun main(args: Array<String>) {
    if (args.isEmpty()) {
        println("imgconv.exe INPUT_FILE [OUTPUT_FILE]")
        exit(1)
    }

    val from = args[0]
    val to = if (args.size > 1) args[1] else args[0].replace("png", "raw")

    val start = getTimeMillis()
    memScoped {
        val width = alloc<IntVar>()
        val height = alloc<IntVar>()
        val bpp = alloc<IntVar>()
        setFlipVerticallyOnLoad(1)
        val pixelData = loadImage(from, width.ptr, height.ptr, bpp.ptr, 3)
        val saveStart = getTimeMillis()

        val f = fopen(to, "wb")
        val size = (width.value * height.value * 3).toULong()
        fwrite(pixelData, 1, size, f)
        fclose(f)
        freeImage(pixelData)
        println("imgconv " + width.value + "x" + height.value + " " + bpp.value + "bpp " + size + "bytes " + (saveStart - start) + "ms " + (getTimeMillis() - saveStart) + "ms " + (getTimeMillis() - start) + "ms total")
    }

}