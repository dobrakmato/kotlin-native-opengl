package gl

import galogen.*
import kotlinx.cinterop.*


enum class QueryType(val glType: Int) {
    TIMESTAMP(GL_TIMESTAMP),
    TIME_ELAPSED(GL_TIME_ELAPSED),
    SAMPLES_PASSED(GL_SAMPLES_PASSED)
}

class Query(private val type: QueryType, override val id: UInt = GLObjects.newQuery(type)) : Labelled,
    Disposable {
    override var label: String? by Label(GL_QUERY)

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
            done.value == GL_TRUE
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
            longVar.value
        }
    }

    fun getResult(): ULong {
        return memScoped {
            val longVar = alloc<ULongVar>()
            glGetQueryObjectui64v(id, GL_QUERY_RESULT, longVar.ptr)
            longVar.value
        }
    }

    override fun free() {
        memScoped {
            glDeleteQueries(1, cValuesOf(id))
        }
    }
}
