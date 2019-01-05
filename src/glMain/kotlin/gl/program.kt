package gl

import galogen.*
import kotlinx.cinterop.*


class Program(override val id: UInt = glCreateProgram()) : Labelled, Disposable {
    override var label: String? by Label(GL_PROGRAM)
    private val uniformLocations = HashMap<String, Int>()

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
                    val log = allocArray<ByteVar>(8192)
                    val length = alloc<IntVar>()
                    glGetProgramInfoLog(id, 8192, length.ptr, log)

                    throw RuntimeException("Program was not linked! ${log.toKString().substring(length.value)}")
                }
            }
        }
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
        glProgramUniform1f(id, uniformLocations[name]!!, value)
    }

    fun setUniform(name: String, value: Int) {
        ensureUniformLocationExists(name)
        glProgramUniform1i(id, uniformLocations[name]!!, value)
    }

    fun setUniform(name: String, value: UInt) {
        ensureUniformLocationExists(name)
        glProgramUniform1ui(id, uniformLocations[name]!!, value)
    }

    fun setUniform(name: String, f0: Float, f1: Float) {
        ensureUniformLocationExists(name)
        glProgramUniform2f(id, uniformLocations[name]!!, f0, f1)
    }

    fun setUniform(name: String, f0: Float, f1: Float, f2: Float) {
        ensureUniformLocationExists(name)
        glProgramUniform3f(id, uniformLocations[name]!!, f0, f1, f2)
    }

    fun setUniform(name: String, f0: Float, f1: Float, f2: Float, f3: Float) {
        ensureUniformLocationExists(name)
        glProgramUniform4f(id, uniformLocations[name]!!, f0, f1, f2, f3)
    }

    fun setUniform(name: String, transpose: Boolean, matrix: CValuesRef<FloatVar>) {
        ensureUniformLocationExists(name)
        val trans: UByte = if (transpose) 1u else 0u
        glProgramUniformMatrix4fv(id, uniformLocations[name]!!, 1, trans, matrix)
    }

    fun setUniformBlock() {
        //todo: research
        // https://www.khronos.org/opengl/wiki/Uniform_Buffer_Object
    }

    override fun free() {
        glDeleteProgram(id)
    }

    companion object {
        var checkLinkageStatus = true
    }
}