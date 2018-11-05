package sample

import galogen.*
import kotlinx.cinterop.*
import platform.opengl32.GL_TEXTURE_MAX_ANISOTROPY_EXT
import kotlin.reflect.KFunction2
import kotlin.reflect.KFunction3
import kotlin.reflect.KProperty

interface Disposable {
    fun free()
}

interface Identifiable {
    val id: UInt
}

interface Labelled : Identifiable {
    var label: String?
}

class OpenGLLabel(private val openGLObjectType: Int) {

    private var value: String? = null

    operator fun getValue(labelled: Labelled, property: KProperty<*>): String? {
        return value
    }

    operator fun setValue(labelled: Labelled, property: KProperty<*>, s: String?) {
        value = s
        glObjectLabel(openGLObjectType.toUInt(), labelled.id.toUInt(), s?.length ?: 0, s)
    }

}

@ThreadLocal
object OpenGLObjectFactory {
    private val texture2d = TypedBufferedOpenGLObjectConstructor(::glCreateTextures, GL_TEXTURE_2D)
    private val bufferObjects = SimpleBufferedOpenGLObjectConstructor(::glCreateBuffers)
    private val vao = SimpleBufferedOpenGLObjectConstructor(::glCreateVertexArrays)

    private val timestampQueries = TypedBufferedOpenGLObjectConstructor(::glCreateQueries, GL_TIMESTAMP)
    private val timeElapsedQueries = TypedBufferedOpenGLObjectConstructor(::glCreateQueries, GL_TIME_ELAPSED)

    fun newTexture2D(): UInt = texture2d.take()
    fun newBufferObject(): UInt = bufferObjects.take()
    fun newVAO(): UInt = vao.take()
    fun newQuery(type: QueryType): UInt = when (type) {
        QueryType.TIMESTAMP -> timestampQueries.take()
        QueryType.TIME_ELAPSED -> timeElapsedQueries.take()
        else -> throw RuntimeException("Other query types not yet implemented!")
    }
}

typealias SimpleOpenGLCreateFunction = KFunction2<GLsizei, CValuesRef<GLuintVar>?, Unit>
typealias TypedOpenGLCreateFunction = KFunction3<GLenum, GLsizei, CValuesRef<GLuintVar>?, Unit>

open class SimpleBufferedOpenGLObjectConstructor(
    protected val createFn: SimpleOpenGLCreateFunction,
    protected var batchSize: Int = 8,
    @Mallocated protected val pool: CArrayPointer<UIntVar> = nativeHeap.allocArray(batchSize)
) : Disposable {

    protected var idx = batchSize - 1

    fun take(): UInt {
        if (idx == batchSize - 1) {
            refill()
        }

        return pool[idx++]
    }

    protected open fun refill() {
        createFn(batchSize, pool)
        idx = 0
    }

    override fun free() {
        nativeHeap.free(pool)
    }
}

class TypedBufferedOpenGLObjectConstructor(
    private val createFn3: TypedOpenGLCreateFunction,
    private val type: Int
) : SimpleBufferedOpenGLObjectConstructor(::glCreateBuffers, type) {
    override fun refill() {
        createFn3(type.toUInt(), batchSize, pool)
        idx = 0
    }
}

/* Texture objects */

enum class TextureFilter(val glFilter: Int) {
    LINEAR(GL_LINEAR),
    NEAREST(GL_NEAREST),
    LINEAR_MIPMAP_LINEAR(GL_LINEAR_MIPMAP_LINEAR),
    LINEAR_MIPMAP_NEAREST(GL_LINEAR_MIPMAP_NEAREST),
}

enum class TextureWrapMode(val glWrapMode: Int) {
    REPEAT(GL_REPEAT),
    CLAMP_TO_EDGE(GL_CLAMP_TO_EDGE),
    CLAMP_TO_BORDER(GL_CLAMP_TO_BORDER),
    MIRRORED_REPEAT(GL_MIRRORED_REPEAT),
}

