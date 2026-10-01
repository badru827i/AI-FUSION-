package com.aifusion.app.core

data class ChatMessage(
    val id: Long,
    val fromUser: Boolean,
    val text: String
)

data class ChatSession(
    val id: Long,
    val title: String,
    val messages: List<ChatMessage>
)

enum class ChatLanguage {
    BM, ENGLISH, MIXED
}

fun detectLanguage(text: String): ChatLanguage {
    val lower = text.lowercase()
    val bmMarkers = listOf("saya", "awak", "yang", "dan", "untuk", "macam mana", "boleh", "tak", "apa", "dengan")
    val enMarkers = listOf("the", "you", "what", "how", "can", "and", "for", "with", "please", "why")
    val words = Regex("[a-zA-Z]+").findAll(lower).map { it.value }.toSet()
    fun hit(marker: String): Boolean {
        return if (marker.contains(" ")) lower.contains(marker) else words.contains(marker)
    }
    val bmHits = bmMarkers.count { hit(it) }
    val enHits = enMarkers.count { hit(it) }
    return when {
        bmHits > 0 && enHits > 0 -> ChatLanguage.MIXED
        bmHits > enHits -> ChatLanguage.BM
        else -> ChatLanguage.ENGLISH
    }
}

fun localResponse(
    query: String,
    capabilities: DeviceCapabilities,
    language: ChatLanguage
): String {
    val deviceLine = " RAM " + capabilities.ramMb + " MB, CPU " + capabilities.cpuCores +
        " cores, " + DeviceOptimizer.label(capabilities.mode) + " mode."

    val coreLine = when {
        query.contains("ram", ignoreCase = true) || query.contains("memory", ignoreCase = true) ->
            "Smart Device Engine is routing this workload using the detected RAM and CPU budget."
        query.contains("model", ignoreCase = true) || query.contains("onnx", ignoreCase = true) ||
            query.contains("tflite", ignoreCase = true) ->
            "Model routing is currently tier-based; a large local model is avoided on Low RAM devices."
        query.contains("research", ignoreCase = true) || query.contains("kajian", ignoreCase = true) ->
            "Open Research Core for parallel web research, source links and the live monitor."
        else ->
            "The lightweight local chat fallback is active. Full model inference remains separate from the UI core."
    }

    return when (language) {
        ChatLanguage.BM ->
            "Chat Core 3.0 aktif. " + coreLine + deviceLine
        ChatLanguage.ENGLISH ->
            "Chat Core 3.0 is active. " + coreLine + deviceLine
        ChatLanguage.MIXED ->
            "Chat Core 3.0 aktif. " + coreLine + " Device: " + deviceLine
    }
}
