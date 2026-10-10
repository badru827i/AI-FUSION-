package com.aifusion.app.core

import android.content.Context

object ModelChatRouter {
    data class Route(
        val model: LocalModel?,
        val label: String,
        val chatCapable: Boolean,
        val reason: String
    )

    fun route(models: List<LocalModel>, capabilities: DeviceCapabilities): Route {
        if (models.isEmpty()) {
            return Route(null, "Chat Core fallback", false, "Tiada model lokal diimport.")
        }

        val maxBytes = maxModelBytes(capabilities)
        val gguf = models.asSequence()
            .filter { it.format.equals("GGUF", true) }
            .filter { it.sizeBytes <= 0L || it.sizeBytes <= maxBytes }
            .minByOrNull { it.sizeBytes.takeIf { size -> size > 0L } ?: Long.MAX_VALUE }

        if (gguf != null) {
            return Route(
                gguf,
                "Local GGUF • ${gguf.name}",
                true,
                "Model GGUF dipilih untuk penjanaan chat lokal; had memori ikut profil peranti."
            )
        }

        // ONNX can be routed to Chat Core, but the actual engine must verify the
        // graph signature. Not every .onnx file is a text-generating language model.
        val onnx = models.asSequence()
            .filter { it.format.equals("ONNX", true) }
            .filter { it.sizeBytes <= 0L || it.sizeBytes <= maxBytes }
            .minByOrNull { it.sizeBytes.takeIf { size -> size > 0L } ?: Long.MAX_VALUE }

        if (onnx != null) {
            return Route(
                onnx,
                "ONNX Chat candidate • ${onnx.name}",
                true,
                "Chat Core akan menguji input/output teks ONNX. Model LLM input_ids/logits memerlukan tokenizer dan runtime ONNX Runtime GenAI."
            )
        }

        val tflite = models.asSequence()
            .filter { it.format.equals("TFLITE", true) }
            .minByOrNull { it.sizeBytes.takeIf { size -> size > 0L } ?: Long.MAX_VALUE }

        if (tflite != null) {
            return Route(
                tflite,
                "LiteRT/TFLite model • ${tflite.name}",
                false,
                "Runtime TFLite tersedia untuk inferens umum, tetapi adapter penjanaan chat belum tersedia."
            )
        }

        val first = models.first()
        return Route(
            first,
            "Imported model • ${first.name}",
            false,
            if (first.sizeBytes > maxBytes) {
                "Model melebihi had saiz profil peranti (${maxBytes / (1024L * 1024L)} MB)."
            } else {
                "Format ini belum mempunyai adapter chat langsung."
            }
        )
    }

    fun status(context: Context, models: List<LocalModel>): String {
        val route = route(models, DeviceOptimizer.detect(context))
        return route.label + " • " + route.reason
    }

    private fun maxModelBytes(capabilities: DeviceCapabilities): Long = when (capabilities.mode) {
        PerformanceMode.LOW_RAM -> 700L * 1024 * 1024
        PerformanceMode.BALANCED -> 2L * 1024 * 1024 * 1024
        PerformanceMode.PERFORMANCE -> 4L * 1024 * 1024 * 1024
    }
}
