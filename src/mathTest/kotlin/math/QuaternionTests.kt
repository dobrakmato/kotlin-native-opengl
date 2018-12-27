package math

import platform.posix.abs
import kotlin.math.abs
import kotlin.math.sqrt
import kotlin.test.*

class QuaternionTests {

    @Test
    fun `plus operator`() {
        val qa = Quaternion(1f, 2f, 3f, 4f)
        val qb = Quaternion(1f, -2f, 3f, -2f)
        val q = qa + qb

        assertEquals(2f, q.x)
        assertEquals(0f, q.y)
        assertEquals(6f, q.z)
        assertEquals(2f, q.w)
    }

    @Test
    fun `times constant operator`() {
        val qa = Quaternion(1f, 2f, 3f, 4f)
        var q = qa * 2f

        assertEquals(2f, q.x)
        assertEquals(4f, q.y)
        assertEquals(6f, q.z)
        assertEquals(8f, q.w)

        q *= 0.5f

        assertEquals(1f, q.x)
        assertEquals(2f, q.y)
        assertEquals(3f, q.z)
        assertEquals(4f, q.w)
    }

    @Test
    fun `times quaternion operator`() {
        val sqrt2 = sqrt(2f)
        val qa = Quaternion(1f / sqrt2, 0f, 0f, 1f / sqrt2)
        val qb = Quaternion(0f, 1f / sqrt2, 0f, 1f / sqrt2)
        val q = qa * qb

        assertTrue(abs(0.5f - q.x) < 0.001f)
        assertTrue(abs(0.5f - q.y) < 0.001f)
        assertTrue(abs(0.5f - q.z) < 0.001f)
        assertTrue(abs(0.5f - q.w) < 0.001f)

        val qc = qa * qa.conjugate()

        assertEquals(0f, qc.x)
        assertEquals(0f, qc.y)
        assertEquals(0f, qc.z)
        assertTrue(1f - qc.w < 0.0001f)
    }

    @Test
    fun length() {
        assertEquals(2f, Quaternion(1f, 1f, 1f, 1f).length())
    }

    @Test
    fun lengthSquared() {
        assertEquals(4f, Quaternion(1f, 1f, 1f, 1f).lengthSquared())
    }

    @Test
    fun normalized() {
        val q = Quaternion(1f, 1f, 1f, 1f).normalized()
        assertEquals(0.5f, q.x)
        assertEquals(0.5f, q.y)
        assertEquals(0.5f, q.z)
        assertEquals(0.5f, q.w)
    }

    @Test
    fun conjugate() {
        val q = Quaternion(1f, 1f, 1f, 1f).conjugate()
        assertEquals(-1f, q.x)
        assertEquals(-1f, q.y)
        assertEquals(-1f, q.z)
        assertEquals(1f, q.w)
    }

    @Test
    fun identity() {
        assertEquals(Quaternion(0f, 0f, 0f, 1f), Quaternion.IDENTITY)
    }

    @Test
    fun `from euler angles q0`() {
        val q0 = Quaternion.fromEuler(0f, 0f, 0f)

        assertEquals(0f, q0.x)
        assertEquals(0f, q0.y)
        assertEquals(0f, q0.z)
        assertEquals(1f, q0.w)
    }

    @Test
    fun `from euler angles qx`() {
        val qx = Quaternion.fromEulerRad(HALF_PI / 2f, 0f, 0f)

        assertEquals(0.38268346f, qx.x)
        assertEquals(0f, qx.y)
        assertEquals(0f, qx.z)
        assertEquals(0.9238795325112867f, qx.w)
    }

    @Test
    fun `from euler angles qy`() {
        val qy = Quaternion.fromEulerRad(0f, HALF_PI / 2f, 0f)

        assertEquals(0f, qy.x)
        assertEquals(0.38268346f, qy.y)
        assertEquals(0f, qy.z)
        assertEquals(0.9238795325112867f, qy.w)
    }

    @Test
    fun `from euler angles qz`() {
        val qz = Quaternion.fromEulerRad(0f, 0f, HALF_PI / 2f)

        assertEquals(0f, qz.x)
        assertEquals(0f, qz.y)
        assertEquals(0.38268346f, qz.z)
        assertEquals(0.9238795325112867f, qz.w)
    }

    @Test
    fun `from euler angles compound`() {
        val qx = Quaternion.fromEuler(30f, 0f, 0f)
        val qy = Quaternion.fromEuler(0f, 30f, 0f)
        val qz = Quaternion.fromEuler(0f, 0f, 45f)

        val qCompound = Quaternion.fromEuler(30f, 30f, 45f)

        assertEquals(0.32664073f, qCompound.x)
        assertEquals(0.13529901f, qCompound.y)
        assertEquals(0.4189367f, qCompound.z)
        assertEquals(0.83635634f, qCompound.w)
        assertEquals(qx * qy * qz, qCompound)
    }

    @Test
    fun angle() {
        val q1 = Quaternion.fromEuler(45f, 0f, 0f)
        val q2 = Quaternion.fromEuler(45.1f, 0f, 0f)
        val q3 = Quaternion.fromEuler(0f, 0f, 0f)

        assertTrue(abs(toRadians(45f) - (q1 angle q3)) < 0.01f)
        assertTrue(abs(toRadians(45f) - (q3 angle q1)) < 0.01f)

        assertTrue(abs((q1 angle q1)) < 0.01f)
        assertTrue(abs((q1 angle q2)) < 0.01f)
        assertTrue(abs((q2 angle q1)) < 0.01f)
    }

    @Test
    fun slerp() {
        // todo:
    }

}