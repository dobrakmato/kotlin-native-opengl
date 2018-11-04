package sample

import bfinfo.BFImageHeader
import bfinfo.BF_HEADER_IMAGE_SIZE
import bfinfo.readBFHeader
import galogen.*
import kotlinx.cinterop.*
import lz4.LZ4_decompress_safe
import platform.opengl32.GL_R
import kotlin.system.getTimeMillis

@Target(AnnotationTarget.TYPE, AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.PROPERTY)
@MustBeDocumented
annotation class Mallocated

interface SpecializedResourceLoader<T> {
    fun loadTo(resource: Resource<T>, data: FileData, dataLength: Int)
}

object ResourceLoader {
    fun <T> load(resource: Resource<T>) {
        val (data, length) = readFileBinary(resource.file)

        if (data.integerAt(0) != 1) {
            throw Exception("Not a BF file!")
        }

        val version = data[4]
        val fileType = data[5]

        // delegate to specific loader
        when (fileType.toInt()) {
            1 -> TextureLoader.loadTo(resource as Resource<Texture>, data, length)
            2 -> MeshLoader.loadTo(resource as Resource<Mesh>, data, length)
        }

        resource.loaded = true
    }
}

object Resources {
    fun <T> load(path: String, async: Boolean = false): Resource<T> {
        val res = Resource<T>(path)
        if (async) {
            res.loadAsync()
        } else {
            res.load()
        }
        return res
    }
}

class Resource<T>(val file: String) {
    internal var resource: T? = null
    internal var loaded: Boolean = false

    val timing = ResourceTiming()

    fun load() {
        ResourceLoader.load(this)
    }

    fun loadAsync() {
        // TODO(fix me)
    }

    fun isLoaded() = loaded
}

enum class ResourceState {
    WAITING,
    IO_READING,
    DECOMPRESSING,
    UPLOADING,
    UPLOADED
}

data class ResourceTiming(
    var ioTime: Long = 0,
    var decompressTime: Long = 0,
    var uploadTime: Long = 0,
    var asyncUploadTime: Long = 0
)

val BFImageHeader.glInternalFormat: Int
    get() {
        if (flags.srgb()) {
            return when (extra.numberOfChannels()) {
                3 -> GL_SRGB8
                4 -> GL_SRGB8_ALPHA8
                else -> throw Exception("Invalid number of channels for SRGB!")
            }
        }

        // todo: support more texture formats

        return when (extra.numberOfChannels()) {
            1 -> GL_R8
            2 -> GL_RG8
            3 -> GL_RGB8
            4 -> GL_RGBA8
            else -> throw Exception("Invalid number of channels! Or not implemented yet.")
        }
    }

val BFImageHeader.glFormat: Int
    get() {
        return when (extra.numberOfChannels()) {
            1 -> GL_R
            2 -> GL_RG
            3 -> GL_RGB
            4 -> GL_RGBA
            else -> throw Exception("Invalid number of channels!")
        }
    }

val BFImageHeader.glType: Int
    get() {
        return if (flags.float()) {
            GL_FLOAT
        } else {
            GL_UNSIGNED_BYTE
        }
    }

object TextureLoader : SpecializedResourceLoader<Texture> {
    override fun loadTo(resource: Resource<Texture>, data: @Mallocated FileData, dataLength: Int) {
        val header = readBFHeader(data.reinterpret())
        val dataPointer = skipHeader(data)

        /* Either the memory is released and new buffer is created in decompressIfNeeded() or old pointer is returned. */
        val realDataPointer = decompressIfNeeded(header, dataPointer, sizeWithoutHeader(dataLength))

        val texture = Texture()
        texture.label = "Texture for ${resource.file}"

        texture.createStorage(
            header.extra.includedMipmaps(), header.glInternalFormat.toUInt(),
            header.width.toInt(), header.height.toInt()
        )

        texture.uploadMipmap(
            0, header.width.toInt(), header.height.toInt(),
            header.glFormat.toUInt(), header.glType.toUInt(), realDataPointer
        )

        /* Upload additional mipmaps. */
        texture.generateOtherMipmaps()

        texture.setFilters(TextureFilter.LINEAR, TextureFilter.LINEAR)
        texture.setWraps(TextureWrapMode.REPEAT, TextureWrapMode.REPEAT)
        texture.setAnisotropicFiltering(Texture.defaultAnisotropyLevel)

        resource.resource = texture

        nativeHeap.free(realDataPointer)
    }

    private fun decompressIfNeeded(
        header: BFImageHeader,
        @Mallocated dataPointer: CArrayPointer<UByteVar>,
        sizeWithoutHeader: Int
    ): @Mallocated CArrayPointer<UByteVar> {
        if (header.flags.lz4()) {
            val decompressed = nativeHeap.allocArray<UByteVar>(header.uncompressedSize)
            LZ4_decompress_safe(
                dataPointer.reinterpret(),
                decompressed.reinterpret(),
                sizeWithoutHeader,
                header.uncompressedSize
            )
            nativeHeap.free(dataPointer)
            return decompressed
        }
        return dataPointer
    }

    private fun sizeWithoutHeader(size: Int): Int {
        return size - BF_HEADER_IMAGE_SIZE
    }

    private fun skipHeader(contents: CPointer<UByteVar>): CPointer<UByteVar> {
        return ((contents.toLong() + BF_HEADER_IMAGE_SIZE).toCPointer())
            ?: throw RuntimeException("Null pointer exception")
    }
}

object MeshLoader : SpecializedResourceLoader<Mesh> {
    override fun loadTo(resource: Resource<Mesh>, data: FileData, dataLength: Int) {

    }
}


class Material {
    private val shader: Resource<Program> = Resources.load("shaders/deffered_pbr.csh")
    private val albedoTexture: Resource<Texture> = Resources.load("textures/stone01.png")
    private val normalTexture: Resource<Texture> = Resources.load("textures/stone01_n.png")

    fun isReady(): Boolean {
        return shader.isLoaded()
                && albedoTexture.isLoaded()
                && normalTexture.isLoaded()
    }

    fun use() {
        albedoTexture.resource?.bindTo(0)
        normalTexture.resource?.bindTo(1)
        shader.resource?.use()
        shader.resource?.setUniform("Time", (getTimeMillis() / 1000f))
    }
}

class Mesh {
    val vao: VAO = VAO()
    val vertexBuffer: BufferObject = BufferObject()
    val indexBuffer: BufferObject? = null
    internal var ready = false

    fun isReady(): Boolean = ready
    fun isIndexed() = indexBuffer != null

    fun bind() {

    }
}

interface Renderer {
    fun render()
}

class MeshRenderer : Renderer {
    val mesh: Mesh = Mesh()
    val material: Material = Material()

    override fun render() {
        if (!material.isReady() || !mesh.isReady()) {
            return
        }

        mesh.bind()
        material.use()

        if (mesh.isIndexed()) {
            renderIndexed()
        } else {
            renderArray()
        }
    }

    private fun renderIndexed() {

    }

    private fun renderArray() {

    }
}