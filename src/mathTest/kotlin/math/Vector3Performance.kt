package math

import kotlin.system.getTimeMillis
import kotlin.test.*

@Ignore // remove if you want to run performance test
class Vector3Performance {
    @Test
    fun emptyTest() {
        val start = getTimeMillis()
        repeat(10000000) {
        }
        val end = getTimeMillis()
        println("emptyTest=" + (end - start) + "ms")
    }

    @Test
    fun vectorTest() {
        val start = getTimeMillis()
        var res = 0f
        repeat(10000000) {
            val a = Vector3f(25f + it, -16f, 45.3f)
            val b = Vector3f(3.14259f, 10000f, -10f)

            val c = ((a + b) * 3f).normalized()
            res = c dot a
        }
        val end = getTimeMillis()
        println("vectorTest=" + (end - start) + "ms")
    }
}