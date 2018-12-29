package utils

import platform.posix.putenv
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class EnviromentVariablesTest {

    @Test
    fun `getenv`() {
        putenv("TEST_A=1")
        putenv("TEST_B=KOTGIN")
        assertEquals("1", utils.getenv("TEST_A"))
        assertEquals("KOTGIN", utils.getenv("TEST_B"))
        assertEquals(null, utils.getenv("TEST_C"))
        assertEquals("ONE", utils.getenv("TEST_C", "ONE"))
    }

    @Test
    fun `dot env`() {
        val dotenv = """ENV_A=128
            |ENV_B=test
            |ENV_C=kotgin
        """.trimMargin()
        loadDotEnv(dotenv.split("\n").toTypedArray())
        assertEquals("128", utils.getenv("ENV_A"))
        assertEquals("test", utils.getenv("ENV_B"))
        assertEquals("kotgin", utils.getenv("ENV_C"))
        assertEquals(null, utils.getenv("ENV_D"))
    }

    @Test
    fun `all env vars`() {
        val list = getAllEnvironmentVariables()
        assertNotNull(list)
    }
}