class Texture(override val id: UInt = OpenGLObjectFactory.newTexture2D()) : Labelled, Disposable {
    override var label: String? by OpenGLLabel(GL_TEXTURE)

    fun bindTo(sampler: Int) {
        glBindTextureUnit(sampler.toUInt(), id)
    }

    override fun free() {
        memScoped {
            glDeleteTextures(1, cValuesOf(id))
        }
    }

    fun createStorage(mipmapLevels: Int, internalFormat: Int, width: Int, height: Int) {
        glTextureStorage2D(id, mipmapLevels, internalFormat.toUInt(), width, height)
    }

    fun uploadMipmap(
        mipmapLevel: Int,
        width: Int,
        height: Int,
        format: Int,
        type: Int,
        pixelData: CValuesRef<*>
    ) {
        glTextureSubImage2D(id, mipmapLevel, 0, 0, width, height, format.toUInt(), type.toUInt(), pixelData)
    }

    fun generateOtherMipmaps() {
        glGenerateTextureMipmap(id)
    }

    fun setFilters(minFilter: TextureFilter, magFilter: TextureFilter) {
        glTextureParameteri(id, GL_TEXTURE_MAG_FILTER, magFilter.glFilter)
        glTextureParameteri(id, GL_TEXTURE_MIN_FILTER, minFilter.glFilter)
    }

    fun setWraps(sWrapMode: TextureWrapMode, tWrapMode: TextureWrapMode) {
        glTextureParameteri(id, GL_TEXTURE_WRAP_S, sWrapMode.glWrapMode)
        glTextureParameteri(id, GL_TEXTURE_WRAP_T, tWrapMode.glWrapMode)
    }

    fun setAnisotropicFiltering(anisotropyLevel: Float) {
        glTextureParameterf(id, GL_TEXTURE_MAX_ANISOTROPY_EXT, anisotropyLevel)
    }

    companion object {
        var defaultAnisotropyLevel = 16f
    }
}

/* Buffer objects. */

class BufferObject(override val id: UInt = OpenGLObjectFactory.newBufferObject()) : Labelled, Disposable {
    override var label: String? by OpenGLLabel(GL_ARRAY_BUFFER)

    fun bind(target: GLenum) {
        glBindBuffer(target, id)
    }

    fun bindAsArrayBuffer() = bind(GL_ARRAY_BUFFER.toUInt())
    fun bindAsIndexBuffer() = bind(GL_ELEMENT_ARRAY_BUFFER.toUInt())
    fun bindAsUniformBuffer() = bind(GL_UNIFORM_BUFFER.toUInt())

    fun createStorage(size: Long, flags: GLbitfield, data: CValuesRef<*>? = null) {
        glNamedBufferStorage(id, size, data, flags)
    }

    fun uploadData(offset: Long, size: Long, data: CValuesRef<*>? = null) {
        glNamedBufferSubData(id, offset, size, data)
    }

    override fun free() {
        memScoped {
            glDeleteBuffers(1, cValuesOf(id))
        }
    }

    companion object {
        fun createArrayBuffer(flags: Int, data: CValues<*>?): BufferObject =
            createArrayBuffer(data?.size?.toLong() ?: 0, flags, data)

        fun createArrayBuffer(size: Long, flags: Int, data: CValuesRef<*>?): BufferObject {
            val bo = BufferObject()
            bo.bindAsArrayBuffer()
            bo.createStorage(size, flags.toUInt(), data)
            return bo
        }

        fun createIndexBuffer(flags: Int, data: CValues<*>?): BufferObject =
            createIndexBuffer(data?.size?.toLong() ?: 0, flags, data)

        fun createIndexBuffer(size: Long, flags: Int, data: CValuesRef<*>?): BufferObject {
            val bo = BufferObject()
            bo.bindAsIndexBuffer()
            bo.createStorage(size, flags.toUInt(), data)
            return bo
        }
    }
}

/* Query objects */

enum class QueryType(val glType: Int) {
    TIMESTAMP(GL_TIMESTAMP),
    TIME_ELAPSED(GL_TIME_ELAPSED)
}

