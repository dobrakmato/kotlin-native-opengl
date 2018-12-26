package math

import kotlin.test.*

class Vector3Tests {
    @Test
    fun `absolute value`() {
        assertEquals(Vector3f(5f, 22.5f, 15.3f), Vector3f(-5f, 22.5f, -15.3f).abs())
    }

    @Test
    fun negate() {
        assertEquals(Vector3f(-1f, 2.5f, 3f), -Vector3f(1f, -2.5f, -3f))
    }

    @Test
    fun add() {
        assertEquals(Vector3f(15f, 10f, 11f), Vector3f(10f, 15f, 10.5f) + Vector3f(5f, -5f, 0.5f))
    }

    @Test
    fun subtract() {
        assertEquals(Vector3f(5f, 20f, 10f), Vector3f(10f, 15f, 10.5f) - Vector3f(5f, -5f, 0.5f))
    }

    @Test
    fun multiply() {
        assertEquals(Vector3f(2f, 4f, 6f), Vector3f(1f, 2f, 3f) * 2f)
    }

    @Test
    fun divide() {
        assertEquals(Vector3f(1f, 2f, 3f), Vector3f(2f, 4f, 6f) / 2f)
    }

    @Test
    fun `dot product`() {
        assertEquals(7f, Vector3f(1f, 5f, 1f) dot Vector3f(2f, 2f, -5f))
    }

    @Test
    fun `cross product`() {
        val cross = Vector3f(4f, 5.6f, 11f) cross Vector3f(7f, 20f, -5f)
        assertEquals(-248f, cross.x)
        assertEquals(97f, cross.y)
        assertEquals(40.8f, cross.z)
    }

    @Test
    fun normalize() {
        assertEquals(Vector3f(1f, 0f, 0f), Vector3f(10f, 0f, 0f).normalized())
        assertEquals(
            Vector3f(0.5773502691896258f, 0.5773502691896258f, 0.5773502691896258f),
            Vector3f(1f, 1f, 1f).normalized()
        )
    }

    @Test
    fun length() {
        assertEquals(52.1225709266f, Vector3f(1.3f, 50.1f, 14.32f).length())
    }

    @Test
    fun `length squared`() {
        assertTrue(2716.7624f - Vector3f(1.3f, 50.1f, 14.32f).lengthSquared() < 0.001f)
    }

    @Test
    fun distance() {
        val from = Vector3f(10f, 15.5f, 14.24f)
        val to = Vector3f(18f, -10f, 22.515f)
        assertTrue(distance(from, to) > 0)
        assertTrue(27.9772340484f - distance(from, to) < 0.001f)
    }

    @Test
    fun `distance squared`() {
        val from = Vector3f(10f, 15.5f, 14.24f)
        val to = Vector3f(18f, -10f, 22.515f)
        assertTrue(distanceSquared(from, to) > 0)
        assertTrue(782.725625f - distanceSquared(from, to) < 0.01f)
    }

    @Test
    fun `companion object`() {
        assertEquals(Vector3f(1f, 0f, 0f), Vector3f.UNIT_X)
        assertEquals(Vector3f(0f, 1f, 0f), Vector3f.UNIT_Y)
        assertEquals(Vector3f(0f, 0f, 1f), Vector3f.UNIT_Z)
        assertEquals(Vector3f(0f, 0f, 0f), Vector3f.ZERO)
        assertEquals(Vector3f(1f, 1f, 1f), Vector3f.ONE)

        assertEquals(Vector3f(1f, 4f, -9f), Vector3f(1f, 4f, -9f))
        assertNotEquals(Vector3f.RANDOM, Vector3f.RANDOM)

    }

    @Test
    fun clamp() {
        assertEquals(
            Vector3f(1f, 1f, 1f),
            clamp(Vector3f.ONE, Vector3f(2f, 2f, 2f), Vector3f(-4f, 0f, -5f))
        )

        assertEquals(
            Vector3f(2f, 2f, 2f),
            clamp(Vector3f.ONE, Vector3f(2f, 2f, 2f), Vector3f(40f, 10f, 4f))
        )

        assertEquals(
            Vector3f(1f, 2f, 1.5f),
            clamp(Vector3f.ONE, Vector3f(2f, 2f, 2f), Vector3f(1f, 2f, 1.5f))
        )
    }

    @Test
    fun lerp() {
        assertEquals(Vector3f(0.25f, 0.25f, 0.25f), lerp(Vector3f.ZERO, Vector3f.ONE, 0.25f))
        assertEquals(Vector3f(0f, 0.25f, 0f), lerp(Vector3f.ZERO, Vector3f.UNIT_Y, 0.25f))
        assertEquals(Vector3f(0f, 0f, 0f), lerp(Vector3f.ZERO, Vector3f.UNIT_Y, 0f))
        assertEquals(Vector3f(0f, 1f, 0f), lerp(Vector3f.ZERO, Vector3f.UNIT_Y, 1f))

        assertFails {
            lerp(Vector3f.ZERO, Vector3f.ONE, -1f)
        }

        assertFails {
            lerp(Vector3f.ZERO, Vector3f.ONE, 2f)
        }

        assertFails {
            lerp(Vector3f.ZERO, Vector3f.ONE, 1.1f)
        }
    }

    @Test
    fun slerp() {
        assertFails {
            slerp(Vector3f.ZERO, Vector3f.ONE, -1f)
        }

        assertFails {
            slerp(Vector3f.ZERO, Vector3f.ONE, 2f)
        }

        // todo:
    }

    @Test
    fun nlerp() {
        assertFails {
            nlerp(Vector3f.ZERO, Vector3f.ONE, -1f)
        }

        assertFails {
            nlerp(Vector3f.ZERO, Vector3f.ONE, 2f)
        }

        // todo:
    }
}