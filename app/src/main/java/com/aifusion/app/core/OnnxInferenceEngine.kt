package com.aifusion.app.core

import android.content.Context
import android.net.Uri
import android.os.Build
import ai.onnxruntime.OnnxJavaType
import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import ai.onnxruntime.TensorInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.FloatBuffer

data class OnnxInferenceResult(
    val accelerator: String,
    val inputName: String,
    val inputShape: LongArray,
    val outputCount: Int,
    val outputSummary: String
)

object OnnxInferenceEngine {
    suspend fun smokeTest(
        context: Context,
        uri: Uri,
        modelName: String,
        capabilities: DeviceCapabilities
    ): OnnxInferenceResult? = withContext(Dispatchers.IO) {
        val modelFile = copyToCache(context, uri, modelName) ?: return@withContext null
        val threads = when (capabilities.mode) {
            PerformanceMode.LOW_RAM -> capabilities.cpuCores.coerceAtMost(2)
            PerformanceMode.BALANCED -> capabilities.cpuCores.coerceAtMost(4)
            PerformanceMode.PERFORMANCE -> capabilities.cpuCores.coerceAtMost(8)
        }.coerceAtLeast(1)

        val env = OrtEnvironment.getEnvironment()
        var session: OrtSession? = null
        var usedAccelerator = "CPU"
        try {
            val options = OrtSession.SessionOptions().apply {
                setOptimizationLevel(OrtSession.SessionOptions.OptLevel.ALL_OPT)
                setIntraOpNumThreads(threads)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                runCatching { options.addNnapi() }
                    .onSuccess { usedAccelerator = "NNAPI / accelerator-capable" }
                    .onFailure { usedAccelerator = "CPU" }
            }
            session = env.createSession(modelFile.absolutePath, options)
            val input = session.inputInfo.entries.firstOrNull()
                ?: return@withContext null
            val info = input.value.info as? TensorInfo
                ?: return@withContext null
            val shape = info.getShape()
            val count = info.getNumElements()
            if (info.type != OnnxJavaType.FLOAT || count <= 0L || count > 1_000_000L || shape.any { it <= 0L }) {
                return@withContext OnnxInferenceResult(
                    accelerator = usedAccelerator,
                    inputName = input.key,
                    inputShape = shape,
                    outputCount = session.outputInfo.size,
                    outputSummary = "Runtime loaded; generic smoke input skipped for non-FLOAT or dynamic/large input."
                )
            }

            val tensor = OnnxTensor.createTensor(
                env,
                FloatBuffer.wrap(FloatArray(count.toInt())),
                shape
            )
            tensor.use { inputTensor ->
                session.run(mapOf(input.key to inputTensor)).use { result ->
                    val first = result.getOrNull(0)
                    val summary = if (first != null) first.info.toString() else "No output"
                    OnnxInferenceResult(
                        accelerator = usedAccelerator,
                        inputName = input.key,
                        inputShape = shape,
                        outputCount = result.size(),
                        outputSummary = summary
                    )
                }
            }
        } catch (_: Throwable) {
            null
        } finally {
            session?.close()
            modelFile.delete()
        }
    }

    private fun copyToCache(context: Context, uri: Uri, modelName: String): File? {
        val safe = modelName.replace(Regex("[^A-Za-z0-9._-]"), "_").ifBlank { "model.onnx" }
        val file = File(context.cacheDir, "onnx_${safe}")
        return runCatching {
            context.contentResolver.openInputStream(uri)?.use { input ->
                file.outputStream().use { output -> input.copyTo(output) }
            } ?: return null
            file
        }.getOrNull()
    }
}
