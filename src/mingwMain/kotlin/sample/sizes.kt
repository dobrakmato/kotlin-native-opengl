package sample

inline val Byte.Companion.BYTES: Long
    get() = 1L

inline val Short.Companion.BYTES: Long
    get() = 2L

inline val Int.Companion.BYTES: Long
    get() = 4L

inline val Long.Companion.BYTES: Long
    get() = 8L

inline val Float.Companion.BYTES: Long
    get() = 4L

inline val Double.Companion.BYTES: Long
    get() = 8L