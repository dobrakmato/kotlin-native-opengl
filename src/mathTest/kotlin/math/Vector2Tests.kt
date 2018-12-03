package math

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue

class Vector2Tests {
    @Test
    fun `absolute value`() {
        assertEquals(Vector2f(5f, 22.5f), Vector2f(-5f, -22.5f).abs())
    }

    @Test
    fun negate() {
        assertEquals(Vector2f(-1f, 2.5f), -Vector2f(1f, -2.5f))
    }

    @Test
    fun add() {
        assertEquals(Vector2f(15f, 10f), Vector2f(10f, 15f) + Vector2f(5f, -5f))
    }

    @Test
    fun subtract() {
        assertEquals(Vector2f(5f, 20f), Vector2f(10f, 15f) - Vector2f(5f, -5f))
    }

    @Test
    fun multiply() {
        assertEquals(Vector2f(2f, 4f), Vector2f(1f, 2f) * 2f)
    }

    @Test
    fun divide() {
        assertEquals(Vector2f(1f, 2f), Vector2f(2f, 4f) / 2f)
    }

    @Test
    fun `dot product`() {
        assertEquals(12f, Vector2f(1f, 5f) dot Vector2f(2f, 2f))
    }

    @Test
    fun normalize() {
        assertEquals(Vector2f(1f, 0f), Vector2f(10f, 0f).normalized())
        assertEquals(
            Vector2f(0.707106782f, 0.707106782f),
            Vector2f(1f, 1f).normalized()
        )
    }

    @Test
    fun length() {
        assertTrue(50.1168634f - Vector2f(1.3f, 50.1f).length()  < 0.001f)
    }

    @Test
    fun `length squared`() {
        assertTrue(2511.7 - Vector2f(1.3f, 50.1f).lengthSquared() < 0.001f)
    }

    @Test
    fun distance() {
        val from = Vector2f(10f, 15.5f)
        val to = Vector2f(18f, -10f)
        assertTrue(distance(from, to) > 0)
        assertTrue(26.725456f - distance(from, to) < 0.001f)
    }

    @Test
    fun `distance squared`() {
        val from = Vector2f(10f, 15.5f)
        val to = Vector2f(18f, -10f)
        assertTrue(distanceSquared(from, to) > 0)
        assertTrue(714.25 - distanceSquared(from, to) < 0.01f)
    }
}