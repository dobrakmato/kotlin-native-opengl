package io

import kotlinx.cinterop.*
import platform.windows.CHARVar
import kotlin.test.*

class ByteBufferTests {

    @Test
    fun `check bounds while writing`() {
        val buffer = ByteBuffer.create(1)
        buffer.writeByte(1)
        assertFails {
            buffer.writeByte(2)
        }
        assertFails {
            buffer.writeUByte(2u)
        }
        assertFails {
            buffer.writeShort(3)
        }
        assertFails {
            buffer.writeUShort(3u)
        }
        assertFails {
            buffer.writeInt(4)
        }
        assertFails {
            buffer.writeUInt(4u)
        }
        assertFails {
            buffer.writeLong(5)
        }
        assertFails {
            buffer.writeULong(5u)
        }
        assertFails {
            buffer.writeFloat(3.14f)
        }
        assertFails {
            buffer.writeDouble(3.14)
        }
        assertFails {
            buffer.writeString("hello kotlin")
        }
        buffer.free()
    }

    @Test
    fun `check bounds while reading`() {
        val buffer = ByteBuffer.create(2)
        buffer.writeByte(1)
        buffer.writeByte(2)
        buffer.pos = 0
        buffer.readByte()
        buffer.readByte()
        assertFails {
            buffer.readByte()
        }
        assertFails {
            buffer.readUByte()
        }
        assertFails {
            buffer.readShort()
        }
        assertFails {
            buffer.readUShort()
        }
        assertFails {
            buffer.readInt()
        }
        assertFails {
            buffer.readUInt()
        }
        assertFails {
            buffer.readLong()
        }
        assertFails {
            buffer.readULong()
        }
        assertFails {
            buffer.readFloat()
        }
        assertFails {
            buffer.readDouble()
        }
        assertFails {
            buffer.readString()
        }
        buffer.free()
    }

    @Test
    fun `write and read byte`() {
        val buffer = ByteBuffer.create(10)
        buffer.writeByte(58)
        buffer.writeByte(24)
        buffer.writeByte(-13)
        buffer.writeByte(1)

        buffer.pos = 0

        assertEquals(58.toByte(), buffer.readByte())
        assertEquals(24.toByte(), buffer.readByte())
        assertEquals((-13).toByte(), buffer.readByte())
        assertEquals(1.toByte(), buffer.readByte())

        buffer.free()
    }

    @Test
    fun `write and read ubyte`() {
        val buffer = ByteBuffer.create(10)
        buffer.writeUByte(58u)
        buffer.writeUByte(24u)
        buffer.writeUByte(13u)
        buffer.writeUByte(1u)

        buffer.pos = 0

        assertEquals(58u, buffer.readUByte())
        assertEquals(24u, buffer.readUByte())
        assertEquals(13u, buffer.readUByte())
        assertEquals(1u, buffer.readUByte())

        assertEquals(58u, buffer.uByteAt(0))
        assertEquals(13u, buffer.uByteAt(2))
        buffer.uByteAt(2, 55u)
        assertEquals(55u, buffer.uByteAt(2))

        buffer.free()
    }

    @Test
    fun `write and read short`() {
        val buffer = ByteBuffer.create(10)
        buffer.writeShort(58)
        buffer.writeShort((Short.MAX_VALUE - 5).toShort())
        buffer.writeShort(-13)
        buffer.writeShort((Short.MIN_VALUE + 12).toShort())

        buffer.pos = 0

        assertEquals(58.toShort(), buffer.readShort())
        assertEquals((Short.MAX_VALUE - 5).toShort(), buffer.readShort())
        assertEquals((-13).toShort(), buffer.readShort())
        assertEquals((Short.MIN_VALUE + 12).toShort(), buffer.readShort())

        buffer.free()
    }

    @Test
    fun `write and read ushort`() {
        val buffer = ByteBuffer.create(10)
        buffer.writeUShort(58u)
        buffer.writeUShort(2864u)
        buffer.writeUShort(63251u)

        buffer.pos = 0

        assertEquals(58u, buffer.readUShort())
        assertEquals(2864u, buffer.readUShort())
        assertEquals(63251u, buffer.readUShort())

        assertEquals(2864u, buffer.uShortAt(2))
        buffer.uShortAt(2, 90u)
        assertEquals(90u, buffer.uShortAt(2))
        assertEquals(63251u, buffer.uShortAt(4))

        buffer.free()
    }

