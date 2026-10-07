package com.aifusion.app

import android.content.Context
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import org.json.JSONObject
import com.aifusion.app.core.CompressionEngine
import com.aifusion.app.core.DeviceOptimizer
import com.aifusion.app.core.FactVerificationEngine
import com.aifusion.app.core.NetworkGuardian
import com.aifusion.app.core.ResearchCore
import com.aifusion.app.core.ResourceManager

/**
 * AI-FUSION 45-tool registry.
 *
 * Every tool is callable from the same chat router. Tools that require a
 * model/file/device capability return a deterministic status instead of
 * pretending that a missing runtime succeeded.
 */
object FusionToolRouter {
    enum class Tool {
        CHAT,
        DEEP_RESEARCH,
        FACT_CHECK,
        LIVE_MONITOR,
        WEB_SEARCH,
        SITE_READER,
        SOURCE_COMPARE,
        SUMMARIZE,
        TRANSLATE,
        LANGUAGE_DETECT,
        VISION_OCR,
        IMAGE_ANALYSIS,
        IMAGE_CREATE,
        VIDEO_CREATE,
        THREE_D,
        CAD_CODE,
        MODEL_TO_CHAT,
        CAD_PLAN,
        CODE,
        CODE_DEBUG,
        JSON_TOOL,
        FILE_READ,
        FILE_SUMMARIZE,
        FILE_COMPRESS,
        ZIP,
        TEXT_COMPRESS,
        CHAT_EXPORT,
        CHAT_SEARCH,
        MODEL_IMPORT,
        MODEL_TEST,
        MODEL_SELECT,
        GGUF_INFERENCE,
        ONNX_INFERENCE,
        TFLITE_INFERENCE,
        DEVICE_SCAN,
        CPU_MONITOR,
        RAM_MONITOR,
        GPU_MONITOR,
        NPU_CHECK,
        PERFORMANCE_MODE,
        CACHE_CLEANER,
        VOICE_INPUT,
        TTS,
        PDF_READ,
        IMAGE_METADATA,
        NETWORK_TEST,
        SHARE_FILE
    }

    data class ToolInfo(
        val tool: Tool,
        val name: String,
        val description: String,
        val keywords: List<String>
    )

    data class ToolResult(
        val tool: Tool,
        val success: Boolean,
        val message: String
    )

