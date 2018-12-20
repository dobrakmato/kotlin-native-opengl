package math

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertTrue

class ScalarTests {
    @Test
    fun clamp() {
        assertEquals(4.0, clamp(0.0, 4.0, 8.0))
        assertEquals(0.0, clamp(0.0, 4.0, -20.0))

        assertEquals(4f, clamp(0f, 4f, 8f))
        assertEquals(0f, clamp(0f, 4f, -20f))

        assertEquals(4, clamp(0, 4, 8))
        assertEquals(0, clamp(0, 4, -20))
        assertEquals(3, clamp(0, 4, 3))

        assertEquals(4L, clamp(0L, 4, 8))
        assertEquals(0L, clamp(0L, 4, -2))
        assertEquals(3L, clamp(0L, 4, 3))
    }

    @Test
    fun saturate() {
        assertEquals(0.0f, saturate(-6f))
        assertEquals(1.0, saturate(6.0))
        assertEquals(1, saturate(255))
        assertEquals(0L, saturate(-65400L))
    }

    @Test
    fun lerp() {
        assertEquals(0.5f, lerp(0f, 1f, 0.5f))
        assertEquals(6.0, lerp(0.0, 8.0, 0.75))
    }

    @Test
    fun pow() {
        assertEquals(8 * 8f, pow2(8f))
        assertEquals(11 * 11 * 11f, pow3(11f))
    }

    @Test
    fun toDegrees() {
        assertEquals(360f, toDegrees(TWO_PI))
        assertEquals(180f, toDegrees(PI))
        assertEquals(90f, toDegrees(HALF_PI))
    }

    @Test
    fun toRadians() {
        assertEquals(TWO_PI, toRadians(360f))
        assertEquals(PI, toRadians(180f))
        assertEquals(HALF_PI, toRadians(90f))
    }

    @Test
    fun safeDiv() {
        assertEquals(4f, 8f safediv 2f)
        assertFails {
            4f safediv 0f
        }
    }
}