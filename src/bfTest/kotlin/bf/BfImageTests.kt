package bf

import io.ByteBuffer
import io.free
import platform.posix.pow
import kotlin.test.*

class BfImageTests {

    @Test
    fun `bf image header byte size`() {
        assertEquals(UByte.SIZE_BYTES, BfImageExtra.SIZE_BYTES)
        assertEquals(UByte.SIZE_BYTES, BfImageFlags.SIZE_BYTES)
        assertEquals(
            BfHeader.SIZE_BYTES + UByte.SIZE_BYTES + UByte.SIZE_BYTES + UShort.SIZE_BYTES * 2,
            BfImageHeader.SIZE_BYTES
        )
    }

    @Test
    fun `bf compute payload size`() {
        assertEquals(
            128 * 128 * 3, computeBfImagePayloadSize(
                BfImageHeader(
                    BfHeader(fileType = BfFileType.IMAGE),
                    BfImageFlags.create(),
                    BfImageExtra.create(3, 0),
                    128u,
                    128u
                )
            )
        )

        assertEquals(
            128 * 128 * 3 + 64 * 64 * 3 + 32 * 32 * 3, computeBfImagePayloadSize(
                BfImageHeader(
                    BfHeader(fileType = BfFileType.IMAGE),
                    BfImageFlags.create().with(BF_IMAGE_FLAG_LZ4),
                    BfImageExtra.create(3, 2),
                    128u,
                    128u
                )
            )
        )

        assertEquals(
            128 * 128 * 3  / 6, computeBfImagePayloadSize(
                BfImageHeader(
                    BfHeader(fileType = BfFileType.IMAGE),
                    BfImageFlags.create().with(BF_IMAGE_FLAG_DXT),
                    BfImageExtra.create(3, 0),
                    128u,
                    128u
                )
            )
        )
    }

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
        assertEquals(0, bfImageExtra3.includedMipmaps())

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
            .with(BF_IMAGE_FLAG_UNUSED)
            .with(BF_IMAGE_FLAG_FLOAT)
        val ref2 = BfImageFlags.create()
            .with(BF_IMAGE_FLAG_LZ4)
            .with(BF_IMAGE_FLAG_UNUSED)
            .with(BF_IMAGE_FLAG_DXT)

        assertTrue(ref1.verticallyFlipped())
        assertTrue(ref1.srgb())
        assertTrue(ref1.unused1())
        assertTrue(ref1.float())
        assertFalse(ref1.lz4())
        assertFalse(ref1.dxt())

        assertTrue(ref2.lz4())
        assertTrue(ref2.unused1())
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
            .with(BF_IMAGE_FLAG_UNUSED)
            .with(BF_IMAGE_FLAG_FLOAT)
        val ref2 = BfImageFlags.create()
            .with(BF_IMAGE_FLAG_LZ4)
            .with(BF_IMAGE_FLAG_UNUSED)
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

    fun `reading bf image file`() {
        val buffer = ByteBuffer.create(512)
        val header = buffer.readBfImageHeader()

        /* prepare the payload */
        val payload = if (header.flags.lz4()) {
            val uncompressedSize = 2048 // compute from header
            val decompressedSize = 0 // file length - header
            buffer.readLZ4Decompressed(uncompressedSize, decompressedSize).data.rawValue
        } else {
            buffer.data.rawValue + buffer.pos.toLong()
        }

        /* upload the mipmaps */
        for (i in 0 until header.extra.includedMipmaps()) {
            val divisor = pow(2.0, i.toDouble()).toInt().toUInt()
            val width = header.width / divisor
            val height = header.height / divisor
            val uncompressedTextureSize = 0

            // upload(payload, width, height, header.format)
            // payload += uncompressedTextureSize
        }
    }
}