package bfinfo

import bf.BfImageHeader
import bf.computeBfImagePayloadSize
import bf.readBfImageHeader
import bf.readLZ4Decompressed
import galogen.*
import gl.*
import glfw.*
import io.ByteBuffer
import io.readUTF8TextFile
import kotlinx.cinterop.*
import math.*
import utils.Logger
import utils.SIZE_BYTES
import kotlin.math.abs
import kotlin.math.cos
import kotlin.system.getTimeMillis

data class Rect(val x: Float, val y: Float, val w: Float, val h: Float)

fun remap(value: Float, low1: Float, high1: Float, low2: Float, high2: Float) =
    low2 + (value - low1) * (high2 - low2) / (high1 - low1)

fun cover(imgW: Int, imgH: Int, canvasW: Int, canvasH: Int): Rect {
    val realW: Float
    val realH: Float
    var posX = 0f
    var posY = 0f

    if ((canvasW - imgW) < (canvasH - imgH)) {
        realW = canvasW.toFloat()
        realH = (imgH.toFloat() / imgW) * realW
        posY = canvasH * 0.5f - (realH * 0.5f)
    } else {
        realH = canvasH.toFloat()
        realW = (imgW.toFloat() / imgH) * realH
        posX = canvasW * 0.5f - (realW * 0.5f)
    }
    return Rect(posX, posY, realW, realH)
}

@ThreadLocal
object Parameters {
    var width = 1024
    var height = 768
    var scale = 1f
}

fun mouseScrollCallback(window: CPointer<GLFWwindow>?, x: Double, y: Double) {
    if (y > 0f) {
        Parameters.scale *= 1.3f
    } else {
        Parameters.scale *= 0.76923076923f
    }
    Parameters.scale = clamp(0.1f, 20f, Parameters.scale)
}

class ImgView(private val bfImage: ByteBuffer, private val title: String) {

    private val log: Logger = Logger("imgview")
    private var translation: Vector2f = vec2(0f, 0f)

    private var lastPosition: Vector2f? = null
    private var lastClicked: Long = 0

