package bfinfo

import bf.BfImageHeader
import galogen.*
import platform.opengl32.GL_COMPRESSED_RGBA_S3TC_DXT5_EXT
import platform.opengl32.GL_COMPRESSED_RGB_S3TC_DXT1_EXT
import platform.opengl32.GL_R

internal val BfImageHeader.glInternalFormat: Int
    get() {
        if (flags.srgb()) {
            return when (extra.numberOfChannels()) {
                3 -> GL_SRGB8
                4 -> GL_SRGB8_ALPHA8
                else -> throw Exception("Invalid number of channels for SRGB!")
            }
        }

        // todo: support more texture formats

        return when (extra.numberOfChannels()) {
            1 -> GL_R8
            2 -> GL_RG8
            3 -> GL_RGB8
            4 -> GL_RGBA8
            else -> throw Exception("Invalid number of channels! Or not implemented yet.")
        }
    }

internal val BfImageHeader.glCompressedFormat: Int
    get() {
        if (!flags.dxt()) {
            throw RuntimeException("Accessing compressed format of uncompressed texture!")
        }

        return when (extra.numberOfChannels()) {
            3 -> GL_COMPRESSED_RGB_S3TC_DXT1_EXT
            4 -> GL_COMPRESSED_RGBA_S3TC_DXT5_EXT
            else -> throw Exception("Invalid number of channels! Or not implemented yet.")
        }
    }

internal val BfImageHeader.glFormat: Int
    get() {
        return when (extra.numberOfChannels()) {
            1 -> GL_R
            2 -> GL_RG
            3 -> GL_RGB
            4 -> GL_RGBA
            else -> throw Exception("Invalid number of channels!")
        }
    }

internal val BfImageHeader.glType: Int
    get() {
        return if (flags.float()) {
            GL_FLOAT
        } else {
            GL_UNSIGNED_BYTE
        }
    }