package com.aifusion.app.core.compression

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream
import kotlin.math.max

/**
 * Lightweight, dependency-free compression core for AI Fusion.
 *
 * Uses streaming GZIP so large payloads do not need to be duplicated in RAM.
 * This is intended for chat/cache/JSON/text data. It does not recompress
 * already-compressed formats such as JPEG/PNG/ZIP/APK.
 */
object CompressionCore {

    enum class DataType {
        TEXT, JSON, CACHE, BINARY, ALREADY_COMPRESSED
    }

    data class Result(
        val data: ByteArray,
        val originalBytes: Long,
        val compressedBytes: Long
    ) {
        val savedBytes: Long
            get() = max(0L, originalBytes - compressedBytes)

        val ratioPercent: Int
            get() = if (originalBytes <= 0) 0
            else ((compressedBytes.toDouble() / originalBytes) * 100.0).toInt()
    }

    fun compress(input: ByteArray, type: DataType = DataType.BINARY): Result {
        if (input.isEmpty() || type == DataType.ALREADY_COMPRESSED) {
            return Result(input, input.size.toLong(), input.size.toLong())
        }

        val output = ByteArrayOutputStream(max(32, input.size / 2))
        GZIPOutputStream(output, 8 * 1024).use { gzip ->
            ByteArrayInputStream(input).use { source ->
                copy(source, gzip)
            }
        }
        val compressed = output.toByteArray()

        // Compression can make small/random data larger. Keep the original in that case.
        return if (compressed.size < input.size) {
            Result(compressed, input.size.toLong(), compressed.size.toLong())
        } else {
            Result(input, input.size.toLong(), input.size.toLong())
        }
    }

    fun decompress(input: ByteArray): ByteArray {
        if (input.size < 2 || input[0] != GZIPInputStream.GZIP_MAGIC.toByte() ||
            input[1] != (GZIPInputStream.GZIP_MAGIC ushr 8).toByte()) {
            return input
        }

        val output = ByteArrayOutputStream()
        GZIPInputStream(ByteArrayInputStream(input), 8 * 1024).use { gzip ->
            copy(gzip, output)
        }
        return output.toByteArray()
    }

    fun compressText(text: String): Result =
        compress(text.toByteArray(Charsets.UTF_8), DataType.TEXT)

    fun decompressText(data: ByteArray): String =
        decompress(data).toString(Charsets.UTF_8)

    private fun copy(input: InputStream, output: java.io.OutputStream) {
        val buffer = ByteArray(8 * 1024)
        while (true) {
            val read = input.read(buffer)
            if (read < 0) break
            if (read > 0) output.write(buffer, 0, read)
        }
    }
}
