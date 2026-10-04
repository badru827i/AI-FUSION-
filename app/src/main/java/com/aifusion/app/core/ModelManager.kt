package com.aifusion.app.core

import android.content.Context
import android.net.Uri
import android.util.Base64
import java.io.File

data class LocalModel(
    val uri: String,
    val name: String,
    val format: String,
    val sizeBytes: Long
)

class ModelManager(context: Context) {
    companion object { const val MAX_MODELS = 10 }
    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences("ai_fusion_models", Context.MODE_PRIVATE)
    private val modelDir = appContext.getExternalFilesDir("models") ?: File(appContext.filesDir, "models")

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
        val normalizedFormat = if (detected.format != AiModelFormat.UNKNOWN) detected.format.name else format.ifBlank { "UNKNOWN" }
        val item = LocalModel(uri.toString(), name, normalizedFormat, sizeBytes)
        val next = (list().filterNot { it.uri == item.uri } + item).takeLast(MAX_MODELS)
        save(next)
    }

    fun remove(uri: String) {
        val item = list().firstOrNull { it.uri == uri }
        save(list().filterNot { it.uri == uri })
        item?.let { modelFile(it.name).delete() }
    }

    fun totalSizeBytes(): Long = list().sumOf { it.sizeBytes.coerceAtLeast(0L) }

    fun runtimeStatus(model: LocalModel): String {
        val info = ModelFormatDetector.detect(Uri.parse(model.uri), model.name)
        return when (info.format) {
            AiModelFormat.GGUF -> "READY • GGUF / llama.cpp"
            AiModelFormat.ONNX -> "IMPORTED • ONNX runtime adapter not bundled"
            AiModelFormat.TFLITE -> "IMPORTED • LiteRT runtime adapter not bundled"
            AiModelFormat.EXECUTORCH -> "IMPORTED • ExecuTorch runtime adapter not bundled"
            AiModelFormat.GGML -> "IMPORTED • convert to GGUF first"
            else -> "IMPORTED • " + info.runtime
        }
    }

    private fun modelFile(name: String): File {
        if (!modelDir.exists()) modelDir.mkdirs()
        return File(modelDir, name.replace(Regex("[^A-Za-z0-9._-]"), "_").ifBlank { "model.bin" })
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
