package bfinfo

import kotlinx.cinterop.*
import platform.posix.*

val COLOR = "YES"

const val ANSI_WITE = "\u001B[0;37m"
const val ANSI_GREEN_INTENSIVE = "\u001B[1;92m"
const val ANSI_GREEN_REGULAR = "\u001B[0;32m"
const val ANSI_RESET = "\u001B[0m"

fun log(msg: String, value: String? = null) {
    if (COLOR == "NO") {
        if (value == null) {
            println("bfinfo $msg")
        } else {
            println("bfinfo $msg\t$value")
        }
    } else {
        if (value == null) {
            println("${ANSI_WITE}bfinfo $ANSI_GREEN_REGULAR$msg$ANSI_RESET")
        } else {
            println("${ANSI_WITE}bfinfo $ANSI_GREEN_REGULAR$msg $ANSI_GREEN_INTENSIVE$value$ANSI_RESET")
        }
    }
}

fun hasBFMagic(ptr: CArrayPointer<UByteVar>): Boolean {
    val a = ptr[0]
    val b = ptr[0 + 1]
    val c = ptr[0 + 2]
    val d = ptr[0 + 3]

    return (a.toInt() or
            (b.toInt() shl 8) or
            (c.toInt() shl 16) or
            (d.toInt() shl 24)) == BF_MAGIC
}


fun main(args: Array<String>) {
    if (args.isEmpty()) {
        println("bfinfo.exe INPUT_FILE")
        exit(1)
    }

    val path = args[0]
    val f = fopen(path, "rb") ?: throw Exception("Cannot open file $path because: ${posix_errno()}")
    fseek(f, 0, SEEK_END)
    val size = ftell(f)
    fseek(f, 0, SEEK_SET)
    val data = nativeHeap.allocArray<UByteVar>(size)
    fread(data, 1, size.toULong(), f)
    fclose(f)

    if (!hasBFMagic(data)) {
        log("not a BF file")
        exit(2)
    }

    val version = data[4]
    val fileType = data[5]

    log("type", "image file")
    log("size", "$size bytes")
    log("version", "$version")

    when (fileType.toByte()) {
        BF_FILE_IMAGE -> printImageInfo(data, version, size)
        BF_FILE_GEOMETRY -> printGeometryInfo(data, version, size)
    }

    nativeHeap.free(data)
}

fun printImageInfo(data: CArrayPointer<UByteVar>, version: UByte, size: Int) {
    val header = readBFHeader(data.reinterpret())

    log("flags lz4", "${header.flags.lz4()}")
    log("flags lz4hc", "${header.flags.lz4hc()}")
    log("flags vflip", "${header.flags.verticallyFlipped()}")
    log("flags float", "${header.flags.float()}")
    log("flags 16bit", "${header.flags.is16bit()}")
    log("flags srgb", "${header.flags.srgb()}")
    log("flags dxt", "${header.flags.dxt()}")
    log("flags skybox", "${header.flags.skybox()}")
    log("has mipmaps", "${header.extra.hasMipmaps()}")
    log("channels", "${header.extra.numberOfChannels()}")
    log("mipmaps", "${header.extra.includedMipmaps()}")
    log("width", "${header.width}")
    log("height", "${header.height}")
    log("uncompressed size", "${header.uncompressedSize}")
}

fun printGeometryInfo(data: CArrayPointer<UByteVar>, version: UByte, size: Int) {

}
