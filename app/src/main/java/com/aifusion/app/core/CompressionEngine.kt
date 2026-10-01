package com.aifusion.app.core

import android.util.Base64
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

object CompressionEngine {
    fun compress(text: String): String {
        val out = ByteArrayOutputStream()
        GZIPOutputStream(out).use { gzip ->
            gzip.write(text.toByteArray(Charsets.UTF_8))
        }
        return Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
    }

    fun decompress(encoded: String): String {
        return try {
            val bytes = Base64.decode(encoded, Base64.NO_WRAP)
            GZIPInputStream(ByteArrayInputStream(bytes)).use { gzip ->
                gzip.readBytes().toString(Charsets.UTF_8)
            }
        } catch (_: Exception) {
            ""
        }
    }
}
