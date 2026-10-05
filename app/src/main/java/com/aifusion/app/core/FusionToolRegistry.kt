package com.aifusion.app.core

/**
 * Single source of truth for AI-FUSION tools.
 *
 * The registry is intentionally lightweight: only metadata lives here.
 * Tool implementations are loaded/called on demand so the full catalog does
 * not stay resident in RAM.
 */
enum class FusionToolDomain {
    CORE, RESEARCH, MULTIMODAL, FILES, CREATE, DEVELOPER, DEVICE, MODELS, ANDROID
}

enum class FusionToolAvailability {
    LOCAL, NETWORK, PERMISSION, MODEL_REQUIRED
}

data class FusionToolSpec(
    val id: String,
    val name: String,
    val description: String,
    val domain: FusionToolDomain,
    val availability: FusionToolAvailability,
    val triggers: List<String>
)

object FusionToolRegistry {
    val all: List<FusionToolSpec> = listOf(
        FusionToolSpec("chat", "Chat Core", "Reasoning, writing, planning and conversation.", FusionToolDomain.CORE, FusionToolAvailability.LOCAL, listOf("chat", "jawab", "tulis", "terangkan", "plan")),
        FusionToolSpec("tool_router", "Tool Router", "Pilih tool yang paling sesuai dan gabungkan beberapa tool.", FusionToolDomain.CORE, FusionToolAvailability.LOCAL, listOf("tool", "gunakan tool", "automatik")),
        FusionToolSpec("multi_agent", "Multi-Agent Research Team", "Pecahkan kerja kepada specialist agents dan gabungkan hasil.", FusionToolDomain.CORE, FusionToolAvailability.LOCAL, listOf("multi agent", "research team", "team")),
        FusionToolSpec("memory", "Local Memory", "Simpan maklumat penting secara lokal.", FusionToolDomain.CORE, FusionToolAvailability.LOCAL, listOf("memory", "ingat", "simpan")),
        FusionToolSpec("context_compressor", "Context Compressor", "Mampatkan context untuk mengurangkan penggunaan RAM/context.", FusionToolDomain.CORE, FusionToolAvailability.LOCAL, listOf("compress context", "ringkaskan context", "context compression")),
        FusionToolSpec("model_router", "Model Router", "Pilih model kecil atau besar mengikut tugasan dan hardware.", FusionToolDomain.MODELS, FusionToolAvailability.LOCAL, listOf("pilih model", "model router", "model kecil", "model besar")),
        FusionToolSpec("hardware_scheduler", "CPU/GPU/NPU Scheduler", "Rancang workload mengikut accelerator yang tersedia.", FusionToolDomain.DEVICE, FusionToolAvailability.LOCAL, listOf("cpu gpu npu", "scheduler", "accelerator", "hardware")),

        FusionToolSpec("web_search", "Web Search", "Cari maklumat semasa menggunakan Internet peranti.", FusionToolDomain.RESEARCH, FusionToolAvailability.NETWORK, listOf("cari web", "web search", "google", "search web", "cari")),
        FusionToolSpec("deep_research", "Deep Research", "Jalankan beberapa laluan carian secara selari.", FusionToolDomain.RESEARCH, FusionToolAvailability.NETWORK, listOf("deep research", "research", "kajian")),
        FusionToolSpec("fact_check", "Deep Fact Verification", "Semak tuntutan penting merentas sumber.", FusionToolDomain.RESEARCH, FusionToolAvailability.NETWORK, listOf("fact check", "semak fakta", "verify fakta", "pengesahan fakta")),
        FusionToolSpec("research_monitor", "Live Research Monitor", "Pantau perubahan maklumat semasa.", FusionToolDomain.RESEARCH, FusionToolAvailability.NETWORK, listOf("monitor", "pantau", "live research", "update terkini")),
        FusionToolSpec("site_reader", "Web & Site Reader", "Baca dan ringkaskan halaman web.", FusionToolDomain.RESEARCH, FusionToolAvailability.NETWORK, listOf("baca website", "site reader", "baca laman", "web page")),
        FusionToolSpec("source_compare", "Source Compare", "Bandingkan sumber dan asingkan fakta daripada pendapat.", FusionToolDomain.RESEARCH, FusionToolAvailability.NETWORK, listOf("banding sumber", "source compare", "compare sources")),

        FusionToolSpec("vision", "Vision", "Fahami imej, screenshot dan visual.", FusionToolDomain.MULTIMODAL, FusionToolAvailability.LOCAL, listOf("vision", "analisis gambar", "faham gambar")),
        FusionToolSpec("ocr", "OCR", "Ekstrak teks daripada imej secara lokal.", FusionToolDomain.MULTIMODAL, FusionToolAvailability.LOCAL, listOf("ocr", "baca gambar", "extract text")),
        FusionToolSpec("voice_input", "Voice Input", "Tukar suara pengguna kepada teks.", FusionToolDomain.MULTIMODAL, FusionToolAvailability.PERMISSION, listOf("voice", "suara", "cakap", "dengar")),
        FusionToolSpec("voice_output", "Text to Speech", "Baca jawapan AI dengan suara.", FusionToolDomain.MULTIMODAL, FusionToolAvailability.LOCAL, listOf("baca jawapan", "tts", "sebut")),

        FusionToolSpec("files", "Files", "Baca, ringkas dan urus fail pengguna.", FusionToolDomain.FILES, FusionToolAvailability.PERMISSION, listOf("fail", "file", "dokumen")),
        FusionToolSpec("pdf", "PDF Reader", "Baca dan analisis PDF.", FusionToolDomain.FILES, FusionToolAvailability.PERMISSION, listOf("pdf", "baca pdf")),
        FusionToolSpec("documents", "Document Reader", "Analisis DOCX/TXT/Markdown dan format teks.", FusionToolDomain.FILES, FusionToolAvailability.PERMISSION, listOf("docx", "txt", "markdown", "document")),
        FusionToolSpec("data", "CSV/XLSX Analysis", "Analisis data tabular secara lokal.", FusionToolDomain.FILES, FusionToolAvailability.PERMISSION, listOf("csv", "xlsx", "excel", "spreadsheet")),
        FusionToolSpec("file_compression", "File Compression", "Mampatkan fail dan data untuk jimat storage.", FusionToolDomain.FILES, FusionToolAvailability.LOCAL, listOf("zip", "compress file", "mampat fail")),

        FusionToolSpec("image_create", "Image Create", "Jana visual lokal melalui generator yang tersedia.", FusionToolDomain.CREATE, FusionToolAvailability.LOCAL, listOf("buat gambar", "generate image", "create image", "lukis")),
        FusionToolSpec("image_edit", "Image Edit", "Edit/crop/transform imej melalui pipeline media.", FusionToolDomain.CREATE, FusionToolAvailability.LOCAL, listOf("edit gambar", "ubah gambar")),
        FusionToolSpec("image_upscale", "Image Upscale", "Naikkan resolusi imej apabila model/runtime tersedia.", FusionToolDomain.CREATE, FusionToolAvailability.MODEL_REQUIRED, listOf("upscale", "besarkan gambar")),
        FusionToolSpec("video_create", "Video Create", "Jana video lokal melalui encoder/generator yang tersedia.", FusionToolDomain.CREATE, FusionToolAvailability.LOCAL, listOf("buat video", "generate video", "create video")),
        FusionToolSpec("video_analyze", "Video Analysis", "Analisis video/frame apabila input diberikan.", FusionToolDomain.CREATE, FusionToolAvailability.MODEL_REQUIRED, listOf("analisis video", "video analysis")),
        FusionToolSpec("three_d", "3D Model Design", "Bina dan rancang model 3D/CAD.", FusionToolDomain.CREATE, FusionToolAvailability.LOCAL, listOf("3d", "model 3d", "obj", "cad", "design 3d")),
        FusionToolSpec("three_d_scene", "3D Scene & CAD Plan", "Susun objek, ukuran, constraint dan assembly.", FusionToolDomain.CREATE, FusionToolAvailability.LOCAL, listOf("scene 3d", "cad plan", "assembly")),

        FusionToolSpec("code", "Code", "Generate, explain, debug dan refactor code.", FusionToolDomain.DEVELOPER, FusionToolAvailability.LOCAL, listOf("code", "coding", "program", "debug")),
        FusionToolSpec("json_api", "JSON/API", "Bina dan analisis JSON/API payload.", FusionToolDomain.DEVELOPER, FusionToolAvailability.LOCAL, listOf("json", "api", "rest")),
        FusionToolSpec("project_analysis", "Project/File Analysis", "Analisis struktur projek dan fail.", FusionToolDomain.DEVELOPER, FusionToolAvailability.PERMISSION, listOf("projek", "repo", "project analysis")),
        FusionToolSpec("build_analysis", "Build/APK Analysis", "Semak build, dependency dan APK metadata.", FusionToolDomain.DEVELOPER, FusionToolAvailability.LOCAL, listOf("apk", "build", "gradle")),

        FusionToolSpec("model_manager", "Model Manager", "Import dan urus GGUF/GGML/ONNX/TFLite/LiteRT model metadata.", FusionToolDomain.MODELS, FusionToolAvailability.LOCAL, listOf("model manager", "import model", "gguf", "ggml", "onnx", "tflite", "litert")),
        FusionToolSpec("quantization", "Model Quantization", "Rancang/ukur quantized model seperti INT8/INT4/INT2.", FusionToolDomain.MODELS, FusionToolAvailability.MODEL_REQUIRED, listOf("quantize", "quantization", "int4", "int8", "2 bit", "4 bit")),
        FusionToolSpec("benchmark", "Model Benchmark", "Ukur latency, memory dan output runtime.", FusionToolDomain.MODELS, FusionToolAvailability.LOCAL, listOf("benchmark model", "latency", "benchmark")),
        FusionToolSpec("inference_profiler", "Inference Profiler", "Profil inference dan resource usage.", FusionToolDomain.MODELS, FusionToolAvailability.LOCAL, listOf("profile inference", "profiler")),
        FusionToolSpec("local_ai", "Local AI / Offline", "Utamakan pemprosesan pada peranti.", FusionToolDomain.MODELS, FusionToolAvailability.LOCAL, listOf("offline", "local ai", "tanpa internet")),
        FusionToolSpec("ram_manager", "Compression & RAM Manager", "Kawal cache, RAM dan local context.", FusionToolDomain.DEVICE, FusionToolAvailability.LOCAL, listOf("ram", "memory", "cache", "compression")),

        FusionToolSpec("device_info", "Device Information", "Baca RAM, CPU, GPU API dan keupayaan NPU yang boleh didedahkan Android.", FusionToolDomain.DEVICE, FusionToolAvailability.LOCAL, listOf("device info", "spesifikasi telefon", "ram cpu gpu npu")),
        FusionToolSpec("battery", "Battery Monitor", "Pantau status bateri untuk keputusan workload.", FusionToolDomain.DEVICE, FusionToolAvailability.LOCAL, listOf("battery", "bateri")),
        FusionToolSpec("network", "Network Monitor", "Semak Wi-Fi/mobile network dan status rangkaian.", FusionToolDomain.DEVICE, FusionToolAvailability.LOCAL, listOf("network", "wifi", "4g", "5g", "internet")),

        FusionToolSpec("app_launcher", "App Launcher", "Buka aplikasi melalui Android intents apabila app menyediakan intent yang sesuai.", FusionToolDomain.ANDROID, FusionToolAvailability.PERMISSION, listOf("buka app", "open app", "launch app")),
        FusionToolSpec("android_intent", "Android Intents", "Hantar tindakan standard kepada aplikasi Android.", FusionToolDomain.ANDROID, FusionToolAvailability.PERMISSION, listOf("intent", "android action")),
        FusionToolSpec("accessibility", "Accessibility Automation", "Automasi UI melalui AccessibilityService selepas pengguna memberi akses.", FusionToolDomain.ANDROID, FusionToolAvailability.PERMISSION, listOf("tap", "scroll", "klik", "automate app", "accessibility")),
        FusionToolSpec("notifications", "Notification Tools", "Baca/interaksi notifikasi apabila permission tersedia.", FusionToolDomain.ANDROID, FusionToolAvailability.PERMISSION, listOf("notification", "notifikasi")),
        FusionToolSpec("clipboard", "Clipboard", "Baca/tulis clipboard mengikut sekatan Android.", FusionToolDomain.ANDROID, FusionToolAvailability.PERMISSION, listOf("clipboard", "copy", "paste"))
    )

    fun findById(id: String): FusionToolSpec? = all.firstOrNull { it.id == id }

    fun match(prompt: String): FusionToolSpec? {
        val q = prompt.lowercase()
        return all
            .map { tool -> tool to tool.triggers.count { q.contains(it) } }
            .filter { it.second > 0 }
            .maxByOrNull { it.second }
            ?.first
    }

    fun related(prompt: String, limit: Int = 4): List<FusionToolSpec> {
        val q = prompt.lowercase()
        return all
            .map { tool -> tool to tool.triggers.count { q.contains(it) } }
            .filter { it.second > 0 }
            .sortedByDescending { it.second }
            .take(limit)
            .map { it.first }
    }
}
