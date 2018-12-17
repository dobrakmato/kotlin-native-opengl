package bf

/* general constants */
const val BF_MAGIC = 1766222146
const val BF_EXTENSION = "bf"
const val BF_VERSION: Byte = 1

/* file types */
enum class BfFileType(id: Int) {
    IMAGE(1),
    GEOMETRY(2),
    AUDIO(3),
    MATERIAL(4),
    FILESYSTEM(5),
    COMPILED_SHADER(6),
    SCENE(7),
}


