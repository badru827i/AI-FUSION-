package com.aifusion.app.core

import android.net.Uri
import java.util.Locale

enum class AiModelFormat {
    GGUF, GGML, ONNX, TFLITE, EXECUTORCH, SAFETENSORS, PYTORCH, TORCHSCRIPT, TENSORFLOW_SAVED_MODEL, MLC, UNKNOWN
}

data class ModelFormatInfo(
    val format: AiModelFormat,
    val extension: String,
    val runtime: String,
    val directAndroidLoad: Boolean,
    val note: String
)

object ModelFormatDetector {
    fun detect(uri: Uri, displayName: String? = null): ModelFormatInfo {
        val name = (displayName ?: uri.lastPathSegment ?: "").lowercase(Locale.US)
        val ext = name.substringAfterLast('.', "")
        return when {
            name.endsWith(".gguf") -> info(AiModelFormat.GGUF, ext, "llama.cpp", true, "Quantized LLM; Q2/Q3/Q4/Q5/Q6/Q8 variants.")
            name.endsWith(".ggml") -> info(AiModelFormat.GGML, ext, "llama.cpp", false, "Legacy llama.cpp format; convert to GGUF when possible.")
            name.endsWith(".onnx") -> info(AiModelFormat.ONNX, ext, "ONNX Runtime", true, "ONNX Runtime Android is bundled; Model Manager can run a smoke inference with CPU/NNAPI fallback.")
            name.endsWith(".tflite") || name.endsWith(".lite") -> info(AiModelFormat.TFLITE, ext, "LiteRT / TensorFlow Lite", true, "LiteRT Interpreter is bundled; Model Manager can run a smoke inference with NNAPI/XNNPACK fallback.")
            name.endsWith(".pte") -> info(AiModelFormat.EXECUTORCH, ext, "ExecuTorch", false, "Format detected and importable; matching ExecuTorch runtime is not bundled yet.")
            name.endsWith(".safetensors") -> info(AiModelFormat.SAFETENSORS, ext, "Importer / converter", false, "Weights container; not a universal Android inference format by itself.")
            name.endsWith(".pth") -> info(AiModelFormat.PYTORCH, ext, "PyTorch importer", false, "Usually needs conversion to a mobile runtime format.")
            name.endsWith(".pt") -> info(AiModelFormat.TORCHSCRIPT, ext, "PyTorch / TorchScript", false, "May be TorchScript or a training checkpoint; inspect metadata before loading.")
            name.endsWith(".pb") || name.endsWith(".pbtxt") -> info(AiModelFormat.TENSORFLOW_SAVED_MODEL, ext, "TensorFlow importer", false, "TensorFlow graph/checkpoint data; conversion is normally required on Android.")
            name.contains("mlc") || name.endsWith(".json") && name.contains("mlc") -> info(AiModelFormat.MLC, ext, "MLC LLM", true, "MLC package/config; requires matching MLC runtime and compiled artifacts.")
            else -> info(AiModelFormat.UNKNOWN, ext, "None", false, "Unknown format; do not load until inspected.")
        }
    }

    private fun info(format: AiModelFormat, ext: String, runtime: String, direct: Boolean, note: String) =
        ModelFormatInfo(format, ext, runtime, direct, note)
}
