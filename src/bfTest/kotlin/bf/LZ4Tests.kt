package bf

import io.ByteBuffer
import io.free
import platform.posix.rand
import platform.posix.srand
import platform.posix.time
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LZ4Tests {

    fun randomBuffer(size: Int): ByteBuffer {
        srand(time(null).toUInt())
        val buffer = ByteBuffer.create(size.toLong())
        repeat(size / 4) {
            buffer.writeUInt(rand().toUInt())
        }
        return buffer
    }

    @Test
    fun `max compressed size`() {
        assertEquals(255 + 1 + 16, LZ4MaxCompressedSize(255))
    }

    @Test
    fun `basic compression`() {
        val buffer = ByteBuffer.create(512)
        buffer.writeInt(314000)
        buffer.writeUByte(18u)
        val data = randomBuffer(128)
        val compressedSize = buffer.writeLZ4Compressed(data.pointerTo(0), 128)
        assertTrue(compressedSize > 0)

        buffer.pos = 0
        assertEquals(314000, buffer.readInt())
        assertEquals(18u, buffer.readUByte())
        val decompressed = buffer.readLZ4Decompressed(compressedSize, 128)
        assertEquals(decompressed, data)

        decompressed.free()
        data.free()
        buffer.free()
    }

    @Test
    fun `tight fit compression`() {
        val buffer = ByteBuffer.create(LZ4MaxCompressedSize(255).toLong())
        val data = randomBuffer(255)
        val compressedSize = buffer.writeLZ4Compressed(data.pointerTo(0), 255)
        assertTrue(compressedSize > 0)

        buffer.pos = 0
        val decompressed = buffer.readLZ4Decompressed(compressedSize, 255)
        assertEquals(decompressed, data)

        decompressed.free()
        data.free()
        buffer.free()
    }

    @Test
    fun `hc big tight fit compression`() {
        val buffer = ByteBuffer.create(LZ4MaxCompressedSize(131072).toLong())
        val data = randomBuffer(131072)
        val compressedSize = buffer.writeLZ4HCCompressed(data.pointerTo(0), 131072)
        assertTrue(compressedSize > 0)

        buffer.pos = 0
        val decompressed = buffer.readLZ4Decompressed(compressedSize, 131072)
        assertEquals(decompressed, data)

        decompressed.free()
        data.free()
        buffer.free()
    }


    @Test
    fun `data after compressed block`() {
        val buffer = ByteBuffer.create(512)
        buffer.writeInt(314000)
        buffer.writeUByte(18u)
        val data = randomBuffer(128)
        val compressedSize = buffer.writeLZ4Compressed(data.pointerTo(0), 128)
        assertTrue(compressedSize > 0)
        buffer.writeUShort(1600u)
        buffer.writeUShort(900u)

        buffer.pos = 0
        assertEquals(314000, buffer.readInt())
        assertEquals(18u, buffer.readUByte())
        val decompressed = buffer.readLZ4Decompressed(compressedSize, 128)
        assertEquals(decompressed, data)
        assertEquals(1600u, buffer.readUShort())
        assertEquals(900u, buffer.readUShort())

        decompressed.free()
        data.free()
        buffer.free()
    }
}