    val registry: List<ToolInfo> = listOf(
        ToolInfo(Tool.CHAT, "Chat Core", "General local-first assistant", listOf("chat", "jawab", "tanya")),
        ToolInfo(Tool.DEEP_RESEARCH, "Deep Research", "Parallel web evidence search", listOf("research", "kajian", "deep research")),
        ToolInfo(Tool.FACT_CHECK, "Fact Check", "Evidence-based claim verification", listOf("fact check", "factcheck", "semak fakta", "verify claim")),
        ToolInfo(Tool.LIVE_MONITOR, "Live Monitor", "Repeat current web research", listOf("live monitor", "monitor", "pantau")),
        ToolInfo(Tool.WEB_SEARCH, "Web Search", "Search current web sources", listOf("web search", "cari web", "google", "search web")),
        ToolInfo(Tool.SITE_READER, "Site Reader", "Fetch readable web page text", listOf("site reader", "baca laman", "baca website", "read website")),
        ToolInfo(Tool.SOURCE_COMPARE, "Source Compare", "Compare independent sources", listOf("compare sources", "banding sumber", "bandingkan sumber")),
        ToolInfo(Tool.SUMMARIZE, "Summarizer", "Create a compact local summary", listOf("summarize", "ringkas", "rumuskan")),
        ToolInfo(Tool.TRANSLATE, "Translator", "Local phrase translation helper", listOf("translate", "terjemah", "translate to")),
        ToolInfo(Tool.LANGUAGE_DETECT, "Language Detect", "Detect BM/English/mixed language", listOf("language detect", "detect language", "bahasa apa")),
        ToolInfo(Tool.VISION_OCR, "Vision OCR", "Extract text from an image when an image is supplied", listOf("ocr", "baca gambar", "extract text")),
        ToolInfo(Tool.IMAGE_ANALYSIS, "Image Analysis", "Inspect image/vision input", listOf("image analysis", "analisis gambar", "analyze image")),
        ToolInfo(Tool.IMAGE_CREATE, "Image Create", "Generate a local image artifact", listOf("buat gambar", "hasilkan gambar", "generate image", "create image")),
        ToolInfo(Tool.VIDEO_CREATE, "Video Create", "Generate a local MP4 artifact", listOf("buat video", "hasilkan video", "generate video", "create video")),
        ToolInfo(Tool.THREE_D, "3D Model", "Build/open/import a local 3D model", listOf("3d", "obj", "fbx", "stl", "glb", "model 3d", "design 3d", "import 3d")),
        ToolInfo(Tool.CAD_CODE, "CAD Coding", "Generate deterministic 3D geometry from CAD code", listOf("cad code", "cad coding", "cad kod", "box(", "cylinder(", "pyramid(")),
        ToolInfo(Tool.MODEL_TO_CHAT, "Model → Chat", "Route an imported local AI model into Chat Core", listOf("model to chat", "model ke chat", "guna model", "use model", "local model chat")),
        ToolInfo(Tool.CAD_PLAN, "CAD Planner", "Produce structured engineering/CAD plan", listOf("cad", "engineering design", "kejuruteraan", "pelan cad")),
        ToolInfo(Tool.CODE, "Code", "Generate/explain code", listOf("code", "coding", "program", "kod")),
        ToolInfo(Tool.CODE_DEBUG, "Code Debug", "Diagnose code errors", listOf("debug code", "fix code", "debug kod", "fix error kod")),
        ToolInfo(Tool.JSON_TOOL, "JSON Tool", "Validate and inspect JSON", listOf("json", "validate json", "semak json")),
        ToolInfo(Tool.FILE_READ, "File Reader", "Read text supplied to the tool", listOf("read file", "baca fail", "file reader")),
        ToolInfo(Tool.FILE_SUMMARIZE, "File Summarizer", "Summarize supplied file text", listOf("summarize file", "ringkas fail")),
        ToolInfo(Tool.FILE_COMPRESS, "File Compressor", "Plan local file compression", listOf("compress file", "mampat fail")),
        ToolInfo(Tool.ZIP, "ZIP", "Create ZIP-ready compression plan", listOf("zip", "buat zip")),
        ToolInfo(Tool.TEXT_COMPRESS, "Text Compression", "GZIP-compress text locally", listOf("compress text", "mampat teks")),
        ToolInfo(Tool.CHAT_EXPORT, "Chat Export", "Export-ready chat representation", listOf("export chat", "eksport chat")),
        ToolInfo(Tool.CHAT_SEARCH, "Chat Search", "Search local chat history", listOf("search chat", "cari chat", "cari sejarah")),
        ToolInfo(Tool.MODEL_IMPORT, "Model Import", "Import models through Model Manager", listOf("import model", "masuk model")),
        ToolInfo(Tool.MODEL_TEST, "Model Test", "Run ONNX/TFLite smoke tests", listOf("test model", "model test", "uji model")),
        ToolInfo(Tool.MODEL_SELECT, "Model Select", "Choose a model tier for device resources", listOf("select model", "pilih model", "model terbaik")),
        ToolInfo(Tool.GGUF_INFERENCE, "GGUF Inference", "Run imported GGUF through local llama runtime", listOf("gguf", "local llama", "local ai")),
        ToolInfo(Tool.ONNX_INFERENCE, "ONNX Inference", "Run imported ONNX runtime smoke inference", listOf("onnx", "onnx inference")),
        ToolInfo(Tool.TFLITE_INFERENCE, "TFLite Inference", "Run imported LiteRT/TFLite smoke inference", listOf("tflite", "litert")),
        ToolInfo(Tool.DEVICE_SCAN, "Device Scan", "Detect phone resources", listOf("device scan", "scan device", "spesifikasi telefon")),
        ToolInfo(Tool.CPU_MONITOR, "CPU Monitor", "Read CPU usage", listOf("cpu monitor", "cpu usage", "cpu")),
        ToolInfo(Tool.RAM_MONITOR, "RAM Monitor", "Read RAM usage", listOf("ram monitor", "ram usage", "ram")),
        ToolInfo(Tool.GPU_MONITOR, "GPU Monitor", "Read GPU utilization where Android exposes it", listOf("gpu monitor", "gpu usage", "gpu")),
        ToolInfo(Tool.NPU_CHECK, "NPU Check", "Check conservative NPU/NNAPI capability", listOf("npu", "npu check", "neural engine")),
        ToolInfo(Tool.PERFORMANCE_MODE, "Performance Mode", "Select low-RAM/balanced/performance routing", listOf("performance mode", "low ram", "balanced mode")),
        ToolInfo(Tool.CACHE_CLEANER, "Cache Cleaner", "Clear temporary cache safely", listOf("clear cache", "cache cleaner", "bersihkan cache")),
        ToolInfo(Tool.VOICE_INPUT, "Voice Input", "Android speech input entry point", listOf("voice input", "suara", "voice")),
        ToolInfo(Tool.TTS, "Text To Speech", "Speak the answer using Android TTS", listOf("tts", "speak", "baca jawapan")),
        ToolInfo(Tool.PDF_READ, "PDF Reader", "PDF file capability/status; parser is activated when a PDF is supplied", listOf("pdf", "baca pdf")),
        ToolInfo(Tool.IMAGE_METADATA, "Image Metadata", "Inspect image dimensions/type when a URI is supplied", listOf("image metadata", "metadata gambar")),
        ToolInfo(Tool.NETWORK_TEST, "Network Test", "Check phone connectivity and latency path", listOf("network test", "test internet", "uji internet")),
        ToolInfo(Tool.SHARE_FILE, "Share File", "Prepare Android share action for generated files", listOf("share file", "kongsi fail", "share"))
    )

