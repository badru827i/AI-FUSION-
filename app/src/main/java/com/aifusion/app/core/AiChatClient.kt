package com.aifusion.app.core

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

object AiChatClient {
    private const val ENDPOINT = "https://api.openai.com/v1/responses"

    suspend fun generateReply(
        apiKey: String,
        model: String,
        conversation: List<ChatMessage>
    ): String = withContext(Dispatchers.IO) {
        require(apiKey.isNotBlank()) { "Missing API key" }

        val input = JSONArray()
        conversation
            .filter { it.text.isNotBlank() }
            .takeLast(24)
            .forEach { message ->
                input.put(
                    JSONObject().apply {
                        put("role", if (message.fromUser) "user" else "assistant")
                        put("content", message.text)
                    }
                )
            }

        if (input.length() == 0) throw IOException("No chat messages to send")

        val payload = JSONObject().apply {
            put("model", model.trim().ifBlank { ApiKeyStore.DEFAULT_MODEL })
            put(
                "instructions",
                "You are AI-FUSION Assistant. Be helpful, concise and practical. " +
                    "Match the user's language (Malay, English, or mixed). " +
                    "For Android, AI, Blender, Roblox and coding questions, give clear step-by-step guidance. " +
                    "Do not claim to have performed actions that you did not perform."
            )
            put("input", input)
        }

        val connection = (URL(ENDPOINT).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 15000
            readTimeout = 60000
            doOutput = true
            setRequestProperty("Authorization", "Bearer ${apiKey.trim()}")
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Accept", "application/json")
        }

        try {
            connection.outputStream.use {
                it.write(payload.toString().toByteArray(Charsets.UTF_8))
            }

            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val body = stream?.bufferedReader()?.use { it.readText() }.orEmpty()

            if (code !in 200..299) {
                val message = runCatching {
                    JSONObject(body)
                        .optJSONObject("error")
                        ?.optString("message")
                        .orEmpty()
                }.getOrNull().orEmpty()

                throw IOException(
                    "OpenAI HTTP $code" +
                        if (message.isNotBlank()) ": $message" else ""
                )
            }

            extractOutputText(body).ifBlank {
                throw IOException("OpenAI returned no output text")
            }
        } finally {
            connection.disconnect()
        }
    }

    private fun extractOutputText(body: String): String {
        val root = JSONObject(body)
        val output = root.optJSONArray("output") ?: JSONArray()

        return buildString {
            for (i in 0 until output.length()) {
                val item = output.optJSONObject(i) ?: continue
                val content = item.optJSONArray("content") ?: continue

                for (j in 0 until content.length()) {
                    val part = content.optJSONObject(j) ?: continue
                    if (part.optString("type") == "output_text") {
                        val text = part.optString("text")
                        if (text.isNotBlank()) {
                            if (isNotEmpty()) append("\n")
                            append(text)
                        }
                    }
                }
            }
        }.trim()
    }
}
