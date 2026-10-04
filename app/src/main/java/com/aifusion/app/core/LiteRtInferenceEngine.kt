package com.aifusion.app.core

import android.content.Context
import android.net.Uri
import android.os.Build
import org.tensorflow.lite.Interpreter
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder

data class LiteRtInferenceResult(
    val accelerator: String,
    val inputShape: IntArray,
    val outputCount: Int,
    val outputSummary: String
)

object LiteRtInferenceEngine {
    suspend fun smokeTest(
        context: Context,
        uri: Uri,
        modelName: String,
        capabilities: DeviceCapabilities
    ): LiteRtInferenceResult? = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        val file = copyToCache(context, uri, modelName) ?: return@withContext null
        var interpreter: Interpreter? = null
        try {
            val options = Interpreter.Options()
                .setNumThreads(
                    when (capabilities.mode) {
                        PerformanceMode.LOW_RAM -> capabilities.cpuCores.coerceAtMost(2)
                        PerformanceMode.BALANCED -> capabilities.cpuCores.coerceAtMost(4)
                        PerformanceMode.PERFORMANCE -> capabilities.cpuCores.coerceAtMost(8)
                    }.coerceAtLeast(1)
                )
                .setUseXNNPACK(true)

            val accelerator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                runCatching { options.setUseNNAPI(true) }.fold(
                    onSuccess = { "NNAPI / accelerator-capable" },
                    onFailure = { "CPU / XNNPACK" }
                )
            } else {
                "CPU / XNNPACK"
            }

            interpreter = Interpreter(file, options)
            if (interpreter!!.inputTensorCount < 1 || interpreter!!.outputTensorCount < 1) return@withContext null

            val input = interpreter!!.getInputTensor(0)
            val shape = input.shape()
            val elements = shape.fold(1L) { acc, value -> acc * value.toLong() }
            if (shape.any { it <= 0 } || elements <= 0L || elements > 1_000_000L) {
                return@withContext LiteRtInferenceResult(
                    accelerator = accelerator,
                    inputShape = shape,
                    outputCount = interpreter!!.outputTensorCount,
                    outputSummary = "Runtime loaded; generic smoke input skipped for dynamic/large input."
                )
            }

            val outputTensor = interpreter!!.getOutputTensor(0)
            val outputBytes = outputTensor.numBytes().coerceAtLeast(4)
            val inputBuffer = ByteBuffer.allocateDirect(elements.toInt() * 4).order(ByteOrder.nativeOrder())
            repeat(elements.toInt()) { inputBuffer.putFloat(0f) }
            inputBuffer.rewind()
            val outputBuffer = ByteBuffer.allocateDirect(outputBytes).order(ByteOrder.nativeOrder())
            interpreter!!.run(inputBuffer, outputBuffer)

            LiteRtInferenceResult(
                accelerator = accelerator,
                inputShape = shape,
                outputCount = interpreter!!.outputTensorCount,
                outputSummary = "Inference smoke test passed; output bytes=$outputBytes"
            )
        } catch (_: Throwable) {
            null
        } finally {
            interpreter?.close()
            file.delete()
        }
    }

    private fun copyToCache(context: Context, uri: Uri, modelName: String): File? {
        val safe = modelName.replace(Regex("[^A-Za-z0-9._-]"), "_").ifBlank { "model.tflite" }
        val file = File(context.cacheDir, "litert_${safe}")
        return runCatching {
            context.contentResolver.openInputStream(uri)?.use { input ->
                file.outputStream().use { output -> input.copyTo(output) }
            } ?: return null
            file
        }.getOrNull()
    }
}
