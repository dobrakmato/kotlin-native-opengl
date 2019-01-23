package geoconv

import io.Path
import io.withExtension
import math.Vector2f
import math.Vector3f
import platform.posix.exit
import utils.IntArrayList
import utils.Logger
import utils.Options
import utils.Timer

internal class Geometry {
    val vertices: MutableList<Vector3f> = arrayListOf()
    val texCoords: MutableList<Vector2f> = arrayListOf()
    val normals: MutableList<Vector3f> = arrayListOf()
    val tangents: MutableList<Vector3f> = arrayListOf()
    val bitangents: MutableList<Vector3f> = arrayListOf()
    val indices: IntArrayList = IntArrayList(65536)
    val geometryMetadata = GeometryMetadata()
}

/* class for extra data related to geometry but not being a real part of geometry data */
internal class GeometryMetadata {
    var smoothShading: Boolean = true
    var name: String = "geometry"
    var materials: MutableList<String> = arrayListOf()
}


/*
geoconv
-------

- generate normals
- recalculate normals (specify alpha)
- flat normals opt
- generate tangents & bitangents
- generate lightmap uvs
- optimize topology
- generate LODs
-

*/

// OBJ file -> (obj reader) -> OBJModel -> (obj resolver) -> Geometry -> (generate & recalculate normals) -> (generate tangents & bitangents) -> (lightmap) -> Geometry -> BGF file

@ThreadLocal
object Timers {
    val IO_LOAD = Timer()
    val DECODE_OBJ = Timer()
    val DECODE_FBX = Timer()
    val CALCULATE_NORMALS = Timer()
    val CALCULATE_TANGENTS = Timer()
    val CALCULATE_LODS = Timer()
    val LZ4 = Timer()
    val IO_SAVE = Timer()
}

fun main(args: Array<String>) {
    val appName = "geoconv.exe"
    val log = Logger("geoconv")
    val options = Options()
        .option('l', "use-lz4", "Enable LZ4 compression")
        .option('h', "use-lz4-high", "Use highest LZ4 compression level (slow)")
        .option('i', "not-indices", "Do not use indices for generated mesh")
        .option('n', "not-normalize-normals", "Do not use normalize normals")
        .requiredValue("input-file", "File to read as input file")
        .optionalValue("output-file", "Output file to write data to")
        .optionalValue("lod-threads", "The number of threads to use when generating LODs")
        .optionalValue("lod-levels", "The number of LOD levels to generate (default 7)")

    val opts = options.parse(args)
    if (opts.shouldDisplayHelp()) {
        opts.displayHelp(appName)
        exit(1)
    }

    val inputFile = Path(opts.getValue("input-file"))
    val outputFile = Path(opts.getOptionalValue("output-file", inputFile.withExtension("bf").path))


    /* load source file */
    val geo = when (inputFile.extension) {
        "obj" -> loadObjFile(inputFile)
        "fbx" -> loadFbxFile(inputFile)
        else -> throw RuntimeException("The format ${inputFile.extension}is not yet supported!")
    }

    log.info("imported vertices ${geo.vertices.size}")
    log.info("imported normals ${geo.normals.size}")
    log.info("imported texCoords ${geo.texCoords.size}")
    log.info("imported indices ${geo.indices.size} (faces ${geo.indices.size / 3})")

    log.debug("indices: ${geo.indices.joinToString(" ")}")
    log.debug("vertices: ${geo.vertices.joinToString("\n ")}")

    /* generate / recalculate normals */

    /* generate tangents and bitangents */

    /* generate lods */

    /* save bf file */

}