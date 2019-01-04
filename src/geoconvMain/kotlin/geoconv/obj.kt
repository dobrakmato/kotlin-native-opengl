package geoconv

import io.Path
import io.readUTF8TextFile
import math.normalized
import math.vec2
import math.vec3
import utils.IntArrayList

internal fun loadObjFile(from: Path): Geometry {
    /* read obj file */
    Timers.IO_LOAD.begin()
    val contents = readUTF8TextFile(from.path)
    Timers.IO_LOAD.end()

    val geo = Geometry()
    val meta = GeometryMetadata()

    /* temporary variables */
    val vertexIndices = IntArrayList(65536)
    val uvIndices = IntArrayList(65536)
    val normalIndices = IntArrayList(65536)

    /* load obj data to memory */
    fun readVertex(tokens: List<String>) {
        /* tokens[0] == 'v' */
        geo.vertices.add(vec3(tokens[1].toFloat(), tokens[2].toFloat(), tokens[3].toFloat()))
    }

    fun readTexCoord(tokens: List<String>) {
        /* tokens[0] == 'vt' */

        // note: there can be tokens[3] which is supposed to be optional W co-ordinate
        geo.texCoords.add(vec2(tokens[1].toFloat(), tokens[2].toFloat()))
    }

    fun readNormal(tokens: List<String>) {
        /* tokens[0] == 'vn' */

        // note: normals might not be unit vectors.
        geo.normals.add(vec3(tokens[1].toFloat(), tokens[2].toFloat(), tokens[3].toFloat()).normalized())
    }

    fun readFace(tokens: List<String>) {
        /* tokens[0] == 'f' */

        // the standard says that there can be any number > 3 polygons
        // defining one face, but in reality only triangles and quads
        // are commonly found in obj exported files, so for now we
        // support only these variants. full-fledged triangulation will
        // be implemented later.
        // check: https://en.wikipedia.org/wiki/Polygon_triangulation

        if (tokens.size > 5) {
            throw UnsupportedOperationException("Cannot triangulate face with more than 4 vertices!")
        }

        if (tokens.size < 4) {
            throw UnsupportedOperationException("Face must have at lest 3 vertices!")
        }

        // note: indices in OBJ file are starting at number 1. negative
        // indices are however supported too. if an index is negative then
        // it relatively refers to the end of the vertex list, -1 referring
        // to the last element.

        if (tokens.size == 5) {

        } else {

        }

        fun emitIndex(tokens: List<String>) {
            val vertexIdx = tokens[0]
            val texCoordIdx = tokens[1]
            val normalIdx = tokens[2]


        }

        emitIndex(tokens[0].split('/'))
        emitIndex(tokens[1].split('/'))
        emitIndex(tokens[2].split('/'))
    }

    Timers.DECODE_OBJ.begin()
    /* step 1: read obj file to memory */
    for (line in contents.lines()) {
        if (line.trim().startsWith('#')) { // ignore comments
            continue
        }

        val tokens = line.trim().split(' ')
        val first = tokens[0]

        when (first) {
            "v" -> readVertex(tokens)
            "vt" -> readTexCoord(tokens)
            "vn" -> readNormal(tokens)
            "f" -> readFace(tokens)
            "s" -> Unit // smooth shading: somehow switch normal computation?
            "usemtl" -> Unit // show warning about non-imported material
            "o" -> Unit // new object
            "g" -> Unit // new group
            "#" -> Unit // comment
        }

    }

    /* step 2: resolve obj indices to opengl indices */

    Timers.DECODE_OBJ.end()

    /* process the data */
    return geo
}