    fun run() {
        val start = getTimeMillis()

        if (glfwInit() != GLFW_TRUE) {
            throw Exception("Failed to initialize GLFW")
        }

        glfwSetErrorCallback(staticCFunction { _, description: CPointer<ByteVar>? ->
            println("GLFW Error: " + description?.toKString())
        })

        glfwWindowHint(GLFW_RESIZABLE, GL_TRUE)
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 4)
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 6)
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE)

        val window = glfwCreateWindow(Parameters.width, Parameters.height, title, null, null)

        if (window == null) {
            glfwTerminate()
            throw Exception("Failed to open GLFW window. If you have an Intel GPU, they are not 3.3 compatible.")
        }

        log.debug("Window created in ${getTimeMillis() - start} ms")

        glfwMakeContextCurrent(window)
        glfwSetWindowSizeCallback(window, staticCFunction { _, width, height ->
            Parameters.width = width
            Parameters.height = height
            glViewport(0, 0, width, height)
        })

        glfwSetScrollCallback(window, staticCFunction(::mouseScrollCallback))

        log.debug("OpenGL Version: " + glGetString(GL_VERSION)?.reinterpret<ByteVar>()?.toKString())
        log.debug("OpenGL Vendor: " + glGetString(GL_VENDOR)?.reinterpret<ByteVar>()?.toKString())
        log.debug("OpenGL Renderer: " + glGetString(GL_RENDERER)?.reinterpret<ByteVar>()?.toKString())
        log.debug("GLSL Version: " + glGetString(GL_SHADING_LANGUAGE_VERSION)?.reinterpret<ByteVar>()?.toKString())

        glEnableDebug(true)
        glfwSwapInterval(1) // enable vsync

        /* load shaders */
        val vertex = VertexShader()
        vertex.uploadSource(readUTF8TextFile("C:\\Users\\Matej\\IdeaProjects\\kotgin\\src\\bfinfoMain\\resources\\shaders\\basic.vert"))
        vertex.compile()

        val fragment = FragmentShader()
        fragment.uploadSource(readUTF8TextFile("C:\\Users\\Matej\\IdeaProjects\\kotgin\\src\\bfinfoMain\\resources\\shaders\\basic.frag"))
        fragment.compile()

        val program = Program()
        program.attachShader(vertex)
        program.attachShader(fragment)
        program.link()

        /* uncompress and prepare image data */
        val header = bfImage.readBfImageHeader()

        log.debug("bf magic: ${header.header.magic}")
        log.debug("bf version: ${header.header.version}")
        log.debug("bf file type: ${header.header.fileType}")
        log.debug("bf flags lz4: ${header.flags.lz4()}")
        log.debug("bf flags vflip: ${header.flags.verticallyFlipped()}")
        log.debug("bf flags dxt: ${header.flags.dxt()}")
        log.debug("bf mipmaps: ${header.extra.includedMipmaps()}")
        log.debug("bf channels: ${header.extra.numberOfChannels()}")
        log.debug("bf width: ${header.width}")
        log.debug("bf height: ${header.height}")
        log.debug("(computed) header size: ${BfImageHeader.SIZE_BYTES}")
        log.debug("(computed) uncompressed size: ${computeBfImagePayloadSize(header)}")

        val payload: CArrayPointer<UByteVar> = if (header.flags.lz4()) {
            val compressedSize = bfImage.size - BfImageHeader.SIZE_BYTES
            val decompressedSize = computeBfImagePayloadSize(header)
            bfImage.readLZ4Decompressed(compressedSize.toInt(), decompressedSize).data
        } else {
            (bfImage.data + bfImage.pos.toLong())!!
        }

        val texture = Texture2D()

        if (header.flags.dxt()) {
            val w = header.width.toInt()
            val h = header.height.toInt()
            val c = header.extra.numberOfChannels()
            val size = if (c == 3) (w * h * 3) / 6 else (w * h * 4) / 4
            texture.createStorage(header.extra.includedMipmaps(), header.glCompressedFormat, w, h)
            texture.uploadCompressedMipmap(0, w, h, header.glCompressedFormat, size, payload)
        } else {
            texture.createStorage(
                header.extra.includedMipmaps(), header.glInternalFormat,
                header.width.toInt(), header.height.toInt()
            )
            texture.uploadMipmap(
                0, header.width.toInt(), header.height.toInt(),
                header.glFormat, header.glType, payload
            )
        }

        /* Upload additional mipmaps. */
        texture.generateOtherMipmaps()

        /* Set by using metadata. */
        texture.setFilters(TextureFilter.LINEAR, TextureFilter.LINEAR)
        texture.setWraps(TextureWrapMode.REPEAT, TextureWrapMode.REPEAT)
        texture.setAnisotropicFiltering(Texture2D.defaultAnisotropyLevel)

        /* load and upload fonts */

        /* create vbo & vao */
        val vbo = BufferObject.createArrayBuffer(
            GL_MAP_READ_BIT or GL_DYNAMIC_STORAGE_BIT,
            cValuesOf(
                -1f, -1f, 0f,
                1f, -1f, 0f,
                1f, 1f, 0f,
                -1f, 1f, 0f
            )
        )

        val ibo = BufferObject.createIndexBuffer(GL_MAP_READ_BIT, cValuesOf(3.toByte(), 0, 1, 2, 3, 1))

        val vao = VAO()
        vao.enableAttribute(0)
        vao.defineBinding(0, vbo, 0, 3 * Float.SIZE_BYTES)
        vao.defineAttribute(0, 3, GL_FLOAT)
        vao.useBindingAsAttribute(0, 0)
        vao.elementBuffer(ibo)


        glClearColor(0f, 0f, 0f, 1f)

        /* start render-loop */
        vao.bind()
        program.use()
        texture.bindTo(0)

        var frames = 0f

        do {
            glClear((GL_COLOR_BUFFER_BIT or GL_DEPTH_BUFFER_BIT).toUInt())

            val rect = cover(header.width.toInt(), header.height.toInt(), Parameters.width, Parameters.height)
            val x0 = remap(rect.x, 0f, Parameters.width.toFloat(), -1f, 1f)
            val y0 = remap(rect.y, 0f, Parameters.height.toFloat(), -1f, 1f)

            val x1 = remap(rect.x + rect.w, 0f, Parameters.width.toFloat(), -1f, 1f)
            val y1 = remap(rect.y + rect.h, 0f, Parameters.height.toFloat(), -1f, 1f)

            /* update image positions to preserve image aspect ratio */
            vbo.uploadData(
                0, Float.SIZE_BYTES * 12L, cValuesOf(
                    x0, y0, 0f,
                    x1, y0, 0f,
                    x1, y1, 0f,
                    x0, y1, 0f
                )
            )
            frames += 0.016f

            if (glfwGetMouseButton(window, GLFW_MOUSE_BUTTON_1) == GLFW_PRESS) {

                // detect double-click "reset"
                if (getTimeMillis() - lastClicked < 100) {
                    translation = vec2(0f, 0f)
                    Parameters.scale = 1.0f
                }
                lastClicked = -1

                // translate
                memScoped {
                    val x = alloc<DoubleVar>()
                    val y = alloc<DoubleVar>()
                    glfwGetCursorPos(window, x.ptr, y.ptr)
                    val current = vec2(x.value.toFloat(), y.value.toFloat())
                    if (lastPosition != null) translation += (lastPosition!! - current)
                    lastPosition = current
                }
            } else {
                lastPosition = null
                if (lastClicked == -1L) {
                    lastClicked = getTimeMillis()
                }
            }

            val translation = Matrix4f.createTranslation(
                -translation.x / (Parameters.width * 0.5f) / Parameters.scale,
                translation.y / (Parameters.height * 0.5f) / Parameters.scale,
                0f
            )
            val scale = Matrix4f.createScale(Parameters.scale, Parameters.scale, Parameters.scale)
            program.setUniform("mvp", true, (scale * translation).toFloatArray().toCValues())

            glDrawElements(GL_TRIANGLES, 6, GL_UNSIGNED_BYTE, null)
            glfwPollEvents()
            glfwSwapBuffers(window)
            if (glfwGetKey(window, GLFW_KEY_ESCAPE) == GLFW_PRESS) {
                glfwSetWindowShouldClose(window, 1)
            }

            if (glfwGetKey(window, GLFW_KEY_N) == GLFW_PRESS) {
                texture.setFilters(TextureFilter.NEAREST, TextureFilter.NEAREST)
            }
            if (glfwGetKey(window, GLFW_KEY_L) == GLFW_PRESS) {
                texture.setFilters(TextureFilter.LINEAR, TextureFilter.LINEAR)
            }
        } while (glfwWindowShouldClose(window) == 0)

        glfwTerminate()
    }

}