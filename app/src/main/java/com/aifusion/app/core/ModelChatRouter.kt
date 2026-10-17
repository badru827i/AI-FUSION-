package com.aifusion.app.core

import android.content.Context

/**
 * Single source of truth for choosing a model that can actually serve chat.
 *
 * ONNX and LiteRT imports are deliberately not marked chat-capable: a generic
 * tensor model is not a conversational LLM without its tokenizer, prompt
 * template and generation loop. Those runtimes remain available to model tests
 * and task-specific skills.
 */
object ModelChatRouter {
    data class Route(
        val model: LocalModel?,
        val label: String,
        val chatCapable: Boolean,
        val reason: String
    )

    fun route(models: List<LocalModel>, capabilities: DeviceCapabilities): Route {
        if (models.isEmpty()) {
            return Route(
                model = null,
                label = "Local fallback",
                chatCapable = false,
                reason = "No local model imported. Import a compatible GGUF chat model."
            )
        }

        val maxModelBytes = maxRecommendedModelBytes(capabilities.mode)
        val ggufModels = models
            .filter { it.format.equals("GGUF", ignoreCase = true) }
            .sortedWith(
                compareBy<LocalModel> { if (it.sizeBytes > 0L) it.sizeBytes else Long.MAX_VALUE }
                    .thenBy { it.name.lowercase() }
            )

        // Prefer the smallest known-size model that fits this device tier. If
        // size metadata is unavailable, allow one guarded attempt; the runtime
        // performs an additional available-RAM check before loading it.
        val fitting = ggufModels.firstOrNull {
            it.sizeBytes in 1..maxModelBytes
        }
        if (fitting != null) {
            return Route(
                model = fitting,
                label = "Local GGUF • ${fitting.name}",
                chatCapable = true,
                reason = "Compatible local chat model selected for ${capabilities.mode.name} mode."
            )
        }

        val unknownSize = ggufModels.firstOrNull { it.sizeBytes <= 0L }
        if (unknownSize != null) {
            return Route(
                model = unknownSize,
                label = "Local GGUF • ${unknownSize.name}",
                chatCapable = true,
                reason = "Model size is unknown; the runtime must pass its RAM safety check before loading."
            )
        }

        if (ggufModels.isNotEmpty()) {
            return Route(
                model = null,
                label = "Local model too large",
                chatCapable = false,
                reason = "All imported GGUF models exceed the recommended ${capabilities.mode.name} tier limit. Import a smaller quantized GGUF model."
            )
        }

        val onnx = models.firstOrNull { it.format.equals("ONNX", ignoreCase = true) }
        if (onnx != null) {
            return Route(
                model = onnx,
                label = "ONNX tool model • ${onnx.name}",
                chatCapable = false,
                reason = "ONNX is imported for task-specific inference; generic chat needs a compatible tokenizer and generation runtime."
            )
        }

        val tflite = models.firstOrNull {
            it.format.equals("TFLITE", ignoreCase = true) ||
                it.format.equals("LITE", ignoreCase = true)
        }
        if (tflite != null) {
            return Route(
                model = tflite,
                label = "LiteRT tool model • ${tflite.name}",
                chatCapable = false,
                reason = "LiteRT/TFLite is imported for task-specific inference; generic chat needs a compatible tokenizer and generation runtime."
            )
        }

        val first = models.first()
        return Route(
            model = first,
            label = "Imported model • ${first.name}",
            chatCapable = false,
            reason = "Format ${first.format} is imported, but no compatible chat runtime is bundled for it."
        )
    }

    fun status(context: Context, models: List<LocalModel>): String {
        val route = route(models, DeviceOptimizer.detect(context))
        return "${route.label} • ${route.reason}"
    }

    private fun maxRecommendedModelBytes(mode: PerformanceMode): Long = when (mode) {
        PerformanceMode.LOW_RAM -> 700L * 1024L * 1024L
        PerformanceMode.BALANCED -> 2L * 1024L * 1024L * 1024L
        PerformanceMode.PERFORMANCE -> 4L * 1024L * 1024L * 1024L
    }
}
