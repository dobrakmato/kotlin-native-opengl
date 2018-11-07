package sample

import bfinfo.BFImageHeader
import bfinfo.BF_HEADER_IMAGE_SIZE
import bfinfo.BF_MAGIC
import bfinfo.readBFHeader
import galogen.*
import kotlinx.cinterop.*
import lz4.LZ4_decompress_safe
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

        val version = data[4]
        val fileType = data[5]

        // delegate to specific loader
        when (fileType.toInt()) {
            1 -> BFTextureLoader.load(asset as Asset<Texture>, data, size)
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

object BFTextureLoader : SpecializedAssetLoader<Texture> {
    private val BFImageHeader.glInternalFormat: Int
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

    private val BFImageHeader.glCompressedFormat: Int
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

    private val BFImageHeader.glFormat: Int
        get() {
            return when (extra.numberOfChannels()) {
                1 -> GL_R
                2 -> GL_RG
                3 -> GL_RGB
                4 -> GL_RGBA
                else -> throw Exception("Invalid number of channels!")
            }
        }

    private val BFImageHeader.glType: Int
        get() {
            return if (flags.float()) {
                GL_FLOAT
            } else {
                GL_UNSIGNED_BYTE
            }
        }

    override fun load(asset: Asset<Texture>, data: FileData, dataSize: Int) {
        val header = readBFHeader(data.reinterpret())
        val dataPointer = skipHeader(data)

        /* DEBUG */
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

        val realDataPointer = decompressIfNeeded(header, dataPointer, sizeWithoutHeader(dataSize))

        val texture = Texture()
        texture.label = "Texture for ${asset.path}"

        if (header.flags.dxt()) {
            val w = header.width.toInt()
            val h = header.height.toInt()
            val c = header.extra.numberOfChannels()
            val size = if (c == 3) (w * h * 3) / 6 else (w * h * 4) / 4
            texture.createStorage(header.extra.includedMipmaps(), header.glCompressedFormat, w, h)
            texture.uploadCompressedMipmap(0, w, h, header.glCompressedFormat, size, realDataPointer)
        } else {
            texture.createStorage(
                header.extra.includedMipmaps(), header.glInternalFormat,
                header.width.toInt(), header.height.toInt()
            )
            texture.uploadMipmap(
                0, header.width.toInt(), header.height.toInt(),
                header.glFormat, header.glType, realDataPointer
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
            nativeHeap.free(realDataPointer)
        }
    }

    private fun decompressIfNeeded(
        header: BFImageHeader,
        @Mallocated dataPointer: CArrayPointer<UByteVar>,
        sizeWithoutHeader: Int
    ): @Mallocated CArrayPointer<UByteVar> {
        if (header.flags.lz4()) {
            return decompress(header.uncompressedSize, dataPointer, sizeWithoutHeader)
        }
        return dataPointer
    }

    private fun decompress(
        uncompressedSize: Int,
        compressedData: CArrayPointer<UByteVar>,
        sizeWithoutHeader: Int
    ): CArrayPointer<UByteVar> {
        val decompressed = nativeHeap.allocArray<UByteVar>(uncompressedSize)
        LZ4_decompress_safe(
            compressedData.reinterpret(),
            decompressed.reinterpret(),
            sizeWithoutHeader,
            uncompressedSize
        )
        return decompressed
    }

    private fun sizeWithoutHeader(size: Int): Int {
        return size - BF_HEADER_IMAGE_SIZE
    }

    private fun skipHeader(contents: CPointer<UByteVar>): CPointer<UByteVar> {
        return ((contents.toLong() + BF_HEADER_IMAGE_SIZE).toCPointer())
            ?: throw RuntimeException("Null pointer exception")
    }
}