class Query(private val type: QueryType, override val id: UInt = OpenGLObjectFactory.newQuery(type)) : Labelled,
    Disposable {
    override var label: String? by OpenGLLabel(GL_QUERY)

    fun begin() {
        glBeginQuery(type.glType.toUInt(), id)
    }

    fun end() {
        glEndQuery(type.glType.toUInt())
    }

    fun isResultReady(): Boolean {
        return memScoped {
            val done = alloc<IntVar>()
            glGetQueryObjectiv(id, GL_QUERY_RESULT_AVAILABLE, done.ptr)
            return done.value == GL_TRUE
        }
    }

    fun busyWaitResult(): ULong {
        return memScoped {
            val done = alloc<IntVar>()
            val longVar = alloc<ULongVar>()
            glGetQueryObjectiv(id, GL_QUERY_RESULT_AVAILABLE, done.ptr)
            while (done.value == GL_FALSE) {
                glGetQueryObjectiv(id, GL_QUERY_RESULT_AVAILABLE, done.ptr)
            }

            glGetQueryObjectui64v(id, GL_QUERY_RESULT, longVar.ptr)
            return longVar.value
        }
    }

    fun getResult(): ULong {
        return memScoped {
            val longVar = alloc<ULongVar>()
            glGetQueryObjectui64v(id, GL_QUERY_RESULT, longVar.ptr)
            return longVar.value
        }
    }

    override fun free() {
        memScoped {
            glDeleteQueries(1, cValuesOf(id))
        }
    }
}


/* Shader object */

enum class ShaderType(val glType: Int) {
    VERTEX(GL_VERTEX_SHADER),
    FRAGMENT(GL_FRAGMENT_SHADER),
    COMPUTE(GL_COMPUTE_SHADER)
}

class Shader(private val type: ShaderType, override val id: UInt = glCreateShader(type.glType.toUInt())) : Labelled,
    Disposable {
    override var label: String? by OpenGLLabel(GL_SHADER)

    fun uploadSource(sources: Array<String>) {
        memScoped {
            val lengths = sources.map { it.length }
            val values: CValuesRef<IntVar> = createValues(sources.size) { index -> this.value = lengths[index] }
            glShaderSource(id, sources.size, sources.toCStringArray(this), values)
        }
    }

    fun uploadCompiled(compiled: CArrayPointer<UByteVar>) {
        // Read more: https://www.khronos.org/opengl/wiki/SPIR-V
        // glShaderBinary()
        throw RuntimeException("not yet implemented!")
    }

    fun compile() {
        glCompileShader(id)
        if (Shader.checkCompilationStatus) {
            memScoped {
                val isCompiled = alloc<GLintVar>()
                glGetShaderiv(id, GL_COMPILE_STATUS, isCompiled.ptr)
                if (isCompiled.value == GL_FALSE) {
                    throw RuntimeException("Shader was not compiled!")
                }
            }
        }
    }

    override fun free() {
        glDeleteShader(id)
    }

    companion object {
        var checkCompilationStatus = true
    }
}

/* Program object */

class Program(override val id: UInt = glCreateProgram()) : Labelled, Disposable {
    override var label: String? by OpenGLLabel(GL_PROGRAM)
    private val uniformLocations = HashMap<String, Int>()
    var ready: Boolean = false
        private set

    fun attachShader(shader: Shader) {
        glAttachShader(id, shader.id)
    }

    fun detachShader(shader: Shader) {
        glDetachShader(id, shader.id)
    }

    fun link() {
        glLinkProgram(id)
        if (Program.checkLinkageStatus) {
            memScoped {
                val isLinked = alloc<GLintVar>()
                glGetProgramiv(id, GL_LINK_STATUS, isLinked.ptr)
                if (isLinked.value == GL_FALSE) {
                    val log = allocArray<ByteVar>(1024)
                    val length = alloc<IntVar>()
                    glGetProgramInfoLog(id, 1024, length.ptr, log)

                    throw RuntimeException("Program was not linked! ${log.toKString().substring(length.value)}")
                }
            }
        }
        ready = true
    }

