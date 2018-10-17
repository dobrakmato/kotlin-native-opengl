package sample

import galogen.*
import glfw.*
import kotlinx.cinterop.*

@ExperimentalUnsignedTypes
fun main(args: Array<String>) {

    if (glfwInit() != GLFW_TRUE) {
        throw Exception("Failed to initialize GLFW")
    }

    glfwSetErrorCallback(staticCFunction { _, description: CPointer<ByteVar>? ->
        println("GLFW Error: " + description?.toKString())
    })

    glfwWindowHint(GLFW_RESIZABLE, GL_FALSE)
    glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 4)
    glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 2)
    glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE)

    val window = glfwCreateWindow(1024, 768, "Title", null, null)

    if (window == null) {
        glfwTerminate()
        throw Exception("Failed to open GLFW window. If you have an Intel GPU, they are not 3.3 compatible.")
    }

    glfwMakeContextCurrent(window)

    println("OpenGL Version: " + glGetString(GL_VERSION)?.reinterpret<ByteVar>()?.toKString())
    println("OpenGL Version: " + glGetString(GL_VENDOR)?.reinterpret<ByteVar>()?.toKString())
    println("OpenGL Version: " + glGetString(GL_RENDERER)?.reinterpret<ByteVar>()?.toKString())

    glClearColor(0.0f, 0.0f, 0.0f, 0.0f)

    var id = glCreateShader(GL_VERTEX_SHADER)
    var id2 = glCreateShader(GL_VERTEX_SHADER.toUInt())
    var id3 = glCreateShader(GL_VERTEX_SHADER.toUInt())

    println("Created shader ID: $id")
    println("Created shader ID: $id2")
    println("Created shader ID: $id3")

    do {
        glClear(GL_COLOR_BUFFER_BIT)

        glfwSwapBuffers(window)
        glfwPollEvents()
    } while (glfwGetKey(window, GLFW_KEY_ESCAPE) != GLFW_PRESS)

    glfwTerminate()

}