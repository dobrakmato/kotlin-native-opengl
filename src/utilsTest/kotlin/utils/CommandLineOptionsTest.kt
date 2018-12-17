package utils

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

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
    fun create() {
        val actual = opts.parse(arrayOf("-"))
        assertFalse(actual.shouldDisplayHelp())
        assertFalse(actual.isOptionPresent("h"))
        assertFalse(actual.isOptionPresent("l"))
        assertFalse(actual.isOptionPresent("use-lz4"))
    }
}