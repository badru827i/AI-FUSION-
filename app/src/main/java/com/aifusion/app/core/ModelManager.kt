package com.aifusion.app.core

import android.content.Context
import android.net.Uri
import android.util.Base64

data class LocalModel(
    val uri: String,
    val name: String,
    val format: String,
    val sizeBytes: Long
)

class ModelManager(context: Context) {
    private val prefs = context.getSharedPreferences("ai_fusion_models", Context.MODE_PRIVATE)

    fun list(): List<LocalModel> {
        return prefs.getString("models", "")
            .orEmpty()
            .split("\n")
            .mapNotNull { row ->
                val parts = row.split("|", limit = 4)
                if (parts.size != 4) return@mapNotNull null
                LocalModel(
                    uri = decode(parts[0]),
                    name = decode(parts[1]),
                    format = decode(parts[2]),
                    sizeBytes = parts[3].toLongOrNull() ?: 0L
                )
            }
    }

    fun add(uri: Uri, name: String, format: String = "", sizeBytes: Long) {
        val detected = ModelFormatDetector.detect(uri, name)
        val normalizedFormat = format.ifBlank { detected.format.name }
        val item = LocalModel(uri.toString(), name, normalizedFormat, sizeBytes)
        val next = list().filterNot { it.uri == item.uri } + item
        save(next)
    }

    fun remove(uri: String) {
        save(list().filterNot { it.uri == uri })
    }

    private fun save(items: List<LocalModel>) {
        prefs.edit().putString(
            "models",
            items.joinToString("\n") {
                listOf(encode(it.uri), encode(it.name), encode(it.format), it.sizeBytes.toString()).joinToString("|")
            }
        ).apply()
    }

    private fun encode(value: String): String =
        Base64.encodeToString(value.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)

    private fun decode(value: String): String =
        runCatching { Base64.decode(value, Base64.NO_WRAP).toString(Charsets.UTF_8) }.getOrDefault("")
}
