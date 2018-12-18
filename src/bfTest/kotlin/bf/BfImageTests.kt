package bf

import io.ByteBuffer
import io.free
import kotlinx.cinterop.get
import platform.posix.pow
import kotlin.test.*

class BfImageTests {

    @Test
    fun `bf image extra`() {
        val bfImageExtra1 = BfImageExtra.create(3, 8)
        val bfImageExtra2 = BfImageExtra.create(1, 15)
        val bfImageExtra3 = BfImageExtra.create(2, 0)
        val bfImageExtra4 = BfImageExtra.create(4, 1)
        assertEquals(3, bfImageExtra1.numberOfChannels())
        assertTrue(bfImageExtra1.hasMipmaps())
        assertEquals(8, bfImageExtra1.includedMipmaps())

        assertEquals(1, bfImageExtra2.numberOfChannels())
        assertTrue(bfImageExtra2.hasMipmaps())
        assertEquals(15, bfImageExtra2.includedMipmaps())

        assertEquals(2, bfImageExtra3.numberOfChannels())
        assertFalse(bfImageExtra3.hasMipmaps())
        assertEquals(1, bfImageExtra3.includedMipmaps())

        assertEquals(4, bfImageExtra4.numberOfChannels())
        assertTrue(bfImageExtra4.hasMipmaps())
        assertEquals(1, bfImageExtra4.includedMipmaps())
    }

    @Test
    fun `bf image extra read write`() {
        val buffer = ByteBuffer.create(20)
        val bfImageExtra1 = BfImageExtra.create(3, 8)
        val bfImageExtra2 = BfImageExtra.create(1, 15)

        buffer.writeBfImageExtra(bfImageExtra1)
        buffer.pos = 0
        assertEquals(bfImageExtra1, buffer.readBfImageExtra())
        buffer.pos = 0

        buffer.writeBfImageExtra(bfImageExtra2)
        buffer.pos = 0
        assertEquals(bfImageExtra2, buffer.readBfImageExtra())
        buffer.free()
    }

    @Test
    fun `bf image extra bad number of mipmaps`() {
        assertFails {
            BfImageExtra.create(3, 20)
        }
        assertFails {
            BfImageExtra.create(3, -5)
        }
    }

    @Test
    fun `bf image extra bad number of channels`() {
        assertFails {
            BfImageExtra.create(0, 0)
        }
        assertFails {
            BfImageExtra.create(7, 0)
        }
        assertFails {
            BfImageExtra.create(-6, 0)
        }
    }

    @Test
    fun `bf image flags`() {
        val ref1 = BfImageFlags.create()
            .with(BF_IMAGE_FLAG_VERTICAL_FLIP)
            .with(BF_IMAGE_FLAG_GAMMA_SRGB)
            .with(BF_IMAGE_FLAG_LZ4_HC)
            .with(BF_IMAGE_FLAG_FLOAT)
        val ref2 = BfImageFlags.create()
            .with(BF_IMAGE_FLAG_LZ4)
            .with(BF_IMAGE_FLAG_LZ4_HC)
            .with(BF_IMAGE_FLAG_DXT)

        assertTrue(ref1.verticallyFlipped())
        assertTrue(ref1.srgb())
        assertTrue(ref1.lz4hc())
        assertTrue(ref1.float())
        assertFalse(ref1.lz4())
        assertFalse(ref1.dxt())

        assertTrue(ref2.lz4())
        assertTrue(ref2.lz4hc())
        assertTrue(ref2.dxt())
        assertFalse(ref2.srgb())
        assertFalse(ref2.is16bit())
        assertFalse(ref2.skybox())
    }

    @Test
    fun `bf image flags read write`() {
        val buffer = ByteBuffer.create(20)
        val ref1 = BfImageFlags.create()
            .with(BF_IMAGE_FLAG_VERTICAL_FLIP)
            .with(BF_IMAGE_FLAG_GAMMA_SRGB)
            .with(BF_IMAGE_FLAG_LZ4_HC)
            .with(BF_IMAGE_FLAG_FLOAT)
        val ref2 = BfImageFlags.create()
            .with(BF_IMAGE_FLAG_LZ4)
            .with(BF_IMAGE_FLAG_LZ4_HC)
            .with(BF_IMAGE_FLAG_DXT)

        buffer.writeBfImageFlags(ref1)
        buffer.pos = 0
        assertEquals(ref1, buffer.readBfImageFlags())
        buffer.pos = 0

        buffer.writeBfImageFlags(ref2)
        buffer.pos = 0
        assertEquals(ref2, buffer.readBfImageFlags())
        buffer.free()
    }

    @Test
    fun `writing and reding bf file type`() {
        val buffer = ByteBuffer.create(20)
        val imageHeader = BfImageHeader(
            BfHeader(fileType = BfFileType.IMAGE),
            BfImageFlags.create().with(BF_IMAGE_FLAG_LZ4).with(BF_IMAGE_FLAG_DXT).with(BF_IMAGE_FLAG_GAMMA_SRGB),
            BfImageExtra.create(3, 8),
            1600u,
            900u
        )

        buffer.writeBfImageHeader(imageHeader)
        buffer.pos = 0
        assertEquals(imageHeader, buffer.readBfImageHeader())

        buffer.free()
    }


}