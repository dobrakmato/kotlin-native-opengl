package bf

import io.ByteBuffer

/* general constants */
const val BF_MAGIC = 1766222146
const val BF_VERSION: UByte = 2u

data class BfHeader(
    val magic: Int = BF_MAGIC,
    val version: UByte = BF_VERSION,
    val fileType: BfFileType
) {
    companion object {
        val SIZE_BYTES: Int
            get() {
                return 4 + 1 + 1
            }
    }
}

fun ByteBuffer.writeBfHeader(bfHeader: BfHeader) {
    writeInt(bfHeader.magic)
    writeUByte(bfHeader.version)
    writeBfFileType(bfHeader.fileType)
}

fun ByteBuffer.readBfHeader(): BfHeader {
    val magic = readInt()
    val version = readUByte()
    val fileType = readBfFileType()

    if (magic != BF_MAGIC) throw IllegalStateException("BF_MAGIC was not found at the start of the file!")

    return BfHeader(magic, version, fileType)
}

/* file types */
enum class BfFileType(val id: UByte) {
    IMAGE(1u),
    GEOMETRY(2u),
    AUDIO(3u),
    MATERIAL(4u),
    FILESYSTEM(5u),
    COMPILED_SHADER(6u),
    SCENE(7u),
}

fun ByteBuffer.writeBfFileType(bfFileType: BfFileType) = writeUByte(bfFileType.id)

fun ByteBuffer.readBfFileType() = when (readUByte().toUInt()) {
    1u -> BfFileType.IMAGE
    2u -> BfFileType.GEOMETRY
    3u -> BfFileType.AUDIO
    4u -> BfFileType.MATERIAL
    5u -> BfFileType.FILESYSTEM
    6u -> BfFileType.COMPILED_SHADER
    7u -> BfFileType.SCENE
    else -> throw IllegalStateException("Invalid BfFileType id!")
}
