package sample

import glew.*
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

    val err = glewInit()

    if (err.toInt() != GLEW_OK) {
        print("GLEW Error $err")
    }

    println("Status: Using GLEW " + glewGetString(GLEW_VERSION)?.reinterpret<ByteVar>()?.toKString())
    println("OpenGL Version: " + glGetString(GL_VERSION)?.reinterpret<ByteVar>()?.toKString())
    println("OpenGL Version: " + glGetString(GL_VENDOR)?.reinterpret<ByteVar>()?.toKString())
    println("OpenGL Version: " + glGetString(GL_RENDERER)?.reinterpret<ByteVar>()?.toKString())

    glClearColor(0.0f, 0.0f, 0.0f, 0.0f)

    do {
        glClear(GL_COLOR_BUFFER_BIT)

        glBegin(GL_TRIANGLES)

        glColor3f(1f, 0f, 0f)
        glVertex2f(-.5f, -.5f)
        glColor3f(0f, 1f, 0f)
        glVertex2f(.5f, -.5f)
        glColor3f(0f, 0f, 1f)
        glVertex2f(0f, .5f)

        glEnd()

        glfwSwapBuffers(window)
        glfwPollEvents()
    } while (glfwGetKey(window, GLFW_KEY_ESCAPE) != GLFW_PRESS)

    glfwTerminate()

}