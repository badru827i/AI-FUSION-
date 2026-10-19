package com.aifusion.app

import android.app.ActivityManager
import android.content.Context
import java.io.File
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

enum class Text3DQuality { FAST, BALANCED, HIGH_DETAIL }
enum class Text3DStyle { REALISTIC, STYLIZED, LOW_POLY, MECHANICAL_CAD }
enum class Text3DTexture { NONE, PBR, GENERATED }
enum class Text3DFormat { OBJ }

data class Text3DRequest(
    val prompt: String,
    val quality: Text3DQuality = Text3DQuality.HIGH_DETAIL,
    val detailPercent: Int = 100,
    val style: Text3DStyle = Text3DStyle.MECHANICAL_CAD,
    val texture: Text3DTexture = Text3DTexture.NONE,
    val symmetry: Boolean = true,
    val format: Text3DFormat = Text3DFormat.OBJ
)

data class Text3DResult(
    val file: File,
    val vertexCount: Int,
    val faceCount: Int,
    val engineLabel: String,
    val notice: String
)

/**
 * Contract for a real on-device text-to-3D inference adapter.
 * A model adapter must be added only after its model, runtime, licence and
 * Android memory footprint have been verified. No network API is used here.
 */
interface OnDeviceTextTo3DModel {
    fun isReady(context: Context): Boolean
    fun generate(context: Context, request: Text3DRequest, outputDirectory: File): Text3DResult
}

object LocalTextTo3DEngine {
    private const val MIN_AVAILABLE_RAM_MB = 160L
    private const val MAX_VERTICES = 120_000
    private const val MAX_FACES = 180_000

    @Volatile
    var modelAdapter: OnDeviceTextTo3DModel? = null

    fun validate(request: Text3DRequest): String? {
        if (request.prompt.isBlank()) return "Masukkan arahan model 3D."
        if (request.prompt.length > 2_000) return "Prompt terlalu panjang (maksimum 2,000 aksara)."
        if (request.detailPercent !in 10..100) return "Tahap perincian mesti antara 10 hingga 100."
        if (request.format != Text3DFormat.OBJ) return "Format eksport ini belum disokong."
        return null
    }

    fun generate(context: Context, request: Text3DRequest, outputDirectory: File): Text3DResult {
        validate(request)?.let { error(it) }

        val adapter = modelAdapter
        if (adapter != null && adapter.isReady(context)) {
            return adapter.generate(context, request, outputDirectory)
        }

        val manager = context.getSystemService(ActivityManager::class.java)
            ?: error("Pengurus memori Android tidak tersedia.")
        val memory = ActivityManager.MemoryInfo()
        manager.getMemoryInfo(memory)
        val availableMb = memory.availMem / (1024L * 1024L)
        if (availableMb < MIN_AVAILABLE_RAM_MB) {
            error("RAM tersedia hanya $availableMb MB. Tutup aplikasi lain dan cuba lagi.")
        }

        // Honest offline fallback: deterministic geometry, not neural generative inference.
        val prompt = request.prompt.lowercase()
        val isCar = listOf("car", "kereta", "sport", "spoiler", "automotif").any(prompt::contains)
        val mesh = Mesh()
        if (isCar) {
            buildCar(mesh, request)
        } else {
            buildPromptPrimitive(mesh, prompt, request)
        }
        require(mesh.vertices.size <= MAX_VERTICES) { "Model melebihi had vertex yang dibenarkan." }
        require(mesh.faces.size <= MAX_FACES) { "Model melebihi had muka yang dibenarkan." }
        require(mesh.vertices.isNotEmpty() && mesh.faces.isNotEmpty()) { "Geometri yang dijana kosong." }
        validateMesh(mesh)

        if (!outputDirectory.exists() && !outputDirectory.mkdirs()) {
            error("Tidak dapat menyediakan folder projek 3D.")
        }
        val output = File(outputDirectory, "AI_FUSION_3D_${System.currentTimeMillis()}.obj")
        output.bufferedWriter().use { writer ->
            writer.appendLine("# AI-FUSION local procedural geometry fallback")
            writer.appendLine("# Text-to-3D neural model: not installed")
            writer.appendLine("# Prompt: ${request.prompt.replace("\n", " ").take(500)}")
            writer.appendLine("o AI_FUSION_Model")
            mesh.vertices.forEach { v -> writer.appendLine("v ${v.x} ${v.y} ${v.z}") }
            mesh.faces.forEach { face -> writer.appendLine("f ${face.joinToString(" ")}") }
        }
        return Text3DResult(
            file = output,
            vertexCount = mesh.vertices.size,
            faceCount = mesh.faces.size,
            engineLabel = "Local geometry fallback",
            notice = "Fail OBJ dijana sepenuhnya pada telefon. Ini geometri prosedural; model neural text-to-3D belum dipasang."
        )
    }

    private data class V3(val x: Float, val y: Float, val z: Float)
    private class Mesh {
        val vertices = mutableListOf<V3>()
        val faces = mutableListOf<IntArray>()

