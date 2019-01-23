package imgconv

import bf.*
import io.*
import kotlinx.cinterop.*
import platform.posix.exit
import stb.*
import utils.ActualOptions
import utils.Logger
import utils.Options
import utils.Timer
import kotlin.math.round
import kotlin.native.concurrent.Future
import kotlin.native.concurrent.TransferMode
import kotlin.native.concurrent.Worker
import kotlin.native.concurrent.waitForMultipleFutures

fun determineRequiredChannels(opts: ActualOptions): Int {
    return when {
        opts.isOptionPresent("a") -> 4
        opts.isOptionPresent("g") -> 2
        opts.isOptionPresent("r") -> 1
        else -> 3
    }
}

inline fun round2(f: Float) = round(f * 100f) / 100f

fun humanFileSize(bytes: Int): String {
    return when {
        bytes < 1024 -> bytes.toString()
        bytes < 1048576 -> "${round2(bytes.toFloat() / 1024)}kb"
        else -> "${round2(bytes.toFloat() / 1048576)}mb"
    }
}

@ThreadLocal
object Timers {
    val IO_LOAD = Timer()
    val DXT = Timer()
    val LZ4 = Timer()
    val IO_SAVE = Timer()
}

/* Extracts 4 * 4 block of pixels to specified buffer from specified buffer. */
fun extractBlock(
    from: CArrayPointer<UByteVar>, to: CArrayPointer<UByteVar>,
    blockX: Int, blockY: Int, width: Int, requiredChannels: Int
) {
    // [R0 G0 B0   R1 G1 B1   R2 G2 B2   R3 G3 B3]
    // [R4 G4 B4   R5 G5 B5   R6 G6 B6   R7 G7 B7]
    // ...

    val sourceRowLength = width * requiredChannels
    val sourceFirstByteInBlock = 4 * blockY * sourceRowLength + 4 * blockX * requiredChannels

    var dstIdx = 0
    repeat(4) { rowIdx ->
        val rowFirstByte = sourceFirstByteInBlock + (rowIdx * sourceRowLength)

        repeat(4) { colIdx ->
            to[dstIdx++] = from[rowFirstByte + requiredChannels * colIdx + 0] // r
            to[dstIdx++] = from[rowFirstByte + requiredChannels * colIdx + 1] // g
            to[dstIdx++] = from[rowFirstByte + requiredChannels * colIdx + 2] // b
            if (requiredChannels == 4) {
                to[dstIdx++] = from[rowFirstByte + requiredChannels * colIdx + 3] // a
            } else {
                to[dstIdx++] = 255.toUByte() // alpha is ignored
            }
        }
    }
}

