package com.aifusion.app.core

import android.content.Context
import android.net.Uri
import android.os.Build
import java.io.File
import java.io.FileOutputStream
import dev.ffmpegkit.llama.Llama
import dev.ffmpegkit.llama.LlamaConfig

/**
 * Real local GGUF inference adapter.
 *
 * The bundled AAR targets arm64-v8a. Unsupported ABIs return null so AI-FUSION
 * can safely fall back to its lightweight local layer or the optional web AI.
 */
object LocalLlamaEngine {
    private const val BYTES_PER_MB = 1024L * 1024L
    private const val MAX_MODEL_FRACTION_OF_AVAILABLE_RAM = 0.5

    suspend fun generate(
        context: Context,
        modelUri: Uri,
        modelName: String,
        prompt: String,
        capabilities: DeviceCapabilities
    ): String? {
        if (!Build.SUPPORTED_ABIS.any { it.equals("arm64-v8a", ignoreCase = true) }) return null
        if (prompt.isBlank()) return null

        val modelDir = context.getExternalFilesDir("models") ?: File(context.filesDir, "models")
        if (!modelDir.exists() && !modelDir.mkdirs()) return null

        val safeName = modelName.replace(Regex("[^A-Za-z0-9._-]"), "_").ifBlank { "model.gguf" }
        val modelFile = File(modelDir, safeName)

        // Preflight RAM before copying the selected model. The previous check only
        // examined modelFile.length() when the file already existed, so the first
        // import could copy a model larger than the available memory budget.
        val availableRamMb = runCatching {
            val info = android.app.ActivityManager.MemoryInfo()
            context.getSystemService(android.app.ActivityManager::class.java)
                ?.getMemoryInfo(info) ?: return null
            info.availMem / BYTES_PER_MB
        }.getOrDefault(0L)
        if (availableRamMb <= 0L) return null

        val maxModelBytes = (availableRamMb * BYTES_PER_MB * MAX_MODEL_FRACTION_OF_AVAILABLE_RAM).toLong()
        val sourceLength = runCatching {
            context.contentResolver.openAssetFileDescriptor(modelUri, "r")?.use { it.length }
        }.getOrNull() ?: -1L
        if (sourceLength > 0L && sourceLength > maxModelBytes) return null

        if (!modelFile.exists() || modelFile.length() == 0L) {
            val copied = runCatching {
                context.contentResolver.openInputStream(modelUri)?.use { input ->
                    FileOutputStream(modelFile).use { output -> input.copyTo(output) }
                } ?: return null
            }.isSuccess
            if (!copied) {
                modelFile.delete()
                return null
            }
        }

        // Always re-check after copying: SAF providers may report an unknown size,
        // and an existing cached file can differ from the current source URI.
        val actualModelBytes = modelFile.length()
        if (actualModelBytes <= 0L || actualModelBytes > maxModelBytes) return null

        val threads = when (capabilities.mode) {
            PerformanceMode.LOW_RAM -> capabilities.cpuCores.coerceAtMost(4)
            PerformanceMode.BALANCED -> capabilities.cpuCores.coerceAtMost(6)
            PerformanceMode.PERFORMANCE -> capabilities.cpuCores.coerceAtMost(8)
        }.coerceAtLeast(1)

        val contextSize = when (capabilities.mode) {
            PerformanceMode.LOW_RAM -> 1024
            PerformanceMode.BALANCED -> 2048
            PerformanceMode.PERFORMANCE -> 3072
        }

        return try {
            val model = Llama.loadModel(
                modelPath = modelFile.absolutePath,
                config = LlamaConfig(contextSize = contextSize, threads = threads)
            )
            try {
                val system = when (detectLanguage(prompt)) {
                    ChatLanguage.BM -> "Anda ialah AI-FUSION, pembantu AI lokal yang menjawab dalam Bahasa Melayu Malaysia dengan jelas."
                    ChatLanguage.ENGLISH -> "You are AI-FUSION, a local AI assistant. Answer clearly and accurately."
                    ChatLanguage.MIXED -> "Anda ialah AI-FUSION. Jawab secara natural dalam gaya campuran BM/English pengguna."
                }
                Llama.complete(
                    model = model,
                    prompt = prompt,
                    systemPrompt = system,
                    maxTokens = if (capabilities.mode == PerformanceMode.LOW_RAM) 192 else 320
                ).text.trim().takeIf { it.isNotBlank() }
            } finally {
                Llama.releaseModel(model)
            }
        } catch (_: Throwable) {
            null
        }
    }
}
