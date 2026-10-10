package com.aifusion.app.core

import android.app.ActivityManager
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

data class OnnxChatAttempt(
    val text: String?,
    val accelerator: String,
    val reason: String
) {
    val success: Boolean get() = !text.isNullOrBlank()
}

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
                    val summary = "Inference smoke test passed; outputs=" + result.size()
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


    /**
     * Attempts a real text-in/text-out chat pass.
     * Token-ID/logits LLM exports need tokenizer + generation runtime and are rejected safely.
     */
    suspend fun generateChat(
        context: Context,
        model: LocalModel,
        prompt: String,
        capabilities: DeviceCapabilities
    ): OnnxChatAttempt = withContext(Dispatchers.IO) {
        if (prompt.isBlank()) return@withContext OnnxChatAttempt(null, "CPU", "Prompt kosong.")

        val uri = Uri.parse(model.uri)
        val modelFile = copyForChat(context, uri, model)
            ?: return@withContext OnnxChatAttempt(null, "CPU", "Fail ONNX tidak dapat dibaca atau disalin.")

        val maxBytes = when (capabilities.mode) {
            PerformanceMode.LOW_RAM -> 700L * 1024 * 1024
            PerformanceMode.BALANCED -> 2L * 1024 * 1024 * 1024
            PerformanceMode.PERFORMANCE -> 4L * 1024 * 1024 * 1024
        }
        val fileSize = modelFile.length()
        if (fileSize > maxBytes) {
            return@withContext OnnxChatAttempt(
                null, "CPU",
                "Model terlalu besar untuk \${DeviceOptimizer.label(capabilities.mode)} mode (had fail \${maxBytes / (1024L * 1024L)} MB)."
            )
        }

        val memoryInfo = ActivityManager.MemoryInfo()
        runCatching { context.getSystemService(ActivityManager::class.java)?.getMemoryInfo(memoryInfo) }
        if (memoryInfo.availMem > 0L && fileSize > memoryInfo.availMem * 0.55f) {
            return@withContext OnnxChatAttempt(null, "CPU", "RAM tersedia tidak mencukupi untuk memuatkan model ini dengan selamat.")
        }

        val threads = when (capabilities.mode) {
            PerformanceMode.LOW_RAM -> capabilities.cpuCores.coerceAtMost(2)
            PerformanceMode.BALANCED -> capabilities.cpuCores.coerceAtMost(4)
            PerformanceMode.PERFORMANCE -> capabilities.cpuCores.coerceAtMost(8)
        }.coerceAtLeast(1)

        val options = OrtSession.SessionOptions().apply {
            setOptimizationLevel(OrtSession.SessionOptions.OptLevel.ALL_OPT)
            setIntraOpNumThreads(threads)
            setInterOpNumThreads(1)
        }
        var session: OrtSession? = null
        try {
            session = OrtEnvironment.getEnvironment().createSession(modelFile.absolutePath, options)
            val inputs = session.inputInfo.entries
            if (inputs.size != 1) {
                return@withContext OnnxChatAttempt(
                    null, "CPU",
                    "Graf ONNX mempunyai \${inputs.size} input. Chat memerlukan satu input STRING; model LLM input_ids/logits memerlukan runtime GenAI."
                )
            }

            val input = inputs.first()
            val inputInfo = input.value.info as? TensorInfo
                ?: return@withContext OnnxChatAttempt(null, "CPU", "Input ONNX bukan tensor yang disokong.")
            if (inputInfo.type != OnnxJavaType.STRING) {
                return@withContext OnnxChatAttempt(
                    null, "CPU",
                    "Input model ialah \${inputInfo.type}, bukan STRING. Model LLM token-ID memerlukan tokenizer dan ONNX Runtime GenAI."
                )
            }

            val shape = inputInfo.getShape()
            if (shape.size != 1 || (shape[0] > 1L && shape[0] != -1L)) {
                return@withContext OnnxChatAttempt(null, "CPU", "Bentuk input STRING tidak serasi; runtime chat memerlukan tensor teks satu dimensi.")
            }

            val output = session.outputInfo.entries.firstOrNull { entry ->
                (entry.value.info as? TensorInfo)?.type == OnnxJavaType.STRING
            } ?: return@withContext OnnxChatAttempt(
                null, "CPU",
                "Graf ini tiada output STRING. Model logit/token-ID memerlukan runtime penjanaan LLM khusus."
            )

            val env = OrtEnvironment.getEnvironment()
            val inputTensor = OnnxTensor.createTensor(
                env, arrayOf(prompt.trim().take(6000)), longArrayOf(1L)
            )
            inputTensor.use { tensor ->
                session.run(mapOf(input.key to tensor)).use { result ->
                    val outputTensor = result.get(output.key) as? OnnxTensor
                        ?: return@withContext OnnxChatAttempt(null, "CPU", "Output ONNX tidak dapat dibaca sebagai tensor.")
                    val generated = flattenStringOutput(outputTensor.value).trim()
                    if (generated.isBlank()) {
                        OnnxChatAttempt(null, "CPU", "Model ONNX dimuatkan tetapi tidak menghasilkan teks.")
                    } else {
                        OnnxChatAttempt(generated, "CPU", "Inferens ONNX text-in/text-out berjaya.")
                    }
                }
            }
        } catch (error: Throwable) {
            val detail = error.message.orEmpty().replace("\n", " ").replace("\r", " ").take(180)
            OnnxChatAttempt(
                null, "CPU",
                if (detail.isBlank()) "Inferens ONNX gagal; semak operator dan bentuk tensor model."
                else "Inferens ONNX gagal: $detail"
            )
        } finally {
            session?.close()
            options.close()
        }
    }

    private fun flattenStringOutput(value: Any?): String = when (value) {
        is String -> value
        is Array<*> -> value.joinToString(separator = "") { flattenStringOutput(it) }
        is Iterable<*> -> value.joinToString(separator = "") { flattenStringOutput(it) }
        else -> ""
    }

    private fun copyForChat(context: Context, uri: Uri, model: LocalModel): File? {
        val root = context.getExternalFilesDir("models") ?: File(context.filesDir, "models")
        val directory = File(root, "onnx-chat")
        if (!directory.exists() && !directory.mkdirs()) return null

        val safeName = model.name.replace(Regex("[^A-Za-z0-9._-]"), "_").ifBlank { "model.onnx" }
        val suffix = Integer.toHexString(uri.toString().hashCode())
        val target = File(directory, "\${safeName}_$suffix")
        if (target.isFile && target.length() > 0L &&
            (model.sizeBytes <= 0L || model.sizeBytes == target.length())
        ) return target

        val stream = runCatching {
            if (uri.scheme.equals("file", ignoreCase = true)) uri.path?.let { File(it).inputStream() }
            else context.contentResolver.openInputStream(uri)
        }.getOrNull() ?: return null

        return runCatching {
            stream.use { input -> target.outputStream().use { output -> input.copyTo(output) } }
            target.takeIf { it.isFile && it.length() > 0L }
        }.getOrNull()
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
