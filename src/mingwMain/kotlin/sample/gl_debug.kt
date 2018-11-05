package sample

import galogen.*
import kotlinx.cinterop.*

class KHRStackTrace : RuntimeException()

fun translateSource(source: Int): String {
    return when (source) {
        GL_DEBUG_SOURCE_API -> "API"
        GL_DEBUG_SOURCE_WINDOW_SYSTEM -> "Window System"
        GL_DEBUG_SOURCE_SHADER_COMPILER -> "Shader Compiler"
        GL_DEBUG_SOURCE_THIRD_PARTY -> "Third-party"
        GL_DEBUG_SOURCE_APPLICATION -> "Application"
        GL_DEBUG_SOURCE_OTHER -> "Other"
        else -> "Unknown"
    }
}

fun translateType(type: Int): String {
    return when (type) {
        GL_DEBUG_TYPE_ERROR -> "Error"
        GL_DEBUG_TYPE_DEPRECATED_BEHAVIOR -> "Deprecated behavior"
        GL_DEBUG_TYPE_UNDEFINED_BEHAVIOR -> "Undefined behavior"
        GL_DEBUG_TYPE_PORTABILITY -> "Portability"
        GL_DEBUG_TYPE_PERFORMANCE -> "Performance"
        GL_DEBUG_TYPE_OTHER -> "Other"
        GL_DEBUG_TYPE_MARKER -> "Marker"
        else -> "Unknown"
    }
}

fun translateSeverity(severity: Int): String {
    return when (severity) {
        GL_DEBUG_SEVERITY_HIGH -> "High"
        GL_DEBUG_SEVERITY_MEDIUM -> "Medium"
        GL_DEBUG_SEVERITY_LOW -> "Low"
        GL_DEBUG_SEVERITY_NOTIFICATION -> "Notification"
        else -> "Unknown"
    }
}

private fun debugCallback(
    source: GLenum,
    type: GLenum,
    id: GLuint,
    severity: GLenum,
    length: GLsizei,
    message: CPointer<ByteVar>?,
    userParam: COpaquePointer?
) {
    printErr(
        "[KHR_debug] ${translateType(type.toInt())} (${translateSeverity(severity.toInt())}) " +
                "${translateSource(source.toInt())}/$id: ${message?.toKString()?.substring(0, length)}"
    )
    if (severity.toInt() == GL_DEBUG_SEVERITY_HIGH) {
        KHRStackTrace().printStackTrace()
    }
}

fun glEnableDebug(synchronous: Boolean = true) {
    glEnable(GL_DEBUG_OUTPUT)
    if (synchronous) {
        glEnable(GL_DEBUG_OUTPUT_SYNCHRONOUS)
    }
    glDebugMessageCallback(staticCFunction(::debugCallback), null)
    glDebugMessage("KHR Debug callback enabled.")
}

fun glDebugMessage(
    message: String,
    source: Int = GL_DEBUG_SOURCE_APPLICATION,
    type: Int = GL_DEBUG_TYPE_OTHER,
    severity: Int = GL_DEBUG_SEVERITY_NOTIFICATION,
    id: Int = 0
) {
    glDebugMessageInsert(source.toUInt(), type.toUInt(), id.toUInt(), severity.toUInt(), message.length, message)
}