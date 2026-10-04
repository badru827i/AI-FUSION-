package com.aifusion.app

import android.content.Context
import java.io.File

/**
 * Central routing point between Chat Core and local AI-FUSION tools.
 * Tools stay modular so the assistant can call them without moving chat
 * history or user data to a server.
 */
object FusionToolRouter {
    enum class Tool {
        CHAT, RESEARCH, THREE_D, IMAGE_CREATE, VIDEO_CREATE, VISION_OCR, FILES, COMPRESSION, DEVICE, MODELS
    }

    fun detectTool(prompt: String): Tool {
        val q = prompt.lowercase()
        return when {
            listOf("3d", "model 3d", "obj", "fbx", "bina model", "buat model", "reka bentuk 3d", "design 3d")
                .any { q.contains(it) } -> Tool.THREE_D
            listOf("research", "kajian", "cari sumber", "semak sumber", "fact check")
                .any { q.contains(it) } -> Tool.RESEARCH
            listOf("buat gambar", "hasilkan gambar", "generate image", "create image", "lukis gambar", "image generation")
                .any { q.contains(it) } -> Tool.IMAGE_CREATE
            listOf("buat video", "hasilkan video", "generate video", "create video", "video generation")
                .any { q.contains(it) } -> Tool.VIDEO_CREATE
            listOf("ocr", "baca gambar", "analisis gambar", "vision")
                .any { q.contains(it) } -> Tool.VISION_OCR
            listOf("compress", "compression", "mampat", "zip")
                .any { q.contains(it) } -> Tool.COMPRESSION
            else -> Tool.CHAT
        }
    }

    fun build3D(context: Context, prompt: String): File {
        return Local3DBuilder.build(
            prompt = prompt,
            directory = File(context.cacheDir, "fusion3d")
        )
    }
}