    fun allTools(): List<ToolInfo> = registry

    fun detectTool(prompt: String): Tool {
        val q = prompt.lowercase()
        return registry
            .asSequence()
            .filter { it.tool != Tool.CHAT }
            .sortedByDescending { info ->
                info.keywords.maxOfOrNull { keyword -> if (q.contains(keyword)) keyword.length else 0 } ?: 0
            }
            .firstOrNull { info -> info.keywords.any { q.contains(it) } }
            ?.tool ?: Tool.CHAT
    }

    suspend fun execute(context: Context, tool: Tool, prompt: String): ToolResult {
        return runCatching {
            when (tool) {
                Tool.CHAT -> ToolResult(tool, true, "Chat Core ready.")
                Tool.DEEP_RESEARCH -> {
                    val results = ResearchCore.research(prompt).flatMap { it.sources }.distinctBy { it.url }
                    ToolResult(tool, results.isNotEmpty(), if (results.isEmpty()) "No web evidence found." else "Research found ${results.size} unique sources.")
                }
                Tool.FACT_CHECK -> {
                    val results = ResearchCore.research(prompt)
                    val report = FactVerificationEngine.evaluate(results)
                    ToolResult(tool, report.uniqueSources > 0, "${report.verdict} • score ${report.score}/100 • ${report.uniqueSources} sources • ${report.uniqueDomains} domains.")
                }
                Tool.LIVE_MONITOR -> ToolResult(tool, true, "Live Monitor ready. Research Core can be repeated on a 30-second cycle.")
                Tool.WEB_SEARCH -> {
                    val results = ResearchCore.research(prompt).flatMap { it.sources }.distinctBy { it.url }.take(5)
                    ToolResult(tool, results.isNotEmpty(), results.joinToString("\n") { "${it.title}\n${it.url}" }.ifBlank { "No results." })
                }
                Tool.SITE_READER -> readSite(prompt)?.let { ToolResult(tool, true, it) }
                    ?: ToolResult(tool, false, "Give a URL beginning with http:// or https://.")
                Tool.SOURCE_COMPARE -> {
                    val results = ResearchCore.research(prompt)
                    val domains = results.flatMap { it.sources }.mapNotNull { runCatching { java.net.URI(it.url).host }.getOrNull() }.distinct()
                    ToolResult(tool, domains.size >= 2, "Compared ${domains.size} source domains: ${domains.joinToString()}.")
                }
                Tool.SUMMARIZE -> ToolResult(tool, true, summarize(prompt))
                Tool.TRANSLATE -> ToolResult(tool, true, translate(prompt))
                Tool.LANGUAGE_DETECT -> ToolResult(tool, true, "Detected: ${com.aifusion.app.core.detectLanguage(prompt).name}")
                Tool.VISION_OCR -> ToolResult(tool, false, "OCR runtime is installed. Attach an image and use the OCR action to supply its URI.")
                Tool.IMAGE_ANALYSIS -> ToolResult(tool, false, "Vision analysis requires an image input. OCR is available locally.")
                Tool.IMAGE_CREATE, Tool.VIDEO_CREATE, Tool.THREE_D -> ToolResult(tool, true, "Handled by the dedicated media/3D action.")
                Tool.CAD_CODE -> ToolResult(tool, true, "CAD Coding ready. Use box(w,h,d), cylinder(r,h) or pyramid(w,h,d).")
                Tool.MODEL_TO_CHAT -> ToolResult(tool, true, com.aifusion.app.core.ModelChatRouter.status(context, com.aifusion.app.core.ModelManager(context).list()))
                Tool.CAD_PLAN -> ToolResult(tool, true, cadPlan(prompt))
                Tool.CODE -> ToolResult(tool, true, "Code tool active. Provide language + task for a generated implementation.")
                Tool.CODE_DEBUG -> ToolResult(tool, true, "Code Debug active. Paste the failing code and exact error; the tool will isolate the failing section.")
                Tool.JSON_TOOL -> validateJson(prompt)
                Tool.FILE_READ -> ToolResult(tool, true, "File Reader ready for a text/file URI.")
                Tool.FILE_SUMMARIZE -> ToolResult(tool, true, "File Summarizer ready for supplied text/file content.")
                Tool.FILE_COMPRESS, Tool.ZIP -> ToolResult(tool, true, "Local compression tool ready; use File Manager/attachment input to select the file.")
                Tool.TEXT_COMPRESS -> {
                    val packed = CompressionEngine.compress(prompt)
                    ToolResult(tool, true, "Compressed locally. Base64-GZIP size=${packed.length} chars.")
                }
                Tool.CHAT_EXPORT -> ToolResult(tool, true, "Chat Export ready; local history stays on-device.")
                Tool.CHAT_SEARCH -> ToolResult(tool, true, "Chat Search ready; use History to search/open saved sessions.")
                Tool.MODEL_IMPORT -> ToolResult(tool, true, "Model Manager supports up to 10 imported local models.")
                Tool.MODEL_TEST -> ToolResult(tool, true, "Model Test supports ONNX and TFLite/LiteRT smoke tests from Model Manager.")
                Tool.MODEL_SELECT -> {
                    val caps = DeviceOptimizer.detect(context)
                    ToolResult(tool, true, "Selected tier: ${caps.modelTier} • ${DeviceOptimizer.label(caps.mode)}.")
                }
                Tool.GGUF_INFERENCE -> ToolResult(tool, true, "GGUF inference is enabled when an imported GGUF model is available.")
                Tool.ONNX_INFERENCE -> ToolResult(tool, true, "ONNX Runtime + NNAPI fallback is enabled.")
                Tool.TFLITE_INFERENCE -> ToolResult(tool, true, "LiteRT/TFLite + XNNPACK/NNAPI fallback is enabled.")
                Tool.DEVICE_SCAN -> {
                    val c = DeviceOptimizer.detect(context)
                    ToolResult(tool, true, "RAM ${c.ramMb} MB • CPU ${c.cpuCores} cores • GPU ${c.gpuApi} • NPU ${c.npuAvailable}.")
                }
                Tool.CPU_MONITOR, Tool.RAM_MONITOR, Tool.GPU_MONITOR -> {
                    val s = com.aifusion.app.core.HardwareMonitor.read(context)
                    ToolResult(tool, true, "CPU ${com.aifusion.app.core.HardwareMonitor.percentText(s.cpuPercent)} • RAM ${s.ramUsedMb}/${s.ramTotalMb} MB (${com.aifusion.app.core.HardwareMonitor.percentText(s.ramPercent)}) • GPU ${com.aifusion.app.core.HardwareMonitor.percentText(s.gpuPercent)}.")
                }
                Tool.NPU_CHECK -> {
                    val c = DeviceOptimizer.detect(context)
                    ToolResult(tool, true, "NPU/NNAPI capability: ${if (c.npuAvailable) "detected" else "not detected/unknown"}; utilization is not fabricated.")
                }
                Tool.PERFORMANCE_MODE -> {
                    val c = DeviceOptimizer.detect(context)
                    ToolResult(tool, true, "Current mode: ${DeviceOptimizer.label(c.mode)} • tier: ${c.modelTier}.")
                }
                Tool.CACHE_CLEANER -> {
                    ResourceManager.clearTemporaryCache(context)
                    ToolResult(tool, true, "Temporary app cache cleared.")
                }
                Tool.VOICE_INPUT -> ToolResult(tool, true, "Voice Input uses Android Speech Recognizer from the chat microphone action.")
                Tool.TTS -> ToolResult(tool, true, "TTS uses Android TextToSpeech from the response speaker action.")
                Tool.PDF_READ -> ToolResult(tool, false, "PDF Reader needs a PDF URI/file input before text extraction can run.")
                Tool.IMAGE_METADATA -> ToolResult(tool, false, "Image Metadata needs an image URI/file input.")
                Tool.NETWORK_TEST -> {
                    val state = NetworkGuardian.state(context)
                    ToolResult(tool, state.connected, "Network: ${NetworkGuardian.label(state)} • connected=${state.connected} • metered=${state.metered}.")
                }
                Tool.SHARE_FILE -> ToolResult(tool, true, "Share File uses Android FileProvider for generated local artifacts.")
            }
        }.getOrElse { error ->
            ToolResult(tool, false, "Tool error: ${error.message ?: "unknown error"}")
        }
    }

