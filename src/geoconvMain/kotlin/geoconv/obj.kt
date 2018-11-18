package geoconv

import kotlinx.cinterop.*
import math.Vector3f
import platform.posix.*

fun loadObjFile(from: String): GeometryData {
    return memScoped {
        /* read obj file */
        val contents = readObjFile(this, from)
        val data = GeometryData()

        val vertices = arrayListOf<Vector3f>()

        /* load obj data to memory */
        fun readVertex(tokens: List<String>) {
            vertices.add(Vector3f(tokens[0].toFloat(), tokens[1].toFloat(), tokens[2].toFloat()))
        }

        fun readFace(tokens: List<String>) {

        }

        fun readNormal(tokens: List<String>) {

        }

        fun readTexCoord(tokens: List<String>) {

        }

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
            }

        }

        /* process the data */
        data
    }
}

private fun readObjFile(memScope: MemScope, from: String): String {
    val f = fopen(from, "r") ?: throw Exception("Cannot open file $from because: ${posix_errno()}")

    fseek(f, 0, SEEK_END)
    val size = ftell(f)
    fseek(f, 0, SEEK_SET)
    val contents = memScope.allocArray<ByteVar>(size)
    fread(contents, 1, size.toULong(), f)
    fclose(f)
    return contents.toKString()
}