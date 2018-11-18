package math

import kotlin.test.Test
import kotlin.test.assertTrue

class MatrixTests {
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
}