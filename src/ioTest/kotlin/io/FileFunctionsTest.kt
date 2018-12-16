package io

import kotlinx.cinterop.memScoped
import kotlin.test.Test
import kotlin.test.assertEquals

class FileFunctionsTest {

    @Test
    fun `test text file`() {
        val cnts = "Hello World!\nMultiline string\uD83E\uDD37\uD83C\uDFFC..."
        writeUTF8TextFile("./text_file.txt", cnts)
        assertEquals(cnts, readUTF8TextFile("./text_file.txt"))
    }

    @Test
    fun `test binary file`() {
        memScoped {
            val cnts = ByteBuffer.create(11)
            cnts.writeUByte(123u)
            cnts.writeUByte(25u)
            cnts.writeUByte(99u)
            cnts.writeUByte(255u)
            cnts.writeUByte(67u)
            cnts.writeUByte(2u)
            cnts.writeUByte(79u)
            cnts.writeUByte(195u)
            cnts.writeUByte(172u)
            cnts.writeUByte(10u)
            cnts.writeUByte(77u)
            writeBinaryFile("./binary_file.bin", cnts)
            assertEquals(cnts, readBinaryFile("./binary_file.bin"))
        }
    }
}