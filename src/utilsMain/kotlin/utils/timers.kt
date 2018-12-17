package utils

import kotlin.system.getTimeMillis

/* simple timer */
class Timer {
    private var start: Long = 0

    /**
     * Milliseconds
     */
    var total: Long = 0
        private set
    var laps: Long = 0
        private set

    fun begin() {
        start = getTimeMillis()
        laps++
    }

    fun end(): Long {
        val took = getTimeMillis() - start
        total += took
        return took
    }

    /**
     * Milliseconds
     */
    inline fun avg() = total.toFloat() / laps
}