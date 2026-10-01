package com.aifusion.app.core

import android.content.Context
import android.util.Base64

class LocalChatStore(context: Context) {
    private val prefs = context.getSharedPreferences("ai_fusion_chat_store", Context.MODE_PRIVATE)

    fun save(session: ChatSession) {
        val rows = session.messages.joinToString("\n") { message ->
            listOf(
                message.id.toString(),
                if (message.fromUser) "1" else "0",
                Base64.encodeToString(message.text.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
            ).joinToString("|")
        }
        prefs.edit()
            .putString("chat_" + session.id, CompressionEngine.compress(rows))
            .putString("title_" + session.id, session.title.ifBlank { "New chat" })
            .putString("chat_index", updatedIndex(session.id).joinToString(","))
            .apply()
    }

    fun load(id: Long): ChatSession? {
        val encoded = prefs.getString("chat_" + id, null) ?: return null
        val rows = CompressionEngine.decompress(encoded)
        if (rows.isBlank()) {
            return ChatSession(id, prefs.getString("title_" + id, "Chat") ?: "Chat", emptyList())
        }

        val messages = rows.lineSequence().mapNotNull { row ->
            val parts = row.split("|", limit = 3)
            if (parts.size != 3) return@mapNotNull null
            val text = try {
                Base64.decode(parts[2], Base64.NO_WRAP).toString(Charsets.UTF_8)
            } catch (_: Exception) {
                return@mapNotNull null
            }
            ChatMessage(
                id = parts[0].toLongOrNull() ?: return@mapNotNull null,
                fromUser = parts[1] == "1",
                text = text
            )
        }.toList()

        return ChatSession(
            id = id,
            title = prefs.getString("title_" + id, "Chat") ?: "Chat",
            messages = messages
        )
    }

    fun listSessions(): List<Pair<Long, String>> {
        return updatedIndex().mapNotNull { id ->
            val title = prefs.getString("title_" + id, null) ?: return@mapNotNull null
            id to title
        }
    }

    fun delete(id: Long) {
        prefs.edit()
            .remove("chat_" + id)
            .remove("title_" + id)
            .putString("chat_index", updatedIndex(idToRemove = id).joinToString(","))
            .apply()
    }

    fun clearAll() {
        prefs.edit().clear().apply()
    }

    private fun updatedIndex(addedId: Long? = null, idToRemove: Long? = null): List<Long> {
        val current = prefs.getString("chat_index", "")
            .orEmpty()
            .split(",")
            .mapNotNull { it.toLongOrNull() }
            .toMutableList()

        idToRemove?.let { current.remove(it) }
        addedId?.let {
            current.remove(it)
            current.add(0, it)
        }
        return current.distinct()
    }
}
