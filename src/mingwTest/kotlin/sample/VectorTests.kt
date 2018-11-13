package sample

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class VectorTests {
    @Test
    fun testAbsolute() {
        assertEquals(Vector3f(5f, 22.5f, 15.3f), Vector3f(-5f, 22.5f, -15.3f).abs())
    }

    @Test
    fun testNegate() {
        assertEquals(Vector3f(-1f, 2.5f, 3f), -Vector3f(1f, -2.5f, -3f))
    }

    @Test
    fun testAdd() {
        assertEquals(Vector3f(15f, 10f, 11f), Vector3f(10f, 15f, 10.5f) + Vector3f(5f, -5f, 0.5f))
    }

    @Test
    fun testSubtract() {
        assertEquals(Vector3f(5f, 20f, 10f), Vector3f(10f, 15f, 10.5f) - Vector3f(5f, -5f, 0.5f))
    }

    @Test
    fun testMultiply() {
        assertEquals(Vector3f(2f, 4f, 6f), Vector3f(1f, 2f, 3f) * 2f)
    }

    @Test
    fun testDivide() {
        assertEquals(Vector3f(1f, 2f, 3f), Vector3f(2f, 4f, 6f) / 2f)
    }

    @Test
    fun testDot() {
        assertEquals(7f, Vector3f(1f, 5f, 1f) dot Vector3f(2f, 2f, -5f))
    }

    @Test
    fun testCross() {
        val cross = Vector3f(4f, 5.6f, 11f) cross Vector3f(7f, 20f, -5f)
        assertEquals(-248f, cross.x)
        assertEquals(97f, cross.y)
        assertEquals(40.8f, cross.z)
    }

    @Test
    fun testNormalize() {
        assertEquals(Vector3f(1f, 0f, 0f), Vector3f(10f, 0f, 0f).normalized())
        assertEquals(
            Vector3f(0.5773502691896258f, 0.5773502691896258f, 0.5773502691896258f),
            Vector3f(1f, 1f, 1f).normalized()
        )
    }

    @Test
    fun testLength() {
        assertEquals(52.1225709266f, Vector3f(1.3f, 50.1f, 14.32f).length())
    }

    @Test
    fun testLengthSquared() {
        assertTrue(2716.7624f - Vector3f(1.3f, 50.1f, 14.32f).lengthSquared() < 0.001f)
    }

    @Test
    fun testDistance() {
        val from = Vector3f(10f, 15.5f, 14.24f)
        val to = Vector3f(18f, -10f, 22.515f)
        assertTrue(distance(from, to) > 0)
        assertTrue(27.9772340484f - distance(from, to) < 0.001f)
    }

    @Test
    fun testDistanceSquared() {
        val from = Vector3f(10f, 15.5f, 14.24f)
        val to = Vector3f(18f, -10f, 22.515f)
        assertTrue(distanceSquared(from, to) > 0)
        assertTrue(782.725625f - distanceSquared(from, to) < 0.01f)
    }
}