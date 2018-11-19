//@file:Suppress("NOTHING_TO_INLINE")

package math

import kotlin.math.*
import kotlin.random.Random


/* For more information about mathematics: https://www.euclideanspace.com/maths/discrete */

/*
 *  R   U   F
 * (x) (y) (z)
 * s_x m12 m13 t_x
 * m21 s_y m23 t_y
 * m31 m32 s_z t_z
 * m41 m42 m43 m44
 *
 *
 *         ^  +Y (UP)
 *         |
 *         |    +Z (FORWARD)
 *         |  /
 *         | /
 *         + --------->   +X (RIGHT)
 *
 */
/* scalar constants */
const val PI = 3.1415926536f
const val HALF_PI = PI * 0.5f
const val TWO_PI = PI * 2.0f
const val FOUR_PI = PI * 4.0f
const val INV_PI = 1.0f / PI
const val INV_TWO_PI = INV_PI * 0.5f
const val INV_FOUR_PI = INV_PI * 0.25f

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

inline fun toDegrees(rad: Float) = rad * (180.0f * INV_PI)
inline fun toRadians(deg: Float) = deg * (PI / 180.0f)

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

        inline val RANDOM
            get() = Vector3f(Random.nextFloat(), Random.nextFloat(), Random.nextFloat())
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

inline fun lerp(min: Vector3f, max: Vector3f, f: Float) = min * (1.0f - f) + max * f
inline fun slerp(min: Vector3f, max: Vector3f, f: Float): Vector3f {
    val dot = clamp(-1f, 1f, min dot max)
    val theta = acos(dot) * f
    val relative = (max - (min * dot)).normalized()
    return min * (cos(theta)) + (relative * sin(theta))
}

inline fun nlerp(min: Vector3f, max: Vector3f, f: Float) = (min * (1.0f - f) + max * f).normalized()

/* quaternion classes */

data class Quaternion(val x: Float = 0f, val y: Float = 0f, val z: Float = 0f, val w: Float = 1f) {

    inline operator fun plus(rhs: Quaternion) = Quaternion(x + rhs.x, y + rhs.y, z + rhs.z, w + rhs.w)
    inline operator fun minus(rhs: Quaternion) = Quaternion(x - rhs.x, y - rhs.y, z - rhs.z, w + rhs.w)

    inline operator fun times(rhs: Float) = Quaternion(x * rhs, y * rhs, z * rhs, w * rhs)
    inline operator fun times(rhs: Quaternion) = Quaternion(
        w * rhs.w - x * rhs.x - y * rhs.y - z * rhs.z,
        w * rhs.x + x * rhs.w + y * rhs.z - z * rhs.y,
        w * rhs.y + y * rhs.w + z * rhs.x - x * rhs.z,
        w * rhs.z + z * rhs.w + x * rhs.y - y * rhs.x
    )

    companion object {
        val IDENTITY = Quaternion(0f, 0f, 0f, 1f)

        fun fromEuler(pitch: Float, yaw: Float, roll: Float): Quaternion {
            val cy = cos(yaw * 0.5f)
            val sy = sin(yaw * 0.5f)
            val cr = cos(roll * 0.5f)
            val sr = sin(roll * 0.5f)
            val cp = cos(pitch * 0.5f)
            val sp = sin(pitch * 0.5f)

            return Quaternion(
                cy * sr * cp - sy * cr * sp,
                cy * cr * sp + sy * sr * cp,
                sy * cr * cp - cy * sr * sp,
                cy * cr * cp + sy * sr * sp
            )
        }
    }
}

/* quaternion functions */
inline fun Quaternion.conjugate() = Quaternion(-x, -y, -z, w)

inline fun Quaternion.lengthSquared() = pow2(x) + pow2(y) + pow2(z) + pow2(w)
inline fun Quaternion.length() = sqrt(lengthSquared())
inline fun Quaternion.normalized() = this * (1 / length())
inline infix fun Quaternion.dot(rhs: Quaternion) = x * rhs.x + y * rhs.y + z * rhs.z + w * rhs.w

