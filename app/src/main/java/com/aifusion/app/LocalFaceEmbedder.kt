package com.aifusion.app

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import java.io.Closeable
import java.io.File
import kotlin.math.sqrt

/**
 * MobileFaceNet-style embedding runner.
 *
 * Expected compatible model:
 * input  [1,112,112,3], float32 RGB normalised to [-1,1]
 * output [1,D], float32 face embedding
 *
 * A pair-comparison model or a model with a different preprocessing contract is rejected.
 */
internal class LocalFaceEmbedder(context: Context) : Closeable {
    private val appContext = context.applicationContext
    private val modelFile = File(appContext.noBackupFilesDir, "jarvis-models/face-embedder.tflite")
    private var interpreter: Interpreter? = null
    var outputDimension: Int = 0
        private set

    val isReady: Boolean
        get() = interpreter != null

    init {
        modelFile.parentFile?.mkdirs()
        runCatching { loadInstalledModel() }
    }

    @Synchronized
    fun installModel(uri: Uri) {
        val temp = File(modelFile.parentFile, "face-embedder.new")
        appContext.contentResolver.openInputStream(uri)?.use { input ->
            temp.outputStream().use { output -> input.copyTo(output); output.fd.sync() }
        } ?: error("The selected model cannot be opened.")

        var trial: Interpreter? = null
        try {
            trial = Interpreter(temp, Interpreter.Options().setNumThreads(2))
            validate(trial)
            trial.close()
            trial = null

            interpreter?.close()
            interpreter = null
            if (modelFile.exists() && !modelFile.delete()) error("Cannot replace the previous model.")
            if (!temp.renameTo(modelFile)) error("Cannot install the model in private storage.")
            loadInstalledModel()
        } catch (error: Exception) {
            trial?.close()
            temp.delete()
            if (error is IllegalArgumentException || error is IllegalStateException) throw error
            throw IllegalArgumentException("The selected file is not a compatible face-embedding model.", error)
        }
    }

    @Synchronized
    fun loadInstalledModel() {
        if (!modelFile.isFile || modelFile.length() < 1024L) {
            interpreter?.close()
            interpreter = null
            outputDimension = 0
            return
        }
        val candidate = Interpreter(modelFile, Interpreter.Options().setNumThreads(2))
        try {
            validate(candidate)
        } catch (error: Exception) {
            candidate.close()
            throw error
        }
        interpreter?.close()
        interpreter = candidate
    }

    @Synchronized
    fun embed(faceCrop: Bitmap): FloatArray {
        val runner = interpreter ?: error("Import a compatible face-embedding model first.")
        val resized = Bitmap.createScaledBitmap(faceCrop, INPUT_SIZE, INPUT_SIZE, true)
        val input = Array(1) { Array(INPUT_SIZE) { Array(INPUT_SIZE) { FloatArray(3) } } }

        for (y in 0 until INPUT_SIZE) {
            for (x in 0 until INPUT_SIZE) {
                val pixel = resized.getPixel(x, y)
                input[0][y][x][0] = (((pixel shr 16) and 0xff) / 127.5f) - 1f
                input[0][y][x][1] = (((pixel shr 8) and 0xff) / 127.5f) - 1f
                input[0][y][x][2] = ((pixel and 0xff) / 127.5f) - 1f
            }
        }
        if (resized !== faceCrop) resized.recycle()

        val output = Array(1) { FloatArray(outputDimension) }
        runner.run(input, output)
        return normalise(output[0])
    }

    private fun validate(candidate: Interpreter) {
        val input = candidate.getInputTensor(0)
        val inputShape = input.shape()
        require(
            input.dataType() == DataType.FLOAT32 &&
                inputShape.contentEquals(intArrayOf(1, INPUT_SIZE, INPUT_SIZE, 3))
        ) {
            "Unsupported model input. Required: [1,112,112,3] float32 RGB."
        }
        val output = candidate.getOutputTensor(0)
        val outputShape = output.shape()
        require(
            output.dataType() == DataType.FLOAT32 &&
                outputShape.size == 2 &&
                outputShape[0] == 1 &&
                outputShape[1] in 32..4096
        ) {
            "Unsupported model output. Required: [1,D] float32 embedding."
        }
        outputDimension = outputShape[1]
    }

    override fun close() {
        synchronized(this) {
            interpreter?.close()
            interpreter = null
            outputDimension = 0
        }
    }

    companion object {
        private const val INPUT_SIZE = 112

        fun normalise(vector: FloatArray): FloatArray {
            val norm = sqrt(vector.sumOf { it.toDouble() * it.toDouble() }).toFloat()
            require(norm.isFinite() && norm > 1e-8f) { "Face model returned an invalid embedding." }
            return FloatArray(vector.size) { vector[it] / norm }
        }

        fun cosineSimilarity(a: FloatArray, b: FloatArray): Float {
            if (a.size != b.size || a.isEmpty()) return -1f
            var dot = 0.0
            var normA = 0.0
            var normB = 0.0
            for (i in a.indices) {
                dot += a[i].toDouble() * b[i].toDouble()
                normA += a[i].toDouble() * a[i].toDouble()
                normB += b[i].toDouble() * b[i].toDouble()
            }
            if (normA <= 1e-12 || normB <= 1e-12) return -1f
            return (dot / (sqrt(normA) * sqrt(normB))).toFloat()
        }

        fun averageEmbeddings(samples: List<FloatArray>): FloatArray {
            require(samples.isNotEmpty())
            val dimension = samples.first().size
            require(samples.all { it.size == dimension }) { "Inconsistent embedding dimensions." }
            val mean = FloatArray(dimension)
            samples.forEach { sample ->
                for (i in 0 until dimension) mean[i] += sample[i] / samples.size
            }
            return normalise(mean)
        }
    }
}
