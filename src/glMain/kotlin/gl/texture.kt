package gl

import galogen.*
import kotlinx.cinterop.CValuesRef
import kotlinx.cinterop.cValuesOf
import kotlinx.cinterop.memScoped
import platform.opengl32.GL_TEXTURE_MAX_ANISOTROPY_EXT


enum class TextureFilter(val glFilter: Int) {
    LINEAR(GL_LINEAR),
    NEAREST(GL_NEAREST),
    LINEAR_MIPMAP_LINEAR(GL_LINEAR_MIPMAP_LINEAR),
    LINEAR_MIPMAP_NEAREST(GL_LINEAR_MIPMAP_NEAREST),
}

enum class TextureWrapMode(val glWrapMode: Int) {
    REPEAT(GL_REPEAT),
    CLAMP_TO_EDGE(GL_CLAMP_TO_EDGE),
    CLAMP_TO_BORDER(GL_CLAMP_TO_BORDER),
    MIRRORED_REPEAT(GL_MIRRORED_REPEAT),
}

class Texture2D(override val id: UInt = GLObjects.newTexture2D()) : Labelled, Disposable {
    override var label: String? by Label(GL_TEXTURE)

    fun bindTo(sampler: Int) {
        glBindTextureUnit(sampler.toUInt(), id)
    }

    override fun free() {
        memScoped {
            glDeleteTextures(1, cValuesOf(id))
        }
    }

    fun createStorage(mipmapLevels: Int, internalFormat: Int, width: Int, height: Int) {
        glTextureStorage2D(id, mipmapLevels, internalFormat.toUInt(), width, height)
    }

    fun uploadMipmap(
        mipmapLevel: Int,
        width: Int,
        height: Int,
        format: Int,
        type: Int,
        pixelData: CValuesRef<*>
    ) {
        glTextureSubImage2D(id, mipmapLevel, 0, 0, width, height, format.toUInt(), type.toUInt(), pixelData)
    }

    fun uploadCompressedMipmap(
        mipmapLevel: Int,
        width: Int,
        height: Int,
        compressedFormat: Int,
        imageSize: Int,
        compressedPixelData: CValuesRef<*>
    ) {
        glCompressedTextureSubImage2D(
            id,
            mipmapLevel,
            0,
            0,
            width,
            height,
            compressedFormat.toUInt(),
            imageSize,
            compressedPixelData
        )
    }

    fun generateOtherMipmaps() {
        glGenerateTextureMipmap(id)
    }

    fun setFilters(minFilter: TextureFilter, magFilter: TextureFilter) {
        glTextureParameteri(id, GL_TEXTURE_MAG_FILTER, magFilter.glFilter)
        glTextureParameteri(id, GL_TEXTURE_MIN_FILTER, minFilter.glFilter)
    }

    fun setWraps(sWrapMode: TextureWrapMode, tWrapMode: TextureWrapMode) {
        glTextureParameteri(id, GL_TEXTURE_WRAP_S, sWrapMode.glWrapMode)
        glTextureParameteri(id, GL_TEXTURE_WRAP_T, tWrapMode.glWrapMode)
    }

    fun setAnisotropicFiltering(anisotropyLevel: Float) {
        glTextureParameterf(id, GL_TEXTURE_MAX_ANISOTROPY_EXT, anisotropyLevel)
    }

    companion object {
        var defaultAnisotropyLevel = 16f
    }
}
