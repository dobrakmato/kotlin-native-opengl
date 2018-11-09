package geoconv

import bfinfo.*
import platform.posix.exit
import kotlin.system.getTimeMillis

fun replaceExtensionWith(name: String, newExtension: String): String {
    return "${name.substring(0, name.lastIndexOf('.'))}.$newExtension"
}

fun extractExtension(from: String): String {
    return from.substring(from.lastIndexOf('.'))
}

class Timer {
    private var start: Long = 0
    fun begin() {
        start = getTimeMillis()
    }

    fun end(): Long = getTimeMillis() - start
}

data class GeometryHeader(
    val magic: Int = BF_MAGIC,
    val version: Byte = BF_VERSION,
    val fileType: Byte = BF_FILE_GEOMETRY,

    val flags: Byte, // 1 = normals, 2 = tangents, 4 = texcoords, 8 = indices
    val extra: Byte, //

    val uncompressedSize: Int
)

class GeometryData {
    val vertices: List<Float> = arrayListOf()
    val texCoords: List<Float> = arrayListOf()
    val normals: List<Float> = arrayListOf()
    val tangents: List<Float> = arrayListOf()
    val bitangents: List<Float> = arrayListOf()
    val indices: List<Int> = arrayListOf()
}

fun main(args: Array<String>) {
    if (args.size < 2) {
        printHelp()
    }

    val timer = Timer()
    val switches = args[0]
    val from = args[1]
    val to = if (args.size > 2) args[2] else replaceExtensionWith(args[1], BF_EXTENSION)
    val extension = extractExtension(args[1])

    /* load source file */
    val geometryData = when (extension) {
        "obj" -> loadObjFile(from)
        "fbx" -> loadFbxFile(from)
        else -> throw RuntimeException("The format $extension is not yet supported!")
    }

    /* save bf file */

}

private fun printHelp() {
    println("geoconv.exe - INPUT_FILE [OUTPUT_FILE]")
    println("\nOptions:")
    println("    -l             : Enable LZ4 compression")
    println("    -h             : Use highest LZ4 compression level (slow)")
    println("    -i             : Use indices (default)")
    println("    -n             : Normalize normal vectors (default)")
    println("\nAt least empty switch '-' is required for first argument.")
    exit(1)
}
