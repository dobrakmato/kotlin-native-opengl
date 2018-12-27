package utils

import kotlin.test.*

class CommandLineOptionsTest {

    private val opts = Options()
        .option('l', "use-lz4", "Enable LZ4 compression")
        .option('h', "use-lz4-high", "Use highest LZ4 compression level (slow)")
        .option('d', "use-dxt", "Use DXT compression")
        .option('q', "use-dxt-hq", "Use DXT HQ compression (40% slower)")
        .option('t', "use-dxt-dither", "Use DXT Dithering (do not use on normal textures)")
        .option('f', "floating-point", "Encode as floating-point texture")
        .option('v', "not-vertical-flip", "Do not flip texture vertically on load")
        .option('6', "16bit", "Treat file as 16bit texture")
        .option('r', "one-channel", "Set channels to R")
        .option('g', "two-channel", "Set channels to RG")
        .option('a', "four-channel", "Set channels to RGBA")
        .option('s', "srgb", "Use gamma for RGB channels (SRGB)")
        .requiredValue("input-file", "File to read as input file")
        .optionalValue("output-file", "Output file to write data to")

    @Test
    fun `display help`() {
        opts.displayHelp("test.exe")
    }

    @Test
    fun `empty args`() {
        val actual = opts.parse(arrayOf("-"))
        assertTrue(actual.shouldDisplayHelp())
        assertFalse(actual.hasAllRequiredValues())
        assertFalse(actual.isValuePresent("input-file"))
        assertFalse(actual.isOptionPresent("h"))
        assertFalse(actual.isOptionPresent("l"))
        assertFalse(actual.isOptionPresent("use-lz4"))
    }

    @Test
    fun `single opt arg`() {
        val actual = opts.parse(arrayOf("-lhdfs"))
        assertTrue(actual.shouldDisplayHelp())
        assertFalse(actual.hasAllRequiredValues())
        assertTrue(actual.isOptionPresent("l"))
        assertTrue(actual.isOptionPresent("h"))
        assertTrue(actual.isOptionPresent("use-dxt"))
        assertTrue(actual.isOptionPresent("f"))
        assertTrue(actual.isOptionPresent("srgb"))
    }

    @Test
    fun `multiple opt args`() {
        val actual = opts.parse(arrayOf("-l", "-h", "-df", "-s", "-"))
        assertTrue(actual.shouldDisplayHelp())
        assertFalse(actual.hasAllRequiredValues())
        assertTrue(actual.isOptionPresent("l"))
        assertTrue(actual.isOptionPresent("h"))
        assertTrue(actual.isOptionPresent("use-dxt"))
        assertTrue(actual.isOptionPresent("f"))
        assertTrue(actual.isOptionPresent("srgb"))
    }

    @Test
    fun `opt and values`() {
        val actual = opts.parse(arrayOf("-lhdfs", "--input-file=test"))
        assertTrue(actual.hasAllRequiredValues())
        assertTrue(opts.hasOption("h"))
        assertTrue(actual.isOptionPresent("h"))
        assertFalse(actual.isOptionPresent("help"))
        assertFalse(actual.shouldDisplayHelp())

        assertTrue(actual.isOptionPresent("l"))
        assertTrue(actual.isOptionPresent("h"))
        assertTrue(actual.isOptionPresent("use-dxt"))
        assertTrue(actual.isOptionPresent("f"))
        assertTrue(actual.isOptionPresent("srgb"))
        assertTrue(actual.isValuePresent("input-file"))
        assertEquals("test", actual.getValue("input-file"))
        assertEquals("testo", actual.getOptionalValue("output-file", "testo"))
        assertFails {
            actual.getValue("output-file")
        }
    }

    @Test
    fun `opt and more values`() {
        val actual = opts.parse(arrayOf("-l6dfs", "--input-file=test", "--test-value=123", "--output-file=kotlin"))
        assertTrue(actual.hasAllRequiredValues())
        assertFalse(actual.shouldDisplayHelp())

        assertTrue(actual.isOptionPresent("l"))
        assertTrue(actual.isOptionPresent("16bit"))
        assertTrue(actual.isOptionPresent("use-dxt"))
        assertTrue(actual.isOptionPresent("f"))
        assertTrue(actual.isOptionPresent("srgb"))
        assertTrue(actual.isValuePresent("input-file"))
        assertEquals("test", actual.getValue("input-file"))
        assertEquals("kotlin", actual.getValue("output-file"))
        assertEquals("kotlin", actual.getOptionalValue("output-file", ""))
    }
}