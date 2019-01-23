package geoconv

import io.Path
import io.readUTF8TextFile
import math.*
import utils.IntArrayList
import utils.Logger

internal fun loadObjFile(from: Path): Geometry {

    val log = Logger("ObjImporter")

    /* read obj file */
    Timers.IO_LOAD.begin()
    val contents = readUTF8TextFile(from.path)
    Timers.IO_LOAD.end()

    val geo = Geometry()

    /* Class for finding unique triples of vertex values */
    data class UniqueVertex(val position: Vector3f, val texCoord: Vector2f?, val normal: Vector3f?) {
        override fun equals(other: Any?): Boolean {
            if (other == null) return false
            if (other is UniqueVertex) {
                return other.normal == normal && other.texCoord == texCoord && other.position == position
            }
            return false
        }

        override fun hashCode(): Int {
            val prime = 31
            var result = 1
            result = prime * result + position.hashCode()
            result = prime * result + (texCoord?.hashCode() ?: 0)
            result = prime * result + (normal?.hashCode() ?: 0)
            return result
        }
    }

    var vertexIdx = 0
    val uniqueVertices = hashMapOf<UniqueVertex, Int>()
    val uniqueVerticesReversed = hashMapOf<Int, UniqueVertex>()

    /* load obj data to memory */
    fun readVertex(tokens: List<String>) {
        if (tokens.size < 3) throw RuntimeException("To few floats (${tokens.size}) in 'v' line")
        geo.vertices.add(vec3(tokens[0].toFloat(), tokens[1].toFloat(), tokens[2].toFloat()))
    }

    fun readTexCoord(tokens: List<String>) {
        if (tokens.size < 2) throw RuntimeException("To few floats in 'vt' line")
        // note: there can be tokens[2] which is supposed to be optional W co-ordinate
        geo.texCoords.add(vec2(tokens[0].toFloat(), tokens[1].toFloat()))
    }

    fun readNormal(tokens: List<String>) {
        if (tokens.size < 3) throw RuntimeException("To few floats in 'vn' line")
        // note: normals might not be unit vectors.
        geo.normals.add(vec3(tokens[0].toFloat(), tokens[1].toFloat(), tokens[2].toFloat()).normalized())
    }

    fun readFace(vertices: List<String>) {
        // the standard says that there can be any number > 3 polygons
        // defining one face, but in reality only triangles and quads
        // are commonly found in obj exported files, so for now we
        // support only these variants. full-fledged triangulation will
        // be implemented later.
        // check: https://en.wikipedia.org/wiki/Polygon_triangulation

        if (vertices.size > 4) {
            throw UnsupportedOperationException("Cannot triangulate face with more than 4 vertices!")
        }

        if (vertices.size < 3) {
            throw UnsupportedOperationException("Face must have at lest 3 vertices!")
        }

        /*
         * Examples of valid "face-lines":
         *
         * f 114//4 14//4 15//4 115//4
         * f 22//14 26//14 40//14 36//14
         * f -2/-2/-2 -5/-5/-5 -4/-4/-4 -1/-1/-1
         * f -15/-18/-18 -18/-17/-17 -17/-16/-16 -14/-15/-15
         * f 85/339 81/340 86/341 90/34
         * f 30/119 26/120 31/121
         */

        // note: indices in OBJ file are starting at number 1. negative
        // indices are however supported too. if an index is negative then
        // it relatively refers to the end of the vertex list, -1 referring
        // to the last element.

        fun emitIndex(vIndex: Int, vtIndex: Int?, vnIndex: Int?) {

            /* resolves obj index to normal c-array index */
            fun resolveIndex(objIndex: Int?, collection: Collection<Any>): Int? {
                val collectionSize = collection.size

                if (objIndex == null) return null
                if (objIndex > 0) return objIndex - 1 // obj indices are one-based
                if (objIndex < 0) return collectionSize + objIndex // objIdex is negative
                throw RuntimeException("Index in OBJ file cannot be equal to zero!")
            }

            /* resolve negative indices */
            val absVerticesIdx = resolveIndex(vIndex, geo.vertices)!! // position must be always present
            val absTexCoordIdx = resolveIndex(vtIndex, geo.texCoords)
            val absNormalIdx = resolveIndex(vnIndex, geo.normals)

            val vert = UniqueVertex(
                geo.vertices[absVerticesIdx],
                if (absTexCoordIdx == null) null else geo.texCoords[absTexCoordIdx],
                if (absNormalIdx == null) null else geo.normals[absNormalIdx]
            )

            if (vert !in uniqueVertices) {
                uniqueVertices[vert] = vertexIdx
                uniqueVerticesReversed[vertexIdx] = vert
                vertexIdx++
            }
            geo.indices.add(uniqueVertices[vert]!!)
        }

        fun processVertex(tokens: List<String>) {
            /* possible inputs: 114(v)//4(vn), -2(v)/-2(vt)/-2(vn), 31(v)/121(vt) */

            fun safeInt(list: List<String>, index: Int): Int? {
                val possibleNull = list.getOrNull(index) ?: return null
                if (possibleNull.isEmpty()) return null
                return possibleNull.toInt(10)
            }

            val vIndex = safeInt(tokens, 0)
            val vtIndex = safeInt(tokens, 1)
            val vnIndex = safeInt(tokens, 2)

            emitIndex(vIndex!!, vtIndex, vnIndex)
        }

        when {
            /* quad: create two triangles */
            vertices.size == 4 -> {
                processVertex(vertices[0].split('/'))
                processVertex(vertices[1].split('/'))
                processVertex(vertices[2].split('/'))

                processVertex(vertices[0].split('/'))
                processVertex(vertices[2].split('/'))
                processVertex(vertices[3].split('/'))
            }
            /* triangle: just emit unique vertices */
            vertices.size == 3 -> {
                processVertex(vertices[0].split('/'))
                processVertex(vertices[1].split('/'))
                processVertex(vertices[2].split('/'))
            }
            /* n-polygon: triangulate */
            else -> {
                // TODO: triangulate
                log.error("Mesh contains n-polygon which is current not supported!")
                log.error("Face: " + vertices.joinToString(" "))
            }
        }
    }

    Timers.DECODE_OBJ.begin()
    /* step 1: read obj file to memory */
    for (line in contents.lines()) {
        if (line.trim().startsWith('#')) { // ignore comments
            continue
        }

        // unify whitespace
        val cleaned = line.replace("\\s+".toRegex(), " ").trim()

        val tokens = cleaned.trim().split(' ')
        val first = tokens[0]

        when (first) {
            "v" -> readVertex(tokens.subList(1, tokens.size))
            "vt" -> readTexCoord(tokens.subList(1, tokens.size))
            "vn" -> readNormal(tokens.subList(1, tokens.size))
            "f" -> readFace(tokens.subList(1, tokens.size))
            "s" -> Unit // smooth shading: somehow switch normal computation?
            "usemtl" -> log.warn("Material ${tokens.getOrNull(1) ?: "#unnamed#"} was not imported!")
            "o" -> geo.geometryMetadata.name = tokens.getOrNull(1) ?: "Geometry" // new object
            "g" -> Unit // new group
            "#" -> Unit // comment
        }

    }

    /* step 2: resolve obj indices to opengl indices */

    geo.vertices.clear()
    geo.texCoords.clear()
    geo.normals.clear()

    for (idx in 0 until vertexIdx) {
        val unique = uniqueVerticesReversed[idx]!!

        geo.vertices.add(unique.position)
        unique.texCoord?.let { geo.texCoords.add(it) }
        unique.normal?.let { geo.normals.add(it) }
    }

    uniqueVertices.clear()
    uniqueVerticesReversed.clear()

    Timers.DECODE_OBJ.end()

    /* process the data */
    return geo
}