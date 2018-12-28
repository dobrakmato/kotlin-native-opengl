package bf

import io.ByteBuffer
import io.free
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails

class BfGeneralTests {

    @Test
    fun `header byte size`() {
        assertEquals(Int.SIZE_BYTES + UByte.SIZE_BYTES + UByte.SIZE_BYTES, BfHeader.SIZE_BYTES)
    }

    @Test
    fun `writing and reding bf file type`() {
        val buffer = ByteBuffer.create(20)
        buffer.writeBfFileType(BfFileType.SCENE)
        buffer.writeBfFileType(BfFileType.IMAGE)
        buffer.writeBfFileType(BfFileType.AUDIO)
        buffer.writeBfFileType(BfFileType.IMAGE)
        buffer.pos = 0
        assertEquals(BfFileType.SCENE, buffer.readBfFileType())
        assertEquals(BfFileType.IMAGE, buffer.readBfFileType())
        assertEquals(BfFileType.AUDIO, buffer.readBfFileType())
        assertEquals(BfFileType.IMAGE, buffer.readBfFileType())

        buffer.free()
    }

    @Test
    fun `writing and reding bfheader`() {
        val buffer = ByteBuffer.create(20)
        val bfImageHeader = BfHeader(BF_MAGIC, BF_VERSION, BfFileType.IMAGE)
        val bfAudioHeader = BfHeader(BF_MAGIC, 1u, BfFileType.AUDIO)

        buffer.writeBfHeader(bfImageHeader)
        buffer.pos = 0
        assertEquals(bfImageHeader, buffer.readBfHeader())

        buffer.pos = 0
        buffer.writeBfHeader(bfAudioHeader)
        buffer.pos = 0
        assertEquals(bfAudioHeader, buffer.readBfHeader())

        buffer.free()
    }

    @Test
    fun `reading invalid bf file`() {
        val buffer = ByteBuffer.create(20)
        buffer.writeInt(128)
        buffer.writeInt(915)
        buffer.writeInt(-500)
        buffer.pos = 0

        assertFails {
            buffer.readBfHeader()
        }

        buffer.free()
    }
}