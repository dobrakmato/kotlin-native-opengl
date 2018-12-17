package utils

import platform.posix.usleep
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TimersTests {

    @Test
    fun `simple timer`() {
        val timer = Timer()
        assertEquals(0, timer.total)
        assertEquals(0, timer.laps)
        timer.begin()
        usleep(250000)
        timer.end()
        assertEquals(1, timer.laps)
        assertTrue(timer.total > 20)
        timer.begin()
        timer.end()
        assertEquals(2, timer.laps)
        assertEquals(timer.total.toFloat() / timer.laps, timer.avg())
    }

}