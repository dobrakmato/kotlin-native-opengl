package sample

import bf.BfImageHeader
import bf.computeBfImagePayloadSize
import bf.readBfImageHeader
import bf.readLZ4Decompressed
import bfinfo.BF_MAGIC
import galogen.*
import io.ByteBuffer
import kotlinx.cinterop.*
import platform.opengl32.GL_COMPRESSED_RGBA_S3TC_DXT5_EXT
import platform.opengl32.GL_COMPRESSED_RGB_S3TC_DXT1_EXT
import platform.opengl32.GL_R

/* generic asset loading */

class Asset<T>(val path: String) {
    var asset: T? = null
        internal set
    var isLoaded: Boolean = false
        internal set
}

object AssetLoader {

    private val loadedAssets = mutableListOf<Asset<*>>() // todo: implement cache
    private val assetMetadataStore = null // todo: implement asset metadata

    fun <T> load(path: String): Asset<T> {
        val asset = Asset<T>(path)
        val (data, size) = readFileBinary(asset.path)

        if (data.integerAt(0) != BF_MAGIC) {
            throw Exception("Not a BF file!")
        }

        val fileType = data[5]

        // delegate to specific loader
        when (fileType.toInt()) {
            1 -> BFTextureLoaderVersion2TEST.load(asset as Asset<Texture>, data, size)
            else -> throw RuntimeException("Loading of type $fileType is not yet supported!")
        }

        nativeHeap.free(data)

        asset.isLoaded = true
        return asset
    }

    fun <T> getAsset(path: String): T {
        return load<T>(path).asset!!
    }
}

interface SpecializedAssetLoader<T> {
    fun load(asset: Asset<T>, data: FileData, dataSize: Int)
}

/* texture specialized loading */

object BFTextureLoaderVersion2TEST : SpecializedAssetLoader<Texture> {
    private val log = Logger("BFTextureLoaderVersion2TEST")

    private val BfImageHeader.glInternalFormat: Int
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

    private val BfImageHeader.glCompressedFormat: Int
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

    private val BfImageHeader.glFormat: Int
        get() {
            return when (extra.numberOfChannels()) {
                1 -> GL_R
                2 -> GL_RG
                3 -> GL_RGB
                4 -> GL_RGBA
                else -> throw Exception("Invalid number of channels!")
            }
        }

    private val BfImageHeader.glType: Int
        get() {
            return if (flags.float()) {
                GL_FLOAT
            } else {
                GL_UNSIGNED_BYTE
            }
        }

    override fun load(asset: Asset<Texture>, data: FileData, dataSize: Int) {
        val buffer = ByteBuffer(dataSize.toLong(), data)
        val header = buffer.readBfImageHeader()

        /* DEBUG */
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
        log.debug("(computed) real bytes left: ${dataSize - BfImageHeader.SIZE_BYTES}")


        /* prepare the payload */
        val payload: CArrayPointer<UByteVar> = if (header.flags.lz4()) {
            val compressedSize = dataSize - BfImageHeader.SIZE_BYTES
            val decompressedSize = computeBfImagePayloadSize(header)
            buffer.readLZ4Decompressed(compressedSize, decompressedSize).data
        } else {
            (buffer.data + buffer.pos.toLong())!!
        }

        val texture = Texture()
        texture.label = "Texture for ${asset.path}"

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
        texture.setAnisotropicFiltering(Texture.defaultAnisotropyLevel)

        asset.asset = texture

        /* Only free if we allocated more space. */
        if (header.flags.lz4()) {
            nativeHeap.free(payload)
        }
    }
}