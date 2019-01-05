package gl

import galogen.*
import kotlinx.cinterop.*
import kotlin.reflect.KFunction2
import kotlin.reflect.KFunction3

@ThreadLocal
object GLObjects {
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
    protected val pool: CArrayPointer<UIntVar> = nativeHeap.allocArray(batchSize)
) : Disposable {

    protected var currentIndex = batchSize - 1

    fun take(): UInt {
        if (currentIndex == batchSize - 1) {
            refill()
        }

        return pool[currentIndex++]
    }

    protected open fun refill() {
        createFn(batchSize, pool)
        currentIndex = 0
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
        currentIndex = 0
    }
}
