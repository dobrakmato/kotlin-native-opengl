package utils

import kotlin.test.Test
import kotlin.test.assertEquals

class UtilsTests {

    @Test
    fun `string times operator`() {
        assertEquals("****", "*" * 4)
        assertEquals("KOTGINKOTGIN", "KOTGIN" * 2)
        assertEquals("", "" * 10)
    }

}