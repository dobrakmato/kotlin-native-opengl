package sample

import galogen.*
import glfw.*
import kotlinx.cinterop.*
import platform.posix.usleep
import kotlin.math.round
import kotlin.system.getTimeMillis
import kotlin.system.getTimeNanos

@ExperimentalUnsignedTypes
fun checkGLError() {
    val error = glGetError().toInt()
    if (error != GL_NO_ERROR) {
        throw Exception("Gl error! $error")
    }
}

@ExperimentalUnsignedTypes
fun main(args: Array<String>) {
    val start = getTimeMillis()

    if (glfwInit() != GLFW_TRUE) {
        throw Exception("Failed to initialize GLFW")
    }

    glfwSetErrorCallback(staticCFunction { _, description: CPointer<ByteVar>? ->
        println("GLFW Error: " + description?.toKString())
    })

    // glfwWindowHint( GLFW_DOUBLEBUFFER, GL_FALSE )
    glfwWindowHint(GLFW_RESIZABLE, GL_TRUE)
    glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 4)
    glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 5)
    glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE)

    val window = glfwCreateWindow(1024, 768, "Title", null, null)

    if (window == null) {
        glfwTerminate()
        throw Exception("Failed to open GLFW window. If you have an Intel GPU, they are not 3.3 compatible.")
    }

    println("Window created in ${getTimeMillis() - start} ms")

    glfwMakeContextCurrent(window)
    glfwSetWindowSizeCallback(window, staticCFunction { _, width, height ->
        println("[Window] new size $width*$height")
    })
    glfwSetJoystickCallback(staticCFunction { joy, event ->
        when (event) {
            GLFW_CONNECTED -> println("[Joystick] Connected #$joy")
            GLFW_DISCONNECTED -> println("[Joystick] Disconnected #$joy")
        }
    })

    println("OpenGL Version: " + glGetString(GL_VERSION)?.reinterpret<ByteVar>()?.toKString())
    println("OpenGL Vendor: " + glGetString(GL_VENDOR)?.reinterpret<ByteVar>()?.toKString())
    println("OpenGL Renderer: " + glGetString(GL_RENDERER)?.reinterpret<ByteVar>()?.toKString())
    println("GLSL Version: " + glGetString(GL_SHADING_LANGUAGE_VERSION)?.reinterpret<ByteVar>()?.toKString())
    println("=" * 80)

    glEnableDebug()

    glfwSwapInterval(0)

    // ---- buffers ----

    val vbo = BufferObject.createArrayBuffer(
        GL_MAP_READ_BIT,
        cValuesOf(
            -1f, -1f, 0f,
            1f, -1f, 0f,
            1f, 1f, 0f,
            -1f, 1f, 0f
        )
    )

    val ibo = BufferObject.createIndexBuffer(GL_MAP_READ_BIT, cValuesOf(3.toByte(), 0, 1, 2, 3, 1))

    // ---- shaders ----

    val vertex = Shader(ShaderType.VERTEX)
    vertex.uploadSource(arrayOf(readFileText("C:\\Users\\Matej\\IdeaProjects\\kotgin\\src\\mingwMain\\resources\\shaders\\basic.vert")))
    vertex.compile()

    val fragment = Shader(ShaderType.FRAGMENT)
    fragment.uploadSource(arrayOf(readFileText("C:\\Users\\Matej\\IdeaProjects\\kotgin\\src\\mingwMain\\resources\\shaders\\basic.frag")))
    fragment.compile()

    val program = Program()
    program.attachShader(vertex)
    program.attachShader(fragment)
    program.link()

    // ---- vao ----

    val vao = VAO()
    vao.enableAttribute(0)
    vao.defineBinding(0, vbo, 0, 3 * Float.BYTES.toInt())
    vao.defineAttribute(0, 3, GL_FLOAT)
    vao.useBindingAsAttribute(0, 0)
    vao.elementBuffer(ibo)

    // ---- texture ----

    val texture = AssetLoader
        .getAsset<Texture>("C:\\Users\\Matej\\IdeaProjects\\kotgin\\src\\mingwMain\\resources\\sprites\\kogin.bf")

    println("textures ready")

    // ---- render loop ----

    glDebugMessage("Entering render loop")
    glClearColor(0f, 0f, 0f, 1f)

    println("Started application in ${getTimeMillis() - start} ms.")

    vao.bind()
    program.use()
    texture.bindTo(0)

    val frameTime = Query(QueryType.TIME_ELAPSED)

    val f = GL_COLOR_BUFFER_BIT or GL_DEPTH_BUFFER_BIT
    var gpuTime = 0.0
    var lastTime = 0.0
    var frames: Long = 0
    do {
        gpuTime += (frameTime.getResult().toInt() / 1000000f)
        frameTime.begin()
        glClear(f.toUInt())


        glDrawElements(GL_TRIANGLES, 6, GL_UNSIGNED_BYTE, null)


        glfwSwapBuffers(window)
        frameTime.end()
        glfwPollEvents()
        frames++

        val time = glfwGetTime()
        if (lastTime + 1 < time) {
            glfwSetWindowTitle(window, "FPS: $frames (" + round(1000 * gpuTime / frames) / 1000f + "ms)")
            lastTime = time
            gpuTime = 0.0
            frames = 0
        }

        val error = glGetError().toInt()
        if (error != GL_NO_ERROR) {
            throw Exception("Gl error! $error")
        }

/*
        val present = glfwJoystickPresent(GLFW_JOYSTICK_1);
        if (present > 0) {
            memScoped {
                val count = alloc<IntVar>()
                val axes = glfwGetJoystickAxes(GLFW_JOYSTICK_1, count.ptr)

                var i = 0
                if (count.value > 0) {
                    val value = (axes + i)!!.pointed
                    println("axis=$i value=${value.value}")
                    i += 1
                }
            }
        }*/
    } while (glfwGetKey(window, GLFW_KEY_ESCAPE) != GLFW_PRESS)

    glfwTerminate()

}