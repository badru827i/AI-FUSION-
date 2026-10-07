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
            return Route(null, "Chat Core fallback", false, "No local model imported.")
        }
        val gguf = models.filter { it.format.equals("GGUF", true) }
            .minByOrNull { it.sizeBytes.takeIf { s -> s > 0 } ?: Long.MAX_VALUE }

        if (gguf != null) {
            val maxBytes = when (capabilities.mode) {
                PerformanceMode.LOW_RAM -> 700L * 1024 * 1024
                PerformanceMode.BALANCED -> 2L * 1024 * 1024 * 1024
                PerformanceMode.PERFORMANCE -> 4L * 1024 * 1024 * 1024
            }
            if (gguf.sizeBytes <= 0L || gguf.sizeBytes <= maxBytes) {
                return Route(
                    gguf,
                    "Local GGUF • ${gguf.name}",
                    true,
                    "GGUF is the preferred local chat runtime for this device tier."
                )
            }
        }

        val onnx = models.firstOrNull { it.format.equals("ONNX", true) }
        if (onnx != null) return Route(onnx, "ONNX model • ${onnx.name}", false,
            "Imported ONNX is available for inference tools, but generic chat tokenization cannot be assumed.")

        val tflite = models.firstOrNull { it.format.equals("TFLITE", true) }
        if (tflite != null) return Route(tflite, "LiteRT/TFLite model • ${tflite.name}", false,
            "Imported LiteRT/TFLite is available for inference tools, but generic chat tokenization cannot be assumed.")

        return Route(models.first(), "Imported model • ${models.first().name}", false,
            "Format imported, but no safe generic chat runtime is available.")
    }

    fun status(context: Context, models: List<LocalModel>): String {
        val route = route(models, DeviceOptimizer.detect(context))
        return route.label + " • " + route.reason
    }
}