/* matrix classes */

data class Matrix4f(
    val m11: Float = 1f, val m12: Float = 0f, val m13: Float = 0f, val m14: Float = 0f,
    val m21: Float = 0f, val m22: Float = 1f, val m23: Float = 0f, val m24: Float = 0f,
    val m31: Float = 0f, val m32: Float = 0f, val m33: Float = 1f, val m34: Float = 0f,
    val m41: Float = 0f, val m42: Float = 0f, val m43: Float = 0f, val m44: Float = 1f
) {

    inline val right
        get() = Vector3f(m11, m21, m31)

    inline val left
        get() = Vector3f(-m11, -m21, -m31)

    inline val up
        get() = Vector3f(m12, m22, m32)

    inline val down
        get() = Vector3f(-m12, -m22, -m32)

    inline val forward
        get() = Vector3f(m13, m23, m33)

    inline val backward
        get() = Vector3f(-m13, -m23, -m33)


    inline operator fun times(rhs: Matrix4f): Matrix4f {
        return Matrix4f(
            m11 * rhs.m11 + m12 * rhs.m21 + m13 * rhs.m31 + m14 * rhs.m41,
            m11 * rhs.m12 + m12 * rhs.m22 + m13 * rhs.m32 + m14 * rhs.m42,
            m11 * rhs.m13 + m12 * rhs.m23 + m13 * rhs.m33 + m14 * rhs.m43,
            m11 * rhs.m14 + m12 * rhs.m24 + m13 * rhs.m34 + m14 * rhs.m44,

            m21 * rhs.m11 + m22 * rhs.m21 + m23 * rhs.m31 + m24 * rhs.m41,
            m21 * rhs.m12 + m22 * rhs.m22 + m23 * rhs.m32 + m24 * rhs.m42,
            m21 * rhs.m13 + m22 * rhs.m23 + m23 * rhs.m33 + m24 * rhs.m43,
            m21 * rhs.m14 + m22 * rhs.m24 + m23 * rhs.m34 + m24 * rhs.m44,

            m31 * rhs.m11 + m32 * rhs.m21 + m33 * rhs.m31 + m34 * rhs.m41,
            m31 * rhs.m12 + m32 * rhs.m22 + m33 * rhs.m32 + m34 * rhs.m42,
            m31 * rhs.m13 + m32 * rhs.m23 + m33 * rhs.m33 + m34 * rhs.m43,
            m31 * rhs.m14 + m32 * rhs.m24 + m33 * rhs.m34 + m34 * rhs.m44,

            m41 * rhs.m11 + m42 * rhs.m21 + m43 * rhs.m31 + m44 * rhs.m41,
            m41 * rhs.m12 + m42 * rhs.m22 + m43 * rhs.m32 + m44 * rhs.m42,
            m41 * rhs.m13 + m42 * rhs.m23 + m43 * rhs.m33 + m44 * rhs.m43,
            m41 * rhs.m14 + m42 * rhs.m24 + m43 * rhs.m34 + m44 * rhs.m44
        )
    }

    inline operator fun times(pointRhs: Vector3f): Vector3f {
        var x = pointRhs.x * m11 + pointRhs.y * m12 + pointRhs.z * m13 + m14
        var y = pointRhs.x * m21 + pointRhs.y * m22 + pointRhs.z * m23 + m24
        var z = pointRhs.x * m31 + pointRhs.y * m32 + pointRhs.z * m33 + m34
        val w = pointRhs.x * m41 + pointRhs.y * m41 + pointRhs.z * m43 + m44

        val wInv = 1 / w

        x *= wInv
        y *= wInv
        z *= wInv

        return Vector3f(x, y, z)
    }

    inline infix fun vecTimes(vectorRhs: Vector3f): Vector3f {
        val x = vectorRhs.x * m11 + vectorRhs.y * m12 + vectorRhs.z * m13
        val y = vectorRhs.x * m21 + vectorRhs.y * m22 + vectorRhs.z * m23
        val z = vectorRhs.x * m31 + vectorRhs.y * m32 + vectorRhs.z * m33

        return Vector3f(x, y, z)
    }

    companion object {

        val IDENTITY = Matrix4f(m11 = 1f, m22 = 1f, m33 = 1f, m44 = 1f)

        fun createScale(scale: Vector3f) = Matrix4f(m11 = scale.x, m22 = scale.y, m33 = scale.z)
        fun createTranslation(translation: Vector3f) =
            Matrix4f(m14 = translation.x, m24 = translation.y, m34 = translation.z)

        fun createRotationFromQuaternion(rotation: Quaternion): Matrix4f {
            val normalized = rotation.normalized()
            val x = normalized.x
            val y = normalized.y
            val z = normalized.z
            val w = normalized.w
            return Matrix4f(
                1 - 2 * y * y - 2 * z * z,
                2 * x * y - 2 * w * z,
                2 * x * z - 2 * w * y,
                0f,
                2 * x * y - 2 * w * z,
                1 - 2 * x * x - 2 * z * z,
                2 * y * z + 2 * x * w,
                0f,
                2 * x * z - 2 * w * y,
                2 * y * z - 2 * w * x,
                1 - 2 * x * x - y * y,
                0f,
                0f, 0f, 0f, 1f
            )
        }

        fun createLookAt(eye: Vector3f, target: Vector3f, up: Vector3f): Matrix4f {
            return createLookTowards(eye, target - eye, up)
        }

        fun createLookTowards(eye: Vector3f, forward: Vector3f, up: Vector3f): Matrix4f {
            val f = forward.normalized()
            val r = (f cross up).normalized()
            val u = (r cross f).normalized()
            return Matrix4f(
                r.x, u.x, f.x, eye.x,
                r.y, u.y, f.y, eye.y,
                r.z, u.z, f.z, eye.z,
                0f, 0f, 0f, 1f
            )
        }

        fun createPerspective(verticalFov: Float, ratio: Float, near: Float, far: Float): Matrix4f {
            val tanFov2 = tan(verticalFov / 2f)
            return Matrix4f(
                m11 = 1f / ratio * tanFov2,
                m22 = 1 / tanFov2,
                m33 = -(far + near) / (far - near),
                m34 = -(2f * far * near) / (far - near),
                m43 = -1f,
                m44 = 0f
            )
        }

        fun createOrthographic(left: Float, right: Float, bottom: Float, top: Float, near: Float, far: Float) =
            Matrix4f(
                m11 = 2f / (right - left), m14 = -(right + left) / (right - left),
                m22 = 2f / (top - bottom), m24 = -(top + bottom) / (top - bottom),
                m33 = -2f / (far - near), m34 = -(far + near) / (far - near)
            )
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

/**
 * To use in OpenGL we you need to set transposed to GL_TRUE.
 */
inline fun Matrix4f.toFloatArray() = floatArrayOf(
    m11, m12, m13, m14,
    m21, m22, m23, m24,
    m31, m32, m33, m34,
    m41, m42, m43, m44
)

inline fun Matrix4f.determinant(): Float {
    return m14 * m23 * m32 * m41 - m13 * m24 * m32 * m41 - m14 * m22 * m33 * m41 + m12 * m24 * m33 * m41 +
            m13 * m22 * m34 * m41 - m12 * m23 * m34 * m41 - m14 * m23 * m31 * m42 + m13 * m24 * m31 * m42 +
            m14 * m21 * m33 * m42 - m11 * m24 * m33 * m42 - m13 * m21 * m34 * m42 + m11 * m23 * m34 * m42 +
            m14 * m22 * m31 * m43 - m12 * m24 * m31 * m43 - m14 * m21 * m32 * m43 + m11 * m24 * m32 * m43 +
            m12 * m21 * m34 * m43 - m11 * m22 * m34 * m43 - m13 * m22 * m31 * m44 + m12 * m23 * m31 * m44 +
            m13 * m21 * m32 * m44 - m11 * m23 * m32 * m44 - m12 * m21 * m33 * m44 + m11 * m22 * m33 * m44
}

inline fun Matrix4f.inverse(): Matrix4f {
    val scale = 1 / determinant()
    return Matrix4f(
        (m23 * m34 * m42 - m24 * m33 * m42 + m24 * m32 * m43 - m22 * m34 * m43 - m23 * m32 * m44 + m22 * m33 * m44) * scale,
        (m14 * m33 * m42 - m13 * m34 * m42 - m14 * m32 * m43 + m12 * m34 * m43 + m13 * m32 * m44 - m12 * m33 * m44) * scale,
        (m13 * m24 * m42 - m14 * m23 * m42 + m14 * m22 * m43 - m12 * m24 * m43 - m13 * m22 * m44 + m12 * m23 * m44) * scale,
        (m14 * m23 * m32 - m13 * m24 * m32 - m14 * m22 * m33 + m12 * m24 * m33 + m13 * m22 * m34 - m12 * m23 * m34) * scale,
        (m24 * m33 * m41 - m23 * m34 * m41 - m24 * m31 * m43 + m21 * m34 * m43 + m23 * m31 * m44 - m21 * m33 * m44) * scale,
        (m13 * m34 * m41 - m14 * m33 * m41 + m14 * m31 * m43 - m11 * m34 * m43 - m13 * m31 * m44 + m11 * m33 * m44) * scale,
        (m14 * m23 * m41 - m13 * m24 * m41 - m14 * m21 * m43 + m11 * m24 * m43 + m13 * m21 * m44 - m11 * m23 * m44) * scale,
        (m13 * m24 * m31 - m14 * m23 * m31 + m14 * m21 * m33 - m11 * m24 * m33 - m13 * m21 * m34 + m11 * m23 * m34) * scale,
        (m22 * m34 * m41 - m24 * m32 * m41 + m24 * m31 * m42 - m21 * m34 * m42 - m22 * m31 * m44 + m21 * m32 * m44) * scale,
        (m14 * m32 * m41 - m12 * m34 * m41 - m14 * m31 * m42 + m11 * m34 * m42 + m12 * m31 * m44 - m11 * m32 * m44) * scale,
        (m12 * m24 * m41 - m14 * m22 * m41 + m14 * m21 * m42 - m11 * m24 * m42 - m12 * m21 * m44 + m11 * m22 * m44) * scale,
        (m14 * m22 * m31 - m12 * m24 * m31 - m14 * m21 * m32 + m11 * m24 * m32 + m12 * m21 * m34 - m11 * m22 * m34) * scale,
        (m23 * m32 * m41 - m22 * m33 * m41 - m23 * m31 * m42 + m21 * m33 * m42 + m22 * m31 * m43 - m21 * m32 * m43) * scale,
        (m12 * m33 * m41 - m13 * m32 * m41 + m13 * m31 * m42 - m11 * m33 * m42 - m12 * m31 * m43 + m11 * m32 * m43) * scale,
        (m13 * m22 * m41 - m12 * m23 * m41 - m13 * m21 * m42 + m11 * m23 * m42 + m12 * m21 * m43 - m11 * m22 * m43) * scale,
        (m12 * m23 * m31 - m13 * m22 * m31 + m13 * m21 * m32 - m11 * m23 * m32 - m12 * m21 * m33 + m11 * m22 * m33) * scale
    )
}

/* geometric shape classes */

data class Ray(val origin: Vector3f, val direction: Vector3f) {
    // todo: intersections
}

inline fun Ray.pointAt(t: Float) = origin + direction * t


/* color classes */
typealias Color3f = Vector3f

inline val Color3f.r
    get() = x
inline val Color3f.g
    get() = y
inline val Color3f.b
    get() = z