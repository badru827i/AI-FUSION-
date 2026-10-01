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
