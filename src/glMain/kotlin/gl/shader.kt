package gl

import galogen.*
import kotlinx.cinterop.*


enum class ShaderType(val glType: Int) {
    VERTEX(GL_VERTEX_SHADER),
    FRAGMENT(GL_FRAGMENT_SHADER),
    COMPUTE(GL_COMPUTE_SHADER)
}

class VertexShader : Shader(ShaderType.VERTEX)
class FragmentShader : Shader(ShaderType.FRAGMENT)
class ComputeShader : Shader(ShaderType.COMPUTE)

sealed class Shader(private val type: ShaderType, override val id: UInt = glCreateShader(type.glType.toUInt())) :
    Labelled, Disposable {
    override var label: String? by Label(GL_SHADER)

    fun uploadSource(source: String) = uploadSources(arrayOf(source))

    fun uploadSources(sources: Array<String>) {
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
