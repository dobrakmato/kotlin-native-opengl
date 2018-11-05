package sample

import bfinfo.BF_HEADER_IMAGE_SIZE
import bfinfo.readBFHeader
import galogen.*
import glfw.*
import kotlinx.cinterop.*
import lz4.LZ4_decompress_safe
import platform.opengl32.GL_TEXTURE_MAX_ANISOTROPY_EXT
import kotlin.system.getTimeMillis

@ExperimentalUnsignedTypes
fun checkGLError() {
    val error = glGetError().toInt()
    if( error != GL_NO_ERROR) {
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

    println("OpenGL Version: " + glGetString(GL_VERSION)?.reinterpret<ByteVar>()?.toKString())
    println("OpenGL Vendor: " + glGetString(GL_VENDOR)?.reinterpret<ByteVar>()?.toKString())
    println("OpenGL Renderer: " + glGetString(GL_RENDERER)?.reinterpret<ByteVar>()?.toKString())
    println("GLSL Version: " + glGetString(GL_SHADING_LANGUAGE_VERSION)?.reinterpret<ByteVar>()?.toKString())
    println("=" * 80)

    glEnableDebug()

    glfwSwapInterval( 0 )

    // ---- buffers ----

    val vbo = BufferObject.createArrayBuffer(GL_MAP_READ_BIT,
        cValuesOf(-1f, -1f, 0f,
        1f, -1f, 0f,
        1f, 1f, 0f,
        -1f, 1f, 0f))

    val ibo = BufferObject.createIndexBuffer(GL_MAP_READ_BIT, cValuesOf(3.toByte(), 0, 1, 2, 3, 1))

    println("buffers ready")

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

    println("shaders ready")

    // ---- vao ----

    println("vao")

    val vao = VAO()
    vao.enableAttribute(0)
    vao.defineBinding(0, vbo, 0, 3 * Float.BYTES.toInt())
    vao.defineAttribute(0, 3, GL_FLOAT)
    vao.useBindingAsAttribute(0, 0)
    vao.elementBuffer(ibo)

    vao.bind()
    program.use()

    println("vao ready")


    // ---- texture ----

    val texture = Texture()



    memScoped {
        val (bfData, compressedSize) = readFileBinary("C:\\Users\\Matej\\IdeaProjects\\kotgin\\src\\mingwMain\\resources\\sprites\\kogin_logo.bf")
        val header = readBFHeader(bfData.reinterpret())

        println("bf magic: ${header.magic}")
        println("bf version: ${header.version}")
        println("bf file type: ${header.fileType}")
        println("bf flags lz4: ${header.flags.lz4()}")
        println("bf flags lz4hc: ${header.flags.lz4hc()}")
        println("bf flags vflip: ${header.flags.verticallyFlipped()}")
        println("bf flags dxt: ${header.flags.dxt()}")
        println("bf has mipmaps: ${header.extra.hasMipmaps()}")
        println("bf channels: ${header.extra.numberOfChannels()}")
        println("bf width: ${header.width}")
        println("bf height: ${header.height}")
        println("bf uncompressed size: ${header.uncompressedSize}")

        // move behind header
        val data = (bfData.toLong()+ BF_HEADER_IMAGE_SIZE).toCPointer<ByteVar>()

        val destSize = header.uncompressedSize
        val pixelData = allocArray<ByteVar>(destSize)
        val decompresed = LZ4_decompress_safe(data, pixelData, compressedSize - BF_HEADER_IMAGE_SIZE, destSize)
        if(decompresed < -1) {
            println("decompression failed!")
            println("compressed resource size: $compressedSize")
            println("dest capacity: $destSize")
            println("decompressed bytes: $decompresed")
        }

        val width =header.width.toInt()
        val height = header.height.toInt()

        texture.createStorage(8, GL_RGB8, width, height)
        texture.uploadMipmap(0, width, height, GL_RGB, GL_UNSIGNED_BYTE, pixelData)
        texture.generateOtherMipmaps()
        texture.setWraps(TextureWrapMode.REPEAT, TextureWrapMode.REPEAT)
        texture.setFilters(TextureFilter.LINEAR_MIPMAP_LINEAR, TextureFilter.LINEAR)
        texture.setAnisotropicFiltering(16f)
    }

    texture.bindTo(0)

    // nativeHeap.free(pixelData)

    println("textures ready")

    // ---- render loop ----

    glDebugMessageInsert(GL_DEBUG_SOURCE_APPLICATION, GL_DEBUG_TYPE_OTHER, 0, GL_DEBUG_SEVERITY_NOTIFICATION, 15, "testing message")

    println("debug msg inserted")

    glClearColor(0f,0f,0f,1f)

    println("Started application in ${getTimeMillis() - start} ms.")

    val f = GL_COLOR_BUFFER_BIT or GL_DEPTH_BUFFER_BIT
    var lastTime = 0.0
    var frames: Long = 0
    do {
        glClear(f.toUInt())

        glDrawElements(GL_TRIANGLES, 6, GL_UNSIGNED_BYTE, null)

        glfwSwapBuffers(window)
        glfwPollEvents()
        frames++

        val time = glfwGetTime()
        if (lastTime + 1 < time) {
            glfwSetWindowTitle(window, "FPS: $frames")
            lastTime = time
            frames = 0
        }

        val error = glGetError().toInt()
        if(error != GL_NO_ERROR) {
            throw Exception("Gl error! $error")
        }
    } while (glfwGetKey(window, GLFW_KEY_ESCAPE) != GLFW_PRESS)

    glfwTerminate()

}