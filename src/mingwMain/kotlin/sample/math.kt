//@file:Suppress("NOTHING_TO_INLINE")

package sample

import kotlin.math.abs
import kotlin.math.sqrt
import kotlin.native.internal.GC

/* scalar functions */
inline fun clamp(min: Double, max: Double, value: Double) = if (value > max) max else if (value < min) min else value

inline fun clamp(min: Float, max: Float, value: Float) = if (value > max) max else if (value < min) min else value
inline fun clamp(min: Int, max: Int, value: Int) = if (value > max) max else if (value < min) min else value
inline fun clamp(min: Long, max: Long, value: Long) = if (value > max) max else if (value < min) min else value

inline fun saturate(value: Double) = clamp(0.0, 1.0, value)
inline fun saturate(value: Float) = clamp(0f, 1f, value)
inline fun saturate(value: Int) = clamp(0, 1, value)
inline fun saturate(value: Long) = clamp(0, 1, value)

inline fun lerp(min: Double, max: Double, f: Double) = min * (1.0f - f) + max * f
inline fun lerp(min: Float, max: Float, f: Float) = min * (1.0f - f) + max * f

inline fun pow2(f: Float) = f * f
inline fun pow3(f: Float) = f * f * f

/* vector classes */
data class Vector2f(val x: Float = 0f, val y: Float = 0f)

data class Vector3f(val x: Float = 0f, val y: Float = 0f, val z: Float = 0f) {
    constructor(xyz: Float) : this(xyz, xyz, xyz)

    inline operator fun plus(rhs: Vector3f) = Vector3f(x + rhs.x, y + rhs.y, z + rhs.z)
    inline operator fun minus(rhs: Vector3f) = Vector3f(x - rhs.x, y - rhs.y, z - rhs.z)

    inline operator fun plus(rhs: Float) = Vector3f(x + rhs, y + rhs, z + rhs)
    inline operator fun minus(rhs: Float) = Vector3f(x - rhs, y - rhs, z - rhs)
    inline operator fun times(rhs: Float) = Vector3f(x * rhs, y * rhs, z * rhs)
    inline operator fun div(rhs: Float) = Vector3f(x / rhs, y / rhs, z / rhs)

    inline operator fun unaryMinus() = Vector3f(-x, -y, -z)

    companion object {
        val UNIT_X = Vector3f(1f, 0f, 0f)
        val UNIT_Y = Vector3f(0f, 1f, 0f)
        val UNIT_Z = Vector3f(0f, 0f, 1f)
        val ZERO = Vector3f(0f)
        val ONE = Vector3f(1f)
    }
}

/* vector functions */

inline fun Vector3f.abs() = Vector3f(abs(x), abs(y), abs(z))
inline fun Vector3f.lengthSquared() = pow2(x) + pow2(y) + pow2(z)
inline fun Vector3f.length() = sqrt(lengthSquared())
inline fun Vector3f.normalized() = this * (1 / length())
inline infix fun Vector3f.dot(rhs: Vector3f) = x * rhs.x + y * rhs.y + z * rhs.z
inline infix fun Vector3f.cross(rhs: Vector3f): Vector3f {
    return Vector3f(y * rhs.z - z * rhs.y, z * rhs.x - x * rhs.z, x * rhs.y - y * rhs.x)
}

inline fun distance(a: Vector3f, b: Vector3f) = (a - b).length()
inline fun distanceSquared(a: Vector3f, b: Vector3f) = (a - b).lengthSquared()

inline fun clamp(min: Vector3f, max: Vector3f, value: Vector3f) = Vector3f(
    clamp(min.x, max.x, value.x),
    clamp(min.y, max.y, value.y),
    clamp(min.z, max.z, value.z)
)


/* quaternion classes */

/* quaternion functions */

/* matrix classes */

data class Matrix4f(
    val m11: Float = 1f, val m12: Float = 0f, val m13: Float = 0f, val m14: Float = 0f,
    val m21: Float = 0f, val m22: Float = 1f, val m23: Float = 0f, val m24: Float = 0f,
    val m31: Float = 0f, val m32: Float = 0f, val m33: Float = 1f, val m34: Float = 0f,
    val m41: Float = 0f, val m42: Float = 0f, val m43: Float = 0f, val m44: Float = 1f
) {

    fun toFloatArray() = floatArrayOf(
        m11, m12, m13, m14,
        m21, m22, m23, m24,
        m31, m32, m33, m34,
        m41, m42, m43, m44
    )

    companion object {

        val IDENTITY = Matrix4f()

        fun createScale(scale: Vector3f) = Matrix4f(m11 = scale.x, m22 = scale.y, m33 = scale.z)
        fun createRotation(scale: Vector3f) {

        }

        fun createTranslation(scale: Vector3f) {

        }

        fun createTransform(scale: Vector3f, rotation: Vector3f, translation: Vector3f) {

        }

        fun createCamera() {

        }

        fun createPerspective() {

        }

        fun createOrthographic() {
            
        }
    }
}

/* matrix functions */
inline fun Matrix4f.transposed(): Matrix4f {
    return Matrix4f(
        m11, m21, m31, m41,
        m12, m22, m32, m42,
        m13, m23, m33, m43,
        m14, m24, m34, m44
    )
}


/* color classes */
typealias Color3f = Vector3f

inline val Color3f.r
    get() = x
inline val Color3f.g
    get() = y
inline val Color3f.b
    get() = z