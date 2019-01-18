package bfinfo

import bf.*
import io.ByteBuffer
import io.Path
import io.readBinaryFile
import platform.posix.exit
import utils.Logger
import utils.Options
import kotlin.math.round

inline fun round2(f: Float) = round(f * 100f) / 100f

fun humanFileSize(bytes: Long): String {
    return when {
        bytes < 1024 -> bytes.toString()
        bytes < 1048576 -> "${round2(bytes.toFloat() / 1024)}KiB"
        else -> "${round2(bytes.toFloat() / 1048576)}MiB"
    }
}

fun main(args: Array<String>) {
    val log = Logger("bfinfo")
    val options = Options()
        .option('g', "gui", "Open GUI")

    if (args.isEmpty()) {
        log.error("No file provided as argument!")
        exit(1)
    }

    val inputFile = args[0]
    val buffer = readBinaryFile(inputFile)
    val genericHeader = buffer.readBfHeader()

    /* generic information */
    log.info("magic ${genericHeader.magic}")
    log.info("version ${genericHeader.version}")
    log.info("file type ${genericHeader.fileType}")
    log.info("file size ${buffer.size} ${humanFileSize(buffer.size)}")

    /* rewind buffer */
    buffer.pos = 0

    when (genericHeader.fileType) {
        BfFileType.IMAGE -> infoImage(options, buffer, log, Path(inputFile))
        BfFileType.GEOMETRY -> infoGeometry(options, buffer, log)
        BfFileType.AUDIO -> TODO()
        BfFileType.MATERIAL -> TODO()
        BfFileType.FILESYSTEM -> TODO()
        BfFileType.COMPILED_SHADER -> TODO()
        BfFileType.SCENE -> TODO()
    }
}

/* BfImage information */
fun infoImage(options: Options, buffer: ByteBuffer, log: Logger, inputFile: Path) {
    val header = buffer.readBfImageHeader()

    log.info("width ${header.width}")
    log.info("height ${header.height}")
    log.info("channels ${header.extra.numberOfChannels()}")
    log.info("mipmaps ${header.extra.includedMipmaps()}")
    log.info("flag lz4 ${header.flags.lz4()}")
    log.info("flag dxt ${header.flags.dxt()}")
    log.info("flag float ${header.flags.float()}")
    log.info("flag 16bit ${header.flags.is16bit()}")
    log.info("flag vflip ${header.flags.verticallyFlipped()}")
    log.info("flag srgb ${header.flags.srgb()}")
    log.info("flag skybox ${header.flags.skybox()}")

    buffer.pos = 0

    ImgView(
        buffer,
        "bfinfo [${inputFile.filename}] ${header.width}x${header.height} ${header.extra.numberOfChannels()}ch " +
                if (header.flags.lz4()) "LZ4 " else "" + if (header.flags.dxt()) "DXT " else ""

    ).run()
}

/* BfGeometry information */
fun infoGeometry(options: Options, buffer: ByteBuffer, log: Logger) {
    val header = buffer.readBfGeometryHeader()


}
