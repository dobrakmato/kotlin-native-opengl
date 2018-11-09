package geoconv

import kotlinx.cinterop.*
import platform.posix.*

fun loadObjFile(from: String) {
    memScoped {
        val (contents, size) = readObjFile(this, from)
        val data = GeometryData()

        val parser = ObjParser(contents, size, data)
        parser.parse()
    }
}

class ObjParser(private val contents: String, val size: Int, val data: GeometryData) {
    private var pointer = 0

    private fun current(): Char = contents[pointer]
    private fun next(): Char = contents[pointer++]

    private fun readUntilNewline() {
        while (current() != '\n') next()
    }

    fun parse() {
        while (pointer < size) {
            when (next()) {
                '#' -> readUntilNewline()
                'v' -> when (next()) {
                    ' ' -> readVertex()
                    'n' -> readNormal()
                    't' -> readTexCoord()
                }
                'f' -> readVertexNormal()
            }
        }
    }

    private fun readTexCoord() {

    }

    private fun readNormal() {

    }

    private fun readVertex() {

    }
}

private fun readObjFile(memScope: MemScope, from: String): Pair<String, Int> {
    val f = fopen(from, "r") ?: throw Exception("Cannot open file $from because: ${posix_errno()}")

    fseek(f, 0, SEEK_END)
    val size = ftell(f)
    fseek(f, 0, SEEK_SET)
    val contents = memScope.allocArray<ByteVar>(size)
    fread(contents, 1, size.toULong(), f)
    fclose(f)
    return Pair(contents.toKString(), size)
}