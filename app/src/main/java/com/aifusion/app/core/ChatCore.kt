package com.aifusion.app.core

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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

data class ChatCoreResult(
    val answer: String,
    val engine: String,
    val modelName: String? = null,
    val warning: String? = null
) {
    fun displayText(): String = if (warning.isNullOrBlank()) answer else "$answer\n\nNota model: $warning"
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

/** Local-first dispatcher; rules-based fallback is never labelled as neural inference. */
suspend fun generateLocalReply(
    context: Context,
    conversation: List<ChatMessage>,
    models: List<LocalModel>,
    capabilities: DeviceCapabilities,
    network: NetworkState
): ChatCoreResult {
    val query = conversation.lastOrNull { it.fromUser }?.text.orEmpty()
    val route = ModelChatRouter.route(models, capabilities)
    val selected = route.model
    val prompt = buildConversationPrompt(conversation)

    if (selected != null && route.chatCapable) {
        when {
            selected.format.equals("ONNX", ignoreCase = true) -> {
                val attempt = runCatching {
                    OnnxInferenceEngine.generateChat(
                        context = context,
                        model = selected,
                        prompt = prompt,
                        capabilities = capabilities
                    )
                }.getOrNull()
                if (attempt?.success == true) {
                    return ChatCoreResult(
                        answer = attempt.text.orEmpty(),
                        engine = "ONNX Runtime • ${attempt.accelerator}",
                        modelName = selected.name
                    )
                }
                val reason = attempt?.reason ?: "ONNX runtime gagal memulakan inferens."
                return ChatCoreResult(
                    answer = fallbackAnswer(query, capabilities, network, models.size),
                    engine = "Local fallback",
                    modelName = selected.name,
                    warning = reason
                )
            }

            selected.format.equals("GGUF", ignoreCase = true) -> {
                val answer = runCatching {
                    withContext(Dispatchers.IO) {
                        LocalLlamaEngine.generate(
                            context = context,
                            modelUri = Uri.parse(selected.uri),
                            modelName = selected.name,
                            prompt = prompt,
                            capabilities = capabilities
                        )
                    }
                }.getOrNull()
                if (!answer.isNullOrBlank()) {
                    return ChatCoreResult(
                        answer = answer,
                        engine = "Local GGUF • CPU/NEON",
                        modelName = selected.name
                    )
                }
                return ChatCoreResult(
                    answer = fallbackAnswer(query, capabilities, network, models.size),
                    engine = "Local fallback",
                    modelName = selected.name,
                    warning = "GGUF tidak dapat menjana jawapan pada peranti ini; semak saiz model dan RAM."
                )
            }
        }
    }

    return ChatCoreResult(
        answer = fallbackAnswer(query, capabilities, network, models.size),
        engine = "Local fallback",
        warning = selected?.let { route.reason }
    )
}

private fun buildConversationPrompt(conversation: List<ChatMessage>): String {
    val lastUserText = conversation.lastOrNull { it.fromUser }?.text.orEmpty()
    val instruction = when (detectLanguage(lastUserText)) {
        ChatLanguage.BM -> "Anda ialah AI-FUSION Assistant. Jawab dalam Bahasa Melayu Malaysia dengan tepat dan berguna."
        ChatLanguage.ENGLISH -> "You are AI-FUSION Assistant. Answer accurately and helpfully in English."
        ChatLanguage.MIXED -> "You are AI-FUSION Assistant. Match the user's natural mix of Malay and English."
    }
    val turns = conversation.asSequence()
        .filter { it.text.isNotBlank() }
        .takeLast(12)
        .joinToString("\n") { message ->
            val role = if (message.fromUser) "User" else "Assistant"
            "$role: ${message.text.take(1200)}"
        }
    return "$instruction\n\nConversation:\n$turns\nAssistant:".takeLast(12000)
}

private fun fallbackAnswer(
    query: String,
    capabilities: DeviceCapabilities,
    network: NetworkState,
    modelCount: Int
): String = LocalAnswerEngine.answer(
    query = query,
    capabilities = capabilities,
    network = network,
    modelCount = modelCount
)

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
            "Model routing is device-aware. ONNX chat is attempted only when the graph signature matches a text-in/text-out interface."
        query.contains("research", ignoreCase = true) || query.contains("kajian", ignoreCase = true) ->
            "Open Research Core for parallel web research, source links and the live monitor."
        else ->
            "The lightweight local fallback is active. Actual neural inference runs through a compatible imported model."
    }

    return when (language) {
        ChatLanguage.BM -> "Chat Core 3.0 aktif. " + coreLine + deviceLine
        ChatLanguage.ENGLISH -> "Chat Core 3.0 is active. " + coreLine + deviceLine
        ChatLanguage.MIXED -> "Chat Core 3.0 aktif. " + coreLine + " Device: " + deviceLine
    }
}