fun main(args: Array<String>) {
    val appName = "imgconv.exe"
    val log = Logger("imgconv")
    val options = Options()
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
        .optionalValue("dxt-threads", "The number of threads to use when DXT encoding")

    val opts = options.parse(args)
    if (opts.shouldDisplayHelp()) {
        opts.displayHelp(appName)
        exit(1)
    }

    val inputFile = Path(opts.getValue("input-file"))
    val outputFile = Path(opts.getOptionalValue("output-file", inputFile.withExtension("bf").path))

    var bfFlags = BfImageFlags.create()
    val requiredChannels = determineRequiredChannels(opts)
    memScoped {

        Timers.IO_LOAD.begin()
        if (!opts.isOptionPresent("not-vertical-flip")) {
            stbi_set_flip_vertically_on_load(1)
            bfFlags = bfFlags.with(BF_IMAGE_FLAG_VERTICAL_FLIP)
        }

        val width = alloc<IntVar>()
        val height = alloc<IntVar>()
        val channelsInFile = alloc<IntVar>()
        var pixels = stbi_load(inputFile.path, width.ptr, height.ptr, channelsInFile.ptr, requiredChannels)

        if (requiredChannels != channelsInFile.value) log.info("Taking only $requiredChannels from file with ${channelsInFile.value} channels")
        var pixelsSize = width.value * height.value * requiredChannels

        if (pixels == null) {
            log.error("Cannot read input file!")
            return exit(2)
        }
        Timers.IO_LOAD.end()

        log.info("input ${width.value}x${height.value}")
        log.info("input ${channelsInFile.value} channels in file")

        if (opts.isOptionPresent("use-dxt")) {
            Timers.DXT.begin()
            val dxtHq = opts.isOptionPresent("use-dxt-hq")
            val dxtDither = opts.isOptionPresent("use-dxt-dither")

            bfFlags = bfFlags.with(BF_IMAGE_FLAG_DXT)

            /* structure passed to worker as argument */
            data class DXTCompressionArg(
                val blockYBase: Int,
                val width: Int,
                val input: CArrayPointer<UByteVar>,
                val output: CArrayPointer<UByteVar>,
                val tempMemory: CArrayPointer<UByteVar>,
                val requiredChannels: Int,
                val stbFlags: Int
            )

            val workerCount = opts.getOptionalValue("dxt-threads", "8").toInt() /* detect num of processors */
            val workers = Array(workerCount) { Worker.start() }

            val compressedSize = pixelsSize / 6 // 4 (dxt5) or 6 (dxt1)
            val compressed = allocArray<UByteVar>(compressedSize)

            val blockSize = 64 // 4 width * 4 height * 4 rgba

            /* thread local memory for threads */
            val workerBlocks =
                Array(workerCount) { allocArray<UByteVar>(blockSize) } /* must be RGBA 4 bytes per pixel for stb */

            val stbFlags = 0 or
                    (if (dxtHq) STB_DXT_HIGHQUAL else 0) or
                    (if (dxtDither) STB_DXT_DITHER else 0)

            log.info("dxt $workerCount threads")
            val futures: MutableSet<Future<Unit>> = mutableSetOf()
            var destPointer = compressed
            repeat(height.value / (4 * 32)) { y ->
                val workerId = (y) % workerCount
                futures.add(workers[workerId].execute(TransferMode.SAFE, {
                    DXTCompressionArg(
                        y * 32,
                        width.value,
                        pixels!!.reinterpret(),
                        destPointer,
                        workerBlocks[workerId],
                        requiredChannels,
                        stbFlags
                    )
                }) {
                    var dest = it.output

                    repeat(32) { blockYOffset ->
                        repeat(it.width / 4) { blockX ->
                            extractBlock(
                                it.input,
                                it.tempMemory,
                                blockX,
                                it.blockYBase + blockYOffset,
                                it.width,
                                it.requiredChannels
                            )
                            stb_compress_dxt_block(dest, it.tempMemory, it.requiredChannels - 3, it.stbFlags)
                            dest = (dest.toLong() + 8L).toCPointer()!!
                        }
                    }
                })
                destPointer = (destPointer.toLong() + (8L * 32 * width.value / 4)).toCPointer()!!
            }

            /* wait for all blocks to be processed */
            var done = 0
            while (done < futures.size) {
                val ready = waitForMultipleFutures(futures,250)
                ready.forEach {
                    it.consume {
                        done += 1
                    }
                }
            }

            /* transparently switch buffers */
            stbi_image_free(pixels)
            pixels = compressed
            pixelsSize /= 6

            Timers.DXT.end()
        }

        /* create header */
        val header = BfImageHeader(
            BfHeader(fileType = BfFileType.IMAGE),
            if (opts.isOptionPresent("use-lz4")) bfFlags.with(BF_IMAGE_FLAG_LZ4) else bfFlags,
            BfImageExtra.create(requiredChannels, 1), // currently only one mipmap
            width.value.toUShort(),
            height.value.toUShort()
        )

        val fileBufferSize = BfImageHeader.SIZE_BYTES + LZ4MaxCompressedSize(computeBfImagePayloadSize(header))
        val fileBuffer = ByteBuffer.create(fileBufferSize.toLong())
        fileBuffer.writeBfImageHeader(header)

        /* write compressed or non-compressed pixels */
        if (opts.isOptionPresent("use-lz4")) {
            Timers.LZ4.begin()
            if (opts.isOptionPresent("use-lz4-high")) {
                fileBuffer.writeLZ4HCCompressed(pixels, pixelsSize)
            } else {
                fileBuffer.writeLZ4Compressed(pixels, pixelsSize)
            }
            Timers.LZ4.end()
        } else {
            fileBuffer.writeBytes(pixels, pixelsSize)
        }

        /* write buffer to disk */
        Timers.IO_SAVE.begin()
        writeBinaryFile(outputFile.path, fileBuffer.slice(0, fileBuffer.pos))
        Timers.IO_SAVE.end()

        /* free the buffer */
        fileBuffer.free()

        /* print statistics */
        val sizeRaw = header.width * header.height * requiredChannels.toUInt()
        val sizeCompressed = fileBuffer.pos.toUInt()
        val ratio = sizeCompressed.toInt().toFloat() / sizeRaw.toInt().toFloat()
        log.info(
            "size raw=${humanFileSize(sizeRaw.toInt())} " +
                    "compressed=${humanFileSize(sizeCompressed.toInt())} " +
                    "ratio=${round(10000f * ratio) / 100f}%"
        )
        log.info("time load=${Timers.IO_LOAD.total}ms")
        log.info("time dxt=${Timers.DXT.total}ms")
        log.info("time lz4=${Timers.LZ4.total}ms")
        log.info("time save=${Timers.IO_SAVE.total}ms")
    }
}