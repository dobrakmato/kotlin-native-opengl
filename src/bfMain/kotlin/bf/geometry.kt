package bf

import io.ByteBuffer

typealias BfGeometryFlag = UByte

enum class BfGeometryListType(val id: UByte) {
    BF_GEOMETRY_LIST_POSITIONS(1u), // float3
    BF_GEOMETRY_LIST_COLORS(2u), // float3
    BF_GEOMETRY_LIST_NORMALS(3u), // float3
    BF_GEOMETRY_LIST_TANGENTS(4u), // float3
    BF_GEOMETRY_LIST_UV1(5u), // float2
    BF_GEOMETRY_LIST_UV2(6u), // float2
    BF_GEOMETRY_LIST_UV3(7u), // float2
    BF_GEOMETRY_LIST_UV4(8u), // float2
    BF_GEOMETRY_LIST_UV5(9u), // float2
    BF_GEOMETRY_LIST_UV6(10u), // float2
    BF_GEOMETRY_LIST_INDICES(11u), // short / int
}

/* geometry flags */
const val BF_GEOMETRY_FLAG_LZ4: BfGeometryFlag = 1u
const val BF_GEOMETRY_FLAG_UNUSED: BfGeometryFlag = 2u
const val BF_GEOMETRY_FLAG_LONG_INDICES: BfGeometryFlag = 4u
const val BF_GEOMETRY_FLAG_HAS_BONES: BfGeometryFlag = 8u


inline class BfGeometryFlags(val value: UByte) {
    inline fun lz4() = (value and BF_GEOMETRY_FLAG_LZ4) == BF_GEOMETRY_FLAG_LZ4
    inline fun unused1() = (value and BF_GEOMETRY_FLAG_UNUSED) == BF_GEOMETRY_FLAG_UNUSED
    inline fun longIndices() = (value and BF_GEOMETRY_FLAG_LONG_INDICES) == BF_GEOMETRY_FLAG_LONG_INDICES
    inline fun hasBones() = (value and BF_GEOMETRY_FLAG_HAS_BONES) == BF_GEOMETRY_FLAG_HAS_BONES

    companion object {
        inline val SIZE_BYTES: Int
            get() {
                return 1
            }

        fun create() = BfGeometryFlags(0u)
    }
}

inline fun BfGeometryFlags.with(flag: UByte) = BfGeometryFlags(this.value or flag)


data class BfGeometryHeader(
    val header: BfHeader,
    val flags: BfGeometryFlags,
    val lodLevels: UByte = 0u,
    val uncompressedSize: UInt
) {
    companion object {
        inline val SIZE_BYTES: Int
            get() {
                return BfHeader.SIZE_BYTES + BfGeometryFlags.SIZE_BYTES + UByte.SIZE_BYTES + UInt.SIZE_BYTES
            }
    }
}


fun ByteBuffer.writeBfGeometryHeader(bfGeometryHeader: BfGeometryHeader) {
    writeBfHeader(bfGeometryHeader.header)
    writeBfGeometryFlags(bfGeometryHeader.flags)
    writeUByte(bfGeometryHeader.lodLevels)
    writeUInt(bfGeometryHeader.uncompressedSize)
}

fun ByteBuffer.readBfGeometryHeader(): BfGeometryHeader {
    val bfHeader = readBfHeader()
    val bfImageFlags = readBfGeometryFlags()
    val lodLevels = readUByte()
    val uncompressedSize = readUInt()

    return BfGeometryHeader(bfHeader, bfImageFlags, lodLevels, uncompressedSize)
}

/* extend ByteBuffer to allow storing image data structures */
fun ByteBuffer.writeBfGeometryFlags(flags: BfGeometryFlags) = writeUByte(flags.value)

fun ByteBuffer.readBfGeometryFlags(): BfGeometryFlags = BfGeometryFlags(readUByte())

fun ByteBuffer.writeBfGeometryListType(bfGeometryListType: BfGeometryListType) = writeUByte(bfGeometryListType.id)

fun ByteBuffer.readBfGeometryListType() = when (readUByte().toUInt()) {
    1u -> BfGeometryListType.BF_GEOMETRY_LIST_POSITIONS
    2u -> BfGeometryListType.BF_GEOMETRY_LIST_COLORS
    3u -> BfGeometryListType.BF_GEOMETRY_LIST_NORMALS
    4u -> BfGeometryListType.BF_GEOMETRY_LIST_TANGENTS
    5u -> BfGeometryListType.BF_GEOMETRY_LIST_UV1
    6u -> BfGeometryListType.BF_GEOMETRY_LIST_UV2
    7u -> BfGeometryListType.BF_GEOMETRY_LIST_UV3
    8u -> BfGeometryListType.BF_GEOMETRY_LIST_UV4
    9u -> BfGeometryListType.BF_GEOMETRY_LIST_UV5
    10u -> BfGeometryListType.BF_GEOMETRY_LIST_UV6
    11u -> BfGeometryListType.BF_GEOMETRY_LIST_INDICES
    else -> throw IllegalStateException("Invalid BfGeometryListType id!")
}
