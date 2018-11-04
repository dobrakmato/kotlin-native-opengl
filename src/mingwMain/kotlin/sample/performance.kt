package sample

import kotlin.system.getTimeMillis

class CPUStopwatch(val name: String) {
    private var starts: Int = 0
    private var cumulativeTime: Long = 0
    private var lastStart: Long = 0

    fun begin() {
        starts++
        lastStart = getTimeMillis()
    }

    fun end() {
        cumulativeTime += (getTimeMillis() - lastStart)
    }
}

class GPUStopwatch(val name: String) {

}

object Timings {
    val IO_READ = CPUStopwatch("IO Read")
    val LZ4_DECODE = CPUStopwatch("Decompress")
}