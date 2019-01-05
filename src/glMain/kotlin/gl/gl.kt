package gl

import galogen.*
import kotlinx.cinterop.CArrayPointer
import kotlinx.cinterop.FloatVar
import kotlinx.cinterop.cValuesOf
import kotlinx.cinterop.memScoped
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

class Label(private val openGLObjectType: Int) {

    private var value: String? = null

    operator fun getValue(labelled: Labelled, property: KProperty<*>): String? {
        return value
    }

    operator fun setValue(labelled: Labelled, property: KProperty<*>, s: String?) {
        value = s
        glObjectLabel(openGLObjectType.toUInt(), labelled.id.toUInt(), s?.length ?: 0, s)
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
    .float("uv1", 2)
    .float("tangent", 3)
    .float("color", 3)

class VAO(override val id: UInt = GLObjects.newVAO()) : Labelled, Disposable {
    override var label: String? by Label(GL_VERTEX_ARRAY)

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
