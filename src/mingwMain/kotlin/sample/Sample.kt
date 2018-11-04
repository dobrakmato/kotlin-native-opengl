package sample

import bfinfo.BF_HEADER_IMAGE_SIZE
import bfinfo.readBFHeader
import galogen.*
import glfw.*
import kotlinx.cinterop.*
import lz4.LZ4_decompress_safe
import platform.opengl32.GL_TEXTURE_MAX_ANISOTROPY_EXT
import platform.posix.fclose
import platform.posix.fopen
import platform.posix.fwrite
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

    glEnableDebug()

    glfwSwapInterval( 0 )

    // ---- buffers ----

    val (vboId, iboId) = memScoped {
        val buffers = allocArray<UIntVar>(2)
        glCreateBuffers(2, buffers)
        Pair(buffers[0], buffers[1])
    }
    val vbo = cValuesOf(-1f, -1f, 0f,
                        1f, -1f, 0f,
                        1f, 1f, 0f,
                        -1f, 1f, 0f)
    glBindBuffer(GL_ARRAY_BUFFER, vboId)
    glNamedBufferStorage(vboId, vbo.size.toLong(), vbo, GL_MAP_READ_BIT)

    val ibo = cValuesOf(3.toByte(), 0, 1, 2, 3, 1)
    glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, iboId)
    glNamedBufferStorage(iboId, ibo.size.toLong(), ibo, GL_MAP_READ_BIT)

    println("buffers ready")

    // ---- shaders ----

    val program = memScoped {
        val vertex = glCreateShader(GL_VERTEX_SHADER)
        val vertexSource = arrayOf(readFileText("C:\\Users\\Matej\\IdeaProjects\\kotgin\\src\\mingwMain\\resources\\shaders\\basic.vert"))
        glShaderSource(vertex,  1, vertexSource.toCStringArray(this), cValuesOf(vertexSource[0].length))
        glCompileShader(vertex)
        val isCompiled = alloc<GLintVar>()
        glGetShaderiv(vertex, GL_COMPILE_STATUS, isCompiled.ptr)
        if (isCompiled.value == GL_FALSE) {
            println("Shader was not compiled!!!")
        }

        val logSize = 1024
        val log = allocArray<GLcharVar>(logSize)
        val actuallyWritten = alloc<IntVar>()
        glGetShaderInfoLog(vertex, logSize, actuallyWritten.ptr, log)
        val actualLog = log.toKString().substring(actuallyWritten.value)

        println("Vertex shader compilation log: $actualLog")

        val fragment = glCreateShader(GL_FRAGMENT_SHADER)
        val fragmentSource = arrayOf(readFileText("C:\\Users\\Matej\\IdeaProjects\\kotgin\\src\\mingwMain\\resources\\shaders\\basic.frag"))
        glShaderSource(fragment,  1, fragmentSource.toCStringArray(this), cValuesOf(fragmentSource[0].length))
        glCompileShader(fragment)

        val logSize2 = 1024
        val log2 = allocArray<GLcharVar>(logSize2)
        val actuallyWritten2 = alloc<IntVar>()
        glGetShaderInfoLog(vertex, logSize2, actuallyWritten2.ptr, log2)
        val actualLog2 = log2.toKString().substring(actuallyWritten2.value)
        println("Fragment shader compilation log: $actualLog2")

        val program = glCreateProgram()
        glAttachShader(program, vertex)
        glAttachShader(program, fragment)
        glLinkProgram(program)

        val logSize3 = 1024
        val log3 = allocArray<GLcharVar>(logSize3)
        val actuallyWritten3 = alloc<IntVar>()
        glGetProgramInfoLog(program, logSize3, actuallyWritten3.ptr, log3)
        val actualLog3 = log3.toKString().substring(actuallyWritten3.value)
        println("Program linking log: $actualLog3")
        program
    }

    println("shaders ready")

    // ---- vao ----

    println("vao")

    val vaoId = memScoped {
        val vaobuffers = allocArray<UIntVar>(1)
        glCreateVertexArrays(1, vaobuffers)
        vaobuffers[0]
    }

    // a b c d e f g h i

    // a b c
    // d e f
    // g h i

    // poviem ze existuje 0
    glEnableVertexArrayAttrib(vaoId, 0)

    // b0 je takto definovane v buffers[0]
    glVertexArrayVertexBuffer(vaoId, 0, vboId, 0, 3 * Float.BYTES.toInt())
    // a0 je taketo
    glVertexArrayAttribFormat(vaoId, 0, 3, GL_FLOAT, GL_FALSE, 0)
    // b0 = a0
    glVertexArrayAttribBinding(vaoId, 0, 0)

    // wtf?
    glVertexArrayElementBuffer(vaoId, iboId)

    glBindVertexArray(vaoId)
    glUseProgram(program)

    println("vao ready")


    // ---- texture ----

    val textureId = memScoped {
        val textureBuffers = allocArray<UIntVar>(1)
        glCreateTextures(GL_TEXTURE_2D, 1, textureBuffers)
        textureBuffers[0]
    }

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

        glTextureStorage2D(textureId, 8, GL_RGB8, header.width.toInt(), header.height.toInt())
        glTextureSubImage2D(textureId, 0, 0,0, header.width.toInt(), header.height.toInt(), GL_RGB, GL_UNSIGNED_BYTE, pixelData)
        glGenerateTextureMipmap(textureId)
        glTextureParameteri(textureId, GL_TEXTURE_WRAP_S, GL_REPEAT)
        glTextureParameteri(textureId, GL_TEXTURE_WRAP_T, GL_REPEAT)
        glTextureParameteri(textureId, GL_TEXTURE_MAG_FILTER, GL_LINEAR)
        glTextureParameteri(textureId, GL_TEXTURE_MIN_FILTER, GL_LINEAR_MIPMAP_LINEAR)
        glTextureParameterf(textureId, GL_TEXTURE_MAX_ANISOTROPY_EXT, 16f)

    }

    glBindTextureUnit(0, textureId)


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