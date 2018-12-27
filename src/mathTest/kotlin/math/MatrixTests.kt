package math

import kotlin.math.abs
import kotlin.math.atan
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MatrixTests {

    private fun matApproxEqual(expected: Matrix4f, actual: Matrix4f): Boolean {
        val result = abs(expected.m11 - actual.m11) < 0.001f &&
                abs(expected.m12 - actual.m12) < 0.001f &&
                abs(expected.m13 - actual.m13) < 0.001f &&
                abs(expected.m14 - actual.m14) < 0.001f &&

                abs(expected.m21 - actual.m21) < 0.001f &&
                abs(expected.m22 - actual.m22) < 0.001f &&
                abs(expected.m23 - actual.m23) < 0.001f &&
                abs(expected.m24 - actual.m24) < 0.001f &&

                abs(expected.m31 - actual.m31) < 0.001f &&
                abs(expected.m32 - actual.m32) < 0.001f &&
                abs(expected.m33 - actual.m33) < 0.001f &&
                abs(expected.m34 - actual.m34) < 0.001f &&

                abs(expected.m41 - actual.m41) < 0.001f &&
                abs(expected.m42 - actual.m42) < 0.001f &&
                abs(expected.m43 - actual.m43) < 0.001f &&
                abs(expected.m44 - actual.m44) < 0.001f
        if (!result) {
            println("expected=$expected")
            println("actual=$actual")
        }

        return result
    }

    @Test
    fun transposition() {
        val a = Matrix4f(
            5f, 2f, 6f, 1f,
            0f, 6f, 2f, 0f,
            3f, 8f, 1f, 4f,
            1f, 8f, 5f, 6f
        )
        val b = Matrix4f(
            5f, 0f, 3f, 1f,
            2f, 6f, 8f, 8f,
            6f, 2f, 1f, 5f,
            1f, 0f, 4f, 6f
        )

        assertTrue { a == b.transposed() }
    }

    @Test
    fun `multiplication of two matrices`() {
        val a = Matrix4f(
            5f, 2f, 6f, 1f,
            0f, 6f, 2f, 0f,
            3f, 8f, 1f, 4f,
            1f, 8f, 5f, 6f
        )
        val b = Matrix4f(
            7f, 5f, 8f, 0f,
            1f, 8f, 2f, 6f,
            9f, 4f, 3f, 8f,
            5f, 3f, 7f, 9f
        )
        val c = Matrix4f(
            96f, 68f, 69f, 69f,
            24f, 56f, 18f, 52f,
            58f, 95f, 71f, 92f,
            90f, 107f, 81f, 142f
        )

        assertTrue { a * b == c }
    }

    @Test
    fun `multiplication with identity`() {
        val a = Matrix4f(
            5f, 2f, 6f, 1f,
            0f, 6f, 2f, 0f,
            3f, 8f, 1f, 4f,
            1f, 8f, 5f, 6f
        )
        val b = Matrix4f.IDENTITY

        assertTrue { a * b == a }
    }

    @Test
    fun scale() {
        val a = Matrix4f.createScale(Vector3f(3f, 0f, 5f))
        val b = Matrix4f.IDENTITY
        val c = Matrix4f(
            m11 = 3f, m22 = 0f, m33 = 5f, m44 = 1f
        )

        assertTrue { c == a * b }
    }

    @Test
    fun `translate point`() {
        val a = Matrix4f.createTranslation(Vector3f(3f, 0f, 5f))
        val b = Vector3f(8f, -4f, -2f)
        val c = Vector3f(11f, -4f, 3f)

        assertTrue { c == a * b }
    }

    @Test
    fun `scale vector`() {
        val a = Matrix4f.createScale(Vector3f(3f, 0f, 5f))
        val b = Vector3f(8f, -4f, -2f)
        val c = Vector3f(8f * 3f, -4f * 0f, -2f * 5f)
        val d = a vecTimes b

        assertTrue { c.x - d.x < 0.01f }
        assertTrue { c.y - d.y < 0.01f }
        assertTrue { c.z - d.z < 0.01f }
    }

    @Test
    fun determinant() {
        assertEquals(
            (-5904) / 5f, Matrix4f(
                4f, 2f, 8f, 5f,
                1f, 5f, 2f, -1f,
                4f, 2f, -5f, 4f,
                -(7 / 5f), 2 / 5f, 1 / 2f, 3f
            ).determinant()
        )
    }

    @Test
    fun inverse() {
        assertTrue(
            matApproxEqual(
                Matrix4f(
                    121 / 1476f, -271 / 5904f, 113 / 1312f, -175 / 656f,
                    -59 / 1476f, 1169 / 5904f, 33 / 1312f, 65 / 656f,
                    55 / 738f, 11 / 2952f, -53 / 656f, -5 / 328f,
                    23 / 738f, -143 / 2952f, 33 / 656f, 65 / 328f
                ), Matrix4f(
                    4f, 2f, 8f, 5f,
                    1f, 5f, 2f, -1f,
                    4f, 2f, -5f, 4f,
                    -(7 / 5f), 2 / 5f, 1 / 2f, 3f
                ).inverse()
            )
        )
    }

    @Test
    fun `to float array`() {
        assertTrue(
            floatArrayOf(1f, 2f, 3f, 4f, 5f, 6f, 7f, 8f, 9f, 10f, 11f, 12f, 13f, 14f, 15f, 16f).contentEquals(
                Matrix4f(
                    1f, 2f, 3f, 4f,
                    5f, 6f, 7f, 8f,
                    9f, 10f, 11f, 12f,
                    13f, 14f, 15f, 16f
                ).toFloatArray()
            )
        )
    }

    @Test
    fun `to orientation float array`() {
        assertTrue(
            floatArrayOf(1f, 2f, 3f, 5f, 6f, 7f, 9f, 10f, 11f).contentEquals(
                Matrix4f(
                    1f, 2f, 3f, 4f,
                    5f, 6f, 7f, 8f,
                    9f, 10f, 11f, 12f,
                    13f, 14f, 15f, 16f
                ).toOrientationFloatArray()
            )
        )
    }

    @Test
    fun directions() {
        val m = Matrix4f()

        assertEquals(Vector3f.UNIT_Y, m.up)
        assertEquals(-Vector3f.UNIT_Y, m.down)
        assertEquals(Vector3f.UNIT_X, m.right)
        assertEquals(-Vector3f.UNIT_X, m.left)
        assertEquals(Vector3f.UNIT_Z, m.forward)
        assertEquals(-Vector3f.UNIT_Z, m.backward)

        // todo: non-basic
    }

    @Test
    fun `create rotation from quaternion`() {

    }

    @Test
    fun `create look at`() {
        val m = Matrix4f.createLookTowards(Vector3f.ZERO, Vector3f.UNIT_X, Vector3f.UNIT_Y)
        assertEquals(Vector3f.UNIT_X, m.forward)

        val m1 = Matrix4f.createLookAt(Vector3f.ONE, Vector3f(10f, 1f, 1f), Vector3f.UNIT_Y)
        assertEquals(Vector3f.UNIT_X, m1.forward)

        val m2 = Matrix4f.createLookAt(Vector3f.ONE, Vector3f.ZERO, Vector3f.UNIT_Y)
        assertEquals(Vector3f(-1f, -1f, -1f).normalized(), m2.forward)
    }

    @Test
    fun `create perspective`() {
        assertTrue(
            matApproxEqual(
                Matrix4f(
                    1f, 0f, 0f, 0f,
                    0f, 1f, 0f, 0f,
                    0f, 0f, -1f, 0f,
                    0f, 0f, -1f, 0f
                ), Matrix4f.createPerspective(atan(1f) * 2f, 1f, 0f, 1f)
            )
        )
    }

    @Test
    fun `create perspective widefov`() {
        assertTrue(
            matApproxEqual(
                Matrix4f(
                    .5f, 0f, 0f, 0f,
                    0f, .5f, 0f, 0f,
                    0f, 0f, -1f, 0f,
                    0f, 0f, -1f, 0f
                ), Matrix4f.createPerspective(atan(2f) * 2f, 1f, 0f, 1f)
            )
        )
    }

    @Test
    fun `create perspective narrowfov`() {
        assertTrue(
            matApproxEqual(
                Matrix4f(
                    10f, 0f, 0f, 0f,
                    0f, 10f, 0f, 0f,
                    0f, 0f, -1f, 0f,
                    0f, 0f, -1f, 0f
                ), Matrix4f.createPerspective(atan(.1f) * 2f, 1f, 0f, 1f)
            )
        )
    }

    @Test
    fun `create perspective 2:1 aspect ratio`() {
        assertTrue(
            matApproxEqual(
                Matrix4f(
                    2f, 0f, 0f, 0f,
                    0f, 1f, 0f, 0f,
                    0f, 0f, -1f, 0f,
                    0f, 0f, -1f, 0f
                ), Matrix4f.createPerspective(atan(1f) * 2f, .5f, 0f, 1f)
            )
        )
    }

    @Test
    fun `create perspective deeper view frustrum`() {
        assertTrue(
            matApproxEqual(
                Matrix4f(
                    2f, 0f, 0f, 0f,
                    0f, 1f, 0f, 0f,
                    0f, 0f, -0.5f, 2f,
                    0f, 0f, -1f, 0f
                ), Matrix4f.createPerspective(atan(1f) * 2f, 1f, -2f, 2f)
            )
        )
    }

    @Test
    fun `create orthographic`() {
        assertTrue(
            matApproxEqual(
                Matrix4f(
                    1f, 0f, 0f, -1f,
                    0f, 1f, 0f, -1f,
                    0f, 0f, 1f, 1f,
                    0f, 0f, 0f, 1f
                ), Matrix4f.createOrthographic(0f, 2f, 0f, 2f, 2f, 0f)
            )
        )
    }
}