    @Test
    fun `write and read int`() {
        val buffer = ByteBuffer.create(16)
        buffer.writeInt(89)
        buffer.writeInt(Int.MAX_VALUE - 3)
        buffer.writeInt(-25)
        buffer.writeInt(Int.MIN_VALUE + 3)

        buffer.pos = 0

        assertEquals(89, buffer.readInt())
        assertEquals(Int.MAX_VALUE - 3, buffer.readInt())
        assertEquals(-25, buffer.readInt())
        assertEquals(Int.MIN_VALUE + 3, buffer.readInt())

        buffer.free()
    }

    @Test
    fun `write and read uint`() {
        val buffer = ByteBuffer.create(16)
        buffer.writeUInt(89u)
        buffer.writeUInt(UInt.MAX_VALUE - 3u)

        buffer.pos = 0

        assertEquals(89u, buffer.readUInt())
        assertEquals(UInt.MAX_VALUE - 3u, buffer.readUInt())

        assertEquals(89u, buffer.uIntAt(0))
        buffer.uIntAt(0, 48u)
        assertEquals(48u, buffer.uIntAt(0))
        assertEquals(UInt.MAX_VALUE - 3u, buffer.uIntAt(4))

        buffer.free()
    }

    @Test
    fun `write and read long`() {
        val buffer = ByteBuffer.create(24)
        buffer.writeLong(89)
        buffer.writeLong(Long.MAX_VALUE - 3)
        buffer.writeLong(Long.MIN_VALUE + 5)

        buffer.pos = 0

        assertEquals(89, buffer.readLong())
        assertEquals(Long.MAX_VALUE - 3, buffer.readLong())
        assertEquals(Long.MIN_VALUE + 5, buffer.readLong())

        buffer.free()
    }

    @Test
    fun `write and read ulong`() {
        val buffer = ByteBuffer.create(24)
        buffer.writeULong(89u)
        buffer.writeULong(ULong.MAX_VALUE - 3u)

        buffer.pos = 0

        assertEquals(89u, buffer.readULong())
        assertEquals(ULong.MAX_VALUE - 3u, buffer.readULong())

        assertEquals(89u, buffer.uLongAt(0))
        assertEquals(ULong.MAX_VALUE - 3u, buffer.uLongAt(8))
        buffer.uLongAt(4, 8847234138u)
        assertEquals(8847234138u, buffer.uLongAt(4))

        buffer.free()
    }

    @Test
    fun `write and read float`() {
        val buffer = ByteBuffer.create(10)
        buffer.writeFloat(3.14159f)
        buffer.writeFloat(Float.MAX_VALUE - 5f)

        buffer.pos = 0

        assertEquals(3.14159f, buffer.readFloat())
        assertEquals(Float.MAX_VALUE - 5f, buffer.readFloat())

        buffer.free()
    }

    @Test
    fun `write and read double`() {
        val buffer = ByteBuffer.create(24)
        buffer.writeDouble(3.14159265)
        buffer.writeDouble(Double.MAX_VALUE - 5.23)
        buffer.writeDouble(Double.MIN_VALUE - 5.23)

        buffer.pos = 0

        assertEquals(3.14159265, buffer.readDouble())
        assertEquals(Double.MAX_VALUE - 5.23, buffer.readDouble())
        assertEquals(Double.MIN_VALUE - 5.23, buffer.readDouble())

        buffer.free()
    }

    @Test
    fun `write and read string`() {
        val buffer = ByteBuffer.create(48)
        buffer.writeString("kotlin")
        buffer.writeFloat(3.14f)
        buffer.writeString("hello\uD83D\uDE0E")

        buffer.pos = 0

        assertEquals("kotlin", buffer.readString())
        assertEquals(3.14f, buffer.readFloat())
        assertEquals("hello\uD83D\uDE0E", buffer.readString())

        buffer.pos = 0

        buffer.writeString("test")

        buffer.pos = 0

        assertEquals("test", buffer.readString())

        buffer.free()
    }

    @Test
    fun `write and read combined`() {
        val buffer = ByteBuffer.create(256)
        buffer.writeString("kotlin")
        buffer.writeFloat(3.14f)
        buffer.writeString("hello\uD83D\uDE0E")
        buffer.writeUShort(64u)
        buffer.writeByte(-13)
        buffer.writeInt(78 * 64)
        buffer.writeString("ahoj")
        buffer.writeDouble(28.159712)

        buffer.pos = 0

        assertEquals("kotlin", buffer.readString())
        assertEquals(3.14f, buffer.readFloat())
        assertEquals("hello\uD83D\uDE0E", buffer.readString())
        assertEquals(64u, buffer.readUShort())
        assertEquals(-13, buffer.readByte())
        assertEquals(78 * 64, buffer.readInt())
        assertEquals("ahoj", buffer.readString())
        assertEquals(28.159712, buffer.readDouble())


        buffer.free()
    }

    @Test
    fun `pointer to`() {
        val buffer = ByteBuffer.create(20)
        assertEquals(buffer.data, buffer.pointerToPosition())
        assertEquals(buffer.data, buffer.pointerTo(0))
        assertNotEquals(buffer.data, buffer.pointerTo(1))
        assertFails {
            buffer.pointerTo(-1)
        }
        buffer.writeString("kotlin")
        assertEquals((buffer.data.toLong() + 4L).toCPointer<UByteVar>(), buffer.pointerTo(4))

        // memory: 6[int] k[byte] o[byte] t l i n
        assertEquals(6, buffer.pointerTo(0).toLong().toCPointer<IntVar>()!!.pointed.value)
        assertEquals(108.toUByte(), buffer.pointerTo(7).toLong().toCPointer<UByteVar>()!!.pointed.value)
        assertEquals(105.toUByte(), buffer.pointerTo(8).toLong().toCPointer<UByteVar>()!!.pointed.value)
        assertEquals(110.toUByte(), buffer.pointerTo(9).toLong().toCPointer<UByteVar>()!!.pointed.value)

        buffer.free()
    }

    @Test
    fun `read and write raw bytes`() {
        val buffer = ByteBuffer.create(256)
        val buffer2 = ByteBuffer.create(256)
        buffer2.writeInt(42)
        buffer2.writeFloat(1.42f)
        buffer2.writeDouble(1.42123)

        buffer.writeString("kotlin")
        buffer.writeFloat(3.14f)
        buffer.writeBytes(buffer2.pointerTo(0), 4 + 4 + 8)
        buffer.writeFloat(7815.1478f)
        buffer.writeString("helo")

        buffer.pos = 0

        assertEquals("kotlin", buffer.readString())
        assertEquals(3.14f, buffer.readFloat())

        val buffer3 = ByteBuffer.create(256)
        buffer.readBytes(buffer3.pointerTo(0), 4 + 4 + 8)

        for (i in 0 until (4 + 4 + 8)) {
            assertEquals(buffer3.uByteAt(i), buffer.uByteAt(buffer.pos - (4 + 4 + 8) + i))
        }
        assertEquals(7815.1478f, buffer.readFloat())
        assertEquals("helo", buffer.readString())

        buffer.free()
    }

    @Test
    fun `buffer view`() {
        val buffer = ByteBuffer.create(256)

        buffer.writeInt(4)
        buffer.writeInt(12)
        buffer.writeInt(-10)
        buffer.writeInt(40)

        val buffer2 = buffer.slice(0, 8)

        assertEquals(4, buffer2.readInt())
        assertEquals(12, buffer2.readInt())
        assertFails {
            buffer2.readInt()
        }

        val buffer3 = buffer.slice(4, 12)
        assertEquals(12, buffer3.readInt())
        assertEquals(-10, buffer3.readInt())
        assertFails {
            buffer3.readInt()
        }

        /* test propagate back to original */
        buffer3.pos = 0
        buffer3.writeInt(88)

        buffer.pos = 0
        assertEquals(4, buffer.readInt())
        assertEquals(88, buffer.readInt())
    }
}