        fun box(cx: Float, cy: Float, cz: Float, sx: Float, sy: Float, sz: Float) {
            val start = vertices.size + 1
            vertices += listOf(
                V3(cx-sx,cy-sy,cz-sz), V3(cx+sx,cy-sy,cz-sz),
                V3(cx+sx,cy+sy,cz-sz), V3(cx-sx,cy+sy,cz-sz),
                V3(cx-sx,cy-sy,cz+sz), V3(cx+sx,cy-sy,cz+sz),
                V3(cx+sx,cy+sy,cz+sz), V3(cx-sx,cy+sy,cz+sz)
            )
            faces += listOf(
                intArrayOf(start,start+1,start+2,start+3),
                intArrayOf(start+4,start+7,start+6,start+5),
                intArrayOf(start,start+4,start+5,start+1),
                intArrayOf(start+1,start+5,start+6,start+2),
                intArrayOf(start+2,start+6,start+7,start+3),
                intArrayOf(start+4,start,start+3,start+7)
            )
        }

        fun wheel(cx: Float, cy: Float, cz: Float, radius: Float, width: Float, segments: Int) {
            val start = vertices.size + 1
            repeat(segments) { i ->
                val a = 2.0 * PI * i / segments
                val x = cx + cos(a).toFloat() * radius
                val z = cz + sin(a).toFloat() * radius
                vertices += V3(x, cy - width / 2f, z)
                vertices += V3(x, cy + width / 2f, z)
            }
            repeat(segments) { i ->
                val next = (i + 1) % segments
                faces += intArrayOf(start + i * 2, start + next * 2, start + next * 2 + 1, start + i * 2 + 1)
            }
        }
    }

    private fun buildCar(mesh: Mesh, request: Text3DRequest) {
        val detailScale = request.detailPercent / 100f
        val wheelSegments = when (request.quality) {
            Text3DQuality.FAST -> 8
            Text3DQuality.BALANCED -> 16
            Text3DQuality.HIGH_DETAIL -> (16 + (detailScale * 16).toInt()).coerceIn(16, 32)
        }
        // Main chassis, raised cabin, front/rear bumpers and rear spoiler.
        mesh.box(0f, 0.35f, 0f, 2.0f, 0.34f, 0.82f)
        mesh.box(-0.12f, 0.88f, 0f, 0.98f, 0.28f, 0.61f)
        mesh.box(2.02f, 0.25f, 0f, 0.18f, 0.18f, 0.76f)
        mesh.box(-2.02f, 0.25f, 0f, 0.18f, 0.18f, 0.76f)
        mesh.box(-1.45f, 0.78f, 0f, 0.12f, 0.58f, 0.65f)
        mesh.box(-1.45f, 1.36f, 0f, 0.14f, 0.06f, 0.78f)
        val wheelY = 0.12f
        val wheelZ = if (request.symmetry) listOf(-0.83f, 0.83f) else listOf(-0.83f)
        for (x in listOf(-1.25f, 1.25f)) {
            wheelZ.forEach { z ->
                mesh.wheel(x, wheelY, z, 0.38f, 0.18f, wheelSegments)
            }
        }
        // A higher detail setting adds restrained CAD panel strips, not image textures.
        if (request.detailPercent >= 60) {
            mesh.box(0f, 0.70f, 0.825f, 1.25f, 0.025f, 0.018f)
            mesh.box(0f, 0.70f, -0.825f, 1.25f, 0.025f, 0.018f)
        }
    }

    private fun buildPromptPrimitive(mesh: Mesh, prompt: String, request: Text3DRequest) {
        when {
            "cylinder" in prompt || "silinder" in prompt -> {
                val segments = if (request.quality == Text3DQuality.HIGH_DETAIL) 32 else 16
                val start = mesh.vertices.size + 1
                repeat(segments) { i ->
                    val a = 2.0 * PI * i / segments
                    val x = cos(a).toFloat()
                    val z = sin(a).toFloat()
                    mesh.vertices += V3(x, -1f, z)
                    mesh.vertices += V3(x, 1f, z)
                }
                repeat(segments) { i ->
                    val n = (i + 1) % segments
                    mesh.faces += intArrayOf(start+i*2,start+n*2,start+n*2+1,start+i*2+1)
                }
            }
            "pyramid" in prompt || "piramid" in prompt -> {
                val start = mesh.vertices.size + 1
                mesh.vertices += listOf(V3(-1f,0f,-1f),V3(1f,0f,-1f),V3(1f,0f,1f),V3(-1f,0f,1f),V3(0f,1.6f,0f))
                mesh.faces += listOf(intArrayOf(start,start+1,start+2,start+3),intArrayOf(start,start+1,start+4),intArrayOf(start+1,start+2,start+4),intArrayOf(start+2,start+3,start+4),intArrayOf(start+3,start,start+4))
            }
            else -> mesh.box(0f, 0f, 0f, 1f, 1f, 1f)
        }
    }

    private fun validateMesh(mesh: Mesh) {
        mesh.faces.forEach { face ->
            require(face.size >= 3) { "Muka mesh tidak sah." }
            require(face.all { it in 1..mesh.vertices.size }) { "Indeks muka merujuk vertex yang tidak wujud." }
        }
        mesh.vertices.forEach { v ->
            require(v.x.isFinite() && v.y.isFinite() && v.z.isFinite()) { "Mesh mengandungi koordinat tidak sah." }
        }
    }
}