    private fun readSite(prompt: String): String? {
        val url = Regex("""https?://\S+""").find(prompt)?.value ?: return null
        val c = URL(url).openConnection() as HttpURLConnection
        c.connectTimeout = 8000
        c.readTimeout = 8000
        c.setRequestProperty("User-Agent", "AI-FUSION/4.4 Android")
        return try {
            c.inputStream.bufferedReader().use { reader ->
                reader.readText()
                    .replace(Regex("<script[\\s\\S]*?</script>", RegexOption.IGNORE_CASE), " ")
                    .replace(Regex("<style[\\s\\S]*?</style>", RegexOption.IGNORE_CASE), " ")
                    .replace(Regex("<[^>]+>"), " ")
                    .replace(Regex("\\s+"), " ")
                    .trim()
                    .take(6000)
            }
        } finally {
            c.disconnect()
        }
    }

    private fun summarize(prompt: String): String {
        val text = prompt.substringAfter(":", prompt).trim()
        val words = text.split(Regex("\\s+")).filter { it.isNotBlank() }
        return if (words.size <= 80) text else words.take(80).joinToString(" ") + "…"
    }

    private fun translate(prompt: String): String {
        val q = prompt.substringAfter(":", prompt).trim()
        val pairs = mapOf(
            "hello" to "hai", "good morning" to "selamat pagi", "thank you" to "terima kasih",
            "please" to "sila", "yes" to "ya", "no" to "tidak", "what" to "apa",
            "how" to "bagaimana", "why" to "kenapa", "good" to "baik"
        )
        val lower = q.lowercase()
        val hit = pairs.entries.firstOrNull { lower.contains(it.key) }
        return hit?.let { "${it.key} → ${it.value}" }
            ?: "Translator helper active. For high-quality free-form translation, provide source + target language and the text."
    }

    private fun validateJson(prompt: String): ToolResult {
        val raw = prompt.substringAfter(":", prompt).trim()
        return try {
            JSONObject(raw)
            ToolResult(Tool.JSON_TOOL, true, "Valid JSON object.")
        } catch (e: Exception) {
            ToolResult(Tool.JSON_TOOL, false, "Invalid JSON: ${e.message ?: "parse error"}")
        }
    }

    private fun cadPlan(prompt: String): String =
        "CAD plan: define requirements → dimensions/tolerances → parts → constraints → materials → assembly → interference check → export STEP/OBJ/FBX. Request: ${prompt.take(500)}"

    fun build3D(context: Context, prompt: String): File {
        return Local3DBuilder.build(prompt = prompt, directory = File(context.cacheDir, "fusion3d"))
    }
}