    fun use() {
        glUseProgram(id)
    }

    private inline fun ensureUniformLocationExists(name: String) {
        if (name !in uniformLocations) {
            uniformLocations[name] = glGetUniformLocation(id, name)
        }
    }

    fun setUniform(name: String, value: Float) {
        ensureUniformLocationExists(name)
        glUniform1f(uniformLocations[name]!!, value)
    }

    fun setUniform(name: String, value: Int) {
        ensureUniformLocationExists(name)
        glUniform1i(uniformLocations[name]!!, value)
    }

    fun setUniform(name: String, value: UInt) {
        ensureUniformLocationExists(name)
        glUniform1ui(uniformLocations[name]!!, value)
    }

    fun setUniform(name: String, f0: Float, f1: Float, f2: Float) {
        ensureUniformLocationExists(name)
        glUniform3f(uniformLocations[name]!!, f0, f1, f2)
    }

    fun setUniformBlock() {
        //todo: research
    }

    override fun free() {
        glDeleteProgram(id)
    }

    companion object {
        var checkLinkageStatus = true
    }
}

/* Vertex array objects */

class VertexSpecification {
    fun interleaved(): VertexSpecification {
        return this
    }

    fun float(description: String, count: Int, padding: Int = 0): VertexSpecification {

        return this
    }
}

class VAOFactory {

    private data class SrcMapping(
        val name: Int,
        val fromBuffer: CArrayPointer<*>,
        val offset: Long,
        val size: Long,
        val stride: Long,
        val count: Long
    )

    private data class DestMapping(val name: Int, val padding: Int = 0)

    private var interleaved = false
    private val srcMappings = mutableListOf<SrcMapping>()
    private val destMappings = mutableListOf<DestMapping>()


    fun src(name: Int, fromBuffer: CArrayPointer<*>, offset: Long, size: Long, stride: Long, count: Long) {
        srcMappings.add(SrcMapping(name, fromBuffer, offset, size, stride, count))
    }

    fun destInterleaved() {
        interleaved = true
    }

    fun dest(name: Int, padding: Int = 0) {
        destMappings.add(DestMapping(name, padding))
    }

    fun build() {

    }
}

val positions: CArrayPointer<FloatVar>? = null

//val cubeVao = VAOFactory()
//    .src(0, positions, 0, Long.BYTES, Long.BYTES, 12)

val DEFAULT_VERTEX_SPEC = VertexSpecification()
    .interleaved()
    .float("position", 3)
    .float("normal", 3)
    .float("uv", 2)
    .float("tangent", 3)

class VAO(override val id: UInt = OpenGLObjectFactory.newVAO()) : Labelled, Disposable {
    override var label: String? by OpenGLLabel(GL_VERTEX_ARRAY)

    fun initializeWith(spec: VertexSpecification) {

    }

    fun bind() {
        glBindVertexArray(id)
    }

    fun enableAttribute(index: Int) {
        glEnableVertexArrayAttrib(id, index.toUInt())
    }

    fun defineBinding(bindingIndex: Int, bufferObject: BufferObject, offset: Long, stride: Int) {
        glVertexArrayVertexBuffer(id, bindingIndex.toUInt(), bufferObject.id, offset, stride)
    }

    fun defineAttribute(
        attributeIndex: Int,
        size: Int,
        type: Int,
        normalize: Int = GL_FALSE,
        relativeOffset: UInt = 0u
    ) {
        glVertexArrayAttribFormat(id, attributeIndex.toUInt(), size, type.toUInt(), normalize.toUByte(), relativeOffset)
    }

    fun useBindingAsAttribute(attributeIndex: Int, bindingIndex: Int) {
        glVertexArrayAttribBinding(id, attributeIndex.toUInt(), bindingIndex.toUInt())
    }

    fun elementBuffer(indexBuffer: BufferObject) {
        glVertexArrayElementBuffer(id, indexBuffer.id)
    }

    override fun free() {
        memScoped {
            glDeleteVertexArrays(1, cValuesOf(id))
        }
    }
}
