package gl

import galogen.*
import kotlinx.cinterop.CValues
import kotlinx.cinterop.CValuesRef
import kotlinx.cinterop.cValuesOf
import kotlinx.cinterop.memScoped


class BufferObject(override val id: UInt = GLObjects.newBufferObject()) : Labelled, Disposable {
    override var label: String? by Label(GL_ARRAY_BUFFER)

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
        inline fun createArrayBuffer(flags: Int, data: CValues<*>?): BufferObject =
            createArrayBuffer(data?.size?.toLong() ?: 0, flags, data)

        inline fun createArrayBuffer(size: Long, flags: Int, data: CValuesRef<*>?): BufferObject {
            val bo = BufferObject()
            bo.bindAsArrayBuffer()
            bo.createStorage(size, flags.toUInt(), data)
            return bo
        }

        inline fun createIndexBuffer(flags: Int, data: CValues<*>?): BufferObject =
            createIndexBuffer(data?.size?.toLong() ?: 0, flags, data)

        inline fun createIndexBuffer(size: Long, flags: Int, data: CValuesRef<*>?): BufferObject {
            val bo = BufferObject()
            bo.bindAsIndexBuffer()
            bo.createStorage(size, flags.toUInt(), data)
            return bo
        }
    }
}
