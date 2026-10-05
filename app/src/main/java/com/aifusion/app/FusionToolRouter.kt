package com.aifusion.app

import android.content.Context
import com.aifusion.app.core.FusionToolRegistry
import com.aifusion.app.core.FusionToolSpec
import java.io.File

/**
 * Central routing point between Chat Core and local AI-FUSION tools.
 *
 * The registry owns tool knowledge; this class keeps compatibility with the
 * existing Chat Core while exposing richer routing information.
 */
object FusionToolRouter {
    enum class Tool {
        CHAT,
        RESEARCH,
        THREE_D,
        IMAGE_CREATE,
        VIDEO_CREATE,
        VISION_OCR,
        FILES,
        COMPRESSION,
        DEVICE,
        MODELS,
        CODE,
        VOICE,
        ANDROID,
        WEB_SEARCH,
        FACT_CHECK,
        MULTI_AGENT
    }

    fun detectTool(prompt: String): Tool {
        val spec = FusionToolRegistry.match(prompt) ?: return Tool.CHAT
        return when (spec.id) {
            "deep_research", "research_monitor", "site_reader", "source_compare" -> Tool.RESEARCH
            "fact_check" -> Tool.FACT_CHECK
            "multi_agent" -> Tool.MULTI_AGENT
            "three_d", "three_d_scene" -> Tool.THREE_D
            "image_create", "image_edit", "image_upscale" -> Tool.IMAGE_CREATE
            "video_create", "video_analyze" -> Tool.VIDEO_CREATE
            "vision", "ocr" -> Tool.VISION_OCR
            "files", "pdf", "documents", "data" -> Tool.FILES
            "file_compression", "context_compressor", "ram_manager" -> Tool.COMPRESSION
            "device_info", "battery", "network", "hardware_scheduler" -> Tool.DEVICE
            "model_manager", "quantization", "benchmark", "inference_profiler", "local_ai", "model_router" -> Tool.MODELS
            "code", "json_api", "project_analysis", "build_analysis" -> Tool.CODE
            "voice_input", "voice_output" -> Tool.VOICE
            "app_launcher", "android_intent", "accessibility", "notifications", "clipboard" -> Tool.ANDROID
            "web_search" -> Tool.WEB_SEARCH
            else -> Tool.CHAT
        }
    }

    fun matchSpec(prompt: String): FusionToolSpec? = FusionToolRegistry.match(prompt)

    fun relatedTools(prompt: String, limit: Int = 4): List<FusionToolSpec> =
        FusionToolRegistry.related(prompt, limit)

    fun allTools(): List<FusionToolSpec> = FusionToolRegistry.all

    fun build3D(context: Context, prompt: String): File {
        return Local3DBuilder.build(
            prompt = prompt,
            directory = File(context.cacheDir, "fusion3d")
        )
    }
}
