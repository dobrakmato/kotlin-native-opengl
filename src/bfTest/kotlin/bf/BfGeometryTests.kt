package bf

import io.ByteBuffer
import io.free
import kotlinx.cinterop.get
import platform.posix.pow
import kotlin.test.*

class BfGeometryTests {

    @Test
    fun `bf geometry flags`() {
        val ref1 = BfGeometryFlags.create()
            .with(BF_GEOMETRY_FLAG_LZ4)
            .with(BF_GEOMETRY_FLAG_LONG_INDICES)
        val ref2 = BfGeometryFlags.create()
            .with(BF_GEOMETRY_FLAG_LZ4)
            .with(BF_GEOMETRY_FLAG_LZ4_HC)
            .with(BF_GEOMETRY_FLAG_HAS_BONES)


        assertTrue(ref1.lz4())
        assertTrue(ref1.longIndices())
        assertFalse(ref1.lz4hc())
        assertFalse(ref1.hasBones())

        assertTrue(ref2.lz4())
        assertTrue(ref2.lz4hc())
        assertTrue(ref2.hasBones())
    }

    @Test
    fun `bf geometry header reading and write`() {
        val buffer = ByteBuffer.create(20)
        val geometryHeader = BfGeometryHeader(
            BfHeader(fileType = BfFileType.GEOMETRY),
            BfGeometryFlags.create()
                .with(BF_GEOMETRY_FLAG_LZ4)
                .with(BF_GEOMETRY_FLAG_LONG_INDICES),
            0u,
            48264u
        )

        buffer.writeBfGeometryHeader(geometryHeader)
        buffer.pos = 0
        assertEquals(geometryHeader, buffer.readBfGeometryHeader())

        buffer.free()
    }

    @Test
    fun `bf geometry list type`() {
        val buffer = ByteBuffer.create(64)
        buffer.writeBfGeometryListType(BfGeometryListType.BF_GEOMETRY_LIST_POSITIONS)
        buffer.writeBfGeometryListType(BfGeometryListType.BF_GEOMETRY_LIST_COLORS)
        buffer.writeBfGeometryListType(BfGeometryListType.BF_GEOMETRY_LIST_NORMALS)
        buffer.writeBfGeometryListType(BfGeometryListType.BF_GEOMETRY_LIST_TANGENTS)
        buffer.writeBfGeometryListType(BfGeometryListType.BF_GEOMETRY_LIST_UV1)
        buffer.writeBfGeometryListType(BfGeometryListType.BF_GEOMETRY_LIST_UV2)
        buffer.writeBfGeometryListType(BfGeometryListType.BF_GEOMETRY_LIST_UV3)
        buffer.writeBfGeometryListType(BfGeometryListType.BF_GEOMETRY_LIST_UV4)
        buffer.writeBfGeometryListType(BfGeometryListType.BF_GEOMETRY_LIST_UV5)
        buffer.writeBfGeometryListType(BfGeometryListType.BF_GEOMETRY_LIST_UV6)
        buffer.writeBfGeometryListType(BfGeometryListType.BF_GEOMETRY_LIST_INDICES)
        buffer.writeUByte(255u)

        buffer.pos = 0
        assertEquals(BfGeometryListType.BF_GEOMETRY_LIST_POSITIONS, buffer.readBfGeometryListType())
        assertEquals(BfGeometryListType.BF_GEOMETRY_LIST_COLORS, buffer.readBfGeometryListType())
        assertEquals(BfGeometryListType.BF_GEOMETRY_LIST_NORMALS, buffer.readBfGeometryListType())
        assertEquals(BfGeometryListType.BF_GEOMETRY_LIST_TANGENTS, buffer.readBfGeometryListType())
        assertEquals(BfGeometryListType.BF_GEOMETRY_LIST_UV1, buffer.readBfGeometryListType())
        assertEquals(BfGeometryListType.BF_GEOMETRY_LIST_UV2, buffer.readBfGeometryListType())
        assertEquals(BfGeometryListType.BF_GEOMETRY_LIST_UV3, buffer.readBfGeometryListType())
        assertEquals(BfGeometryListType.BF_GEOMETRY_LIST_UV4, buffer.readBfGeometryListType())
        assertEquals(BfGeometryListType.BF_GEOMETRY_LIST_UV5, buffer.readBfGeometryListType())
        assertEquals(BfGeometryListType.BF_GEOMETRY_LIST_UV6, buffer.readBfGeometryListType())
        assertEquals(BfGeometryListType.BF_GEOMETRY_LIST_INDICES, buffer.readBfGeometryListType())

        assertFails {
            buffer.readBfGeometryListType()
        }

        buffer.free()
    }

    fun `reading bf geometry file`() {
        val buffer = ByteBuffer.create(512)
        val header = buffer.readBfGeometryHeader()

        /* prepare the payload */
        val payload = if (header.flags.lz4()) {
            val decompressedSize = 0 // file length - header
            val result = buffer.readLZ4Decompressed(header.uncompressedSize.toInt(), decompressedSize)
            buffer.free() // free original buffer

            result
        } else {
            buffer
        }

        fun readList() = null // loading function

        while (payload.pos < payload.size) {
            when (buffer.readBfGeometryListType()) {
                BfGeometryListType.BF_GEOMETRY_LIST_POSITIONS -> readList()
                BfGeometryListType.BF_GEOMETRY_LIST_COLORS -> readList()
                BfGeometryListType.BF_GEOMETRY_LIST_NORMALS -> readList()
                BfGeometryListType.BF_GEOMETRY_LIST_TANGENTS -> readList()
                BfGeometryListType.BF_GEOMETRY_LIST_UV1 -> readList()
                BfGeometryListType.BF_GEOMETRY_LIST_UV2 -> readList()
                BfGeometryListType.BF_GEOMETRY_LIST_UV3 -> readList()
                BfGeometryListType.BF_GEOMETRY_LIST_UV4 -> readList()
                BfGeometryListType.BF_GEOMETRY_LIST_UV5 -> readList()
                BfGeometryListType.BF_GEOMETRY_LIST_UV6 -> readList()
                BfGeometryListType.BF_GEOMETRY_LIST_INDICES -> readList()
            }
        }

        payload.free()
    }
}