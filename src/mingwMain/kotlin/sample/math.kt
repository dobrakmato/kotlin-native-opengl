//@file:Suppress("NOTHING_TO_INLINE")

package sample

import kotlin.math.abs
import kotlin.math.sqrt

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

data class Matrix4f(val m00: Float, val m01: Float, val m02: Float, val m03: Float) {

}

/* matrix functions */


/* color classes */
typealias Color3f = Vector3f

inline val Color3f.r
    get() = x
inline val Color3f.g
    get() = y
inline val Color3f.b
    get() = z