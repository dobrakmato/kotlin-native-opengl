package sample

import galogen.*
import gl.BufferObject
import gl.Program
import gl.Texture2D
import gl.VAO
import kotlinx.cinterop.*
import lz4.LZ4_decompress_safe
import platform.opengl32.GL_R
import kotlin.system.getTimeMillis

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


object MeshLoader : SpecializedResourceLoader<Mesh> {
    override fun loadTo(resource: Resource<Mesh>, data: FileData, dataLength: Int) {

    }
}


class Material {
    private val shader: Resource<Program> = Resources.load("shaders/deffered_pbr.csh")
    private val albedoTexture: Resource<Texture2D> = Resources.load("textures/stone01.png")
    private val normalTexture: Resource<Texture2D> = Resources.load("textures/stone01_n.png")

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