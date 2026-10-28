package com.aifusion.app.core

/**
 * Lightweight, deterministic specialist routing for AI-FUSION.
 *
 * This chooses a prompt profile only; it does not claim to launch separate agents.
 * Tool execution and web research remain behind their existing app flows.
 */
object AgentTaskRouter {
    enum class Specialist { GENERAL, RESEARCH, DESIGN, CODE, VISION, THREE_D, DEVICE }

    data class Plan(
        val specialist: Specialist,
        val skillName: String,
        val instructions: String,
        val needsLiveResearch: Boolean
    )

    fun route(query: String): Plan {
        val q = query.trim().lowercase()
        val research = containsAny(q, "research", "terkini", "berita terbaru", "latest news", "latest", "current", "sumber rasmi", "fact check", "semak fakta", "cari di web", "search web")
        val design = containsAny(q, "design", "reka bentuk", "ui/ux", "figma", "layout", "glassmorphism", "animasi", "animation", "warna", "typography")
        val code = containsAny(q, "code", "coding", "debug", "compile", "kotlin", "java", "android studio", "router", "bug", "ralat", "baiki")
        val vision = containsAny(q, "ocr", "gambar", "image", "photo", "screenshot", "baca dokumen", "extract text")
        val threeD = containsAny(q, "3d", "obj", "mesh", "cad", "model 3d", "text-to-3d")
        val device = containsAny(q, "cpu", "gpu", "npu", "ram", "onnx", "tflite", "gguf", "model local", "offline", "mini server")

        return when {
            research -> Plan(
                Specialist.RESEARCH, "research",
                "RESEARCH SKILL: Separate verified facts from assumptions. When current information or citations are required, use the app's dedicated Research flow and real sources. Never invent searches, links, dates, citations, or claim web access that did not occur. If no research result is available, explain the limitation and provide a verification plan.",
                true
            )
            design -> Plan(
                Specialist.DESIGN, "design",
                "DESIGN SKILL: Start from user goals and task flow before visuals. Define hierarchy, spacing, typography, color tokens, component states, accessibility, and responsive behavior. Prefer a cohesive, editable design system over decorative effects. For AI-FUSION, favor minimal premium indigo/blue/violet glass surfaces, restrained neon accents, smooth low-cost motion, readable contrast, and Android-friendly layouts. Do not claim to edit Figma or files unless a connected tool actually did so."
            )
            code -> Plan(
                Specialist.CODE, "coding",
                "CODING SKILL: Diagnose the existing implementation before proposing changes. Prefer small, testable patches; preserve existing behavior; identify the exact files and assumptions; add regression tests where feasible. Do not claim a build or test passed unless it was actually run."
            )
            vision -> Plan(
                Specialist.VISION, "vision-files",
                "VISION/FILES SKILL: Only describe image or file content that is actually supplied or accessible. Distinguish OCR output from verified meaning; protect private content and state when extraction is uncertain."
            )
            threeD -> Plan(
                Specialist.THREE_D, "3d-design",
                "3D SKILL: Prefer deterministic on-device geometry and explicit validation when no generative 3D model is available. State mesh/export limitations honestly. Check dimensions, symmetry, triangle count, memory use, and OBJ output before claiming success."
            )
            device -> Plan(
                Specialist.DEVICE, "device-local-ai",
                "DEVICE/LOCAL-AI SKILL: Prefer compatible local inference when available. Check runtime, model format, available RAM, and accelerator support before loading. ONNX/TFLite files are not automatically chat models. Use network only when needed and available; never describe a planned route as completed inference."
            )
            else -> Plan(
                Specialist.GENERAL, "general",
                "GENERAL SKILL: Match the user's language, be practical and accurate, and do not claim actions or capabilities that were not performed."
            )
        }
    }

    private fun containsAny(text: String, vararg terms: String): Boolean =
        terms.any { text.contains(it) }
}
