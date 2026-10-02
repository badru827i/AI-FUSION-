package com.aifusion.app.core

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

data class CloudAccount(
    val uid: String,
    val displayName: String,
    val email: String,
    val photoUrl: String?
)

class FirebaseAccountManager {
    private val auth: FirebaseAuth
        get() = FirebaseAuth.getInstance()

    private val db: FirebaseFirestore
        get() = FirebaseFirestore.getInstance()

    fun currentAccount(): CloudAccount? {
        val user = auth.currentUser ?: return null
        return CloudAccount(
            uid = user.uid,
            displayName = user.displayName ?: "Google user",
            email = user.email.orEmpty(),
            photoUrl = user.photoUrl?.toString()
        )
    }

    suspend fun signInWithGoogleIdToken(idToken: String): CloudAccount {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        val result = auth.signInWithCredential(credential).await()
        val user = result.user ?: error("Firebase did not return a user")
        return CloudAccount(
            uid = user.uid,
            displayName = user.displayName ?: "Google user",
            email = user.email.orEmpty(),
            photoUrl = user.photoUrl?.toString()
        )
    }

    suspend fun syncSession(session: ChatSession) {
        val user = auth.currentUser ?: return
        val messages = session.messages.map { message ->
            mapOf(
                "id" to message.id,
                "fromUser" to message.fromUser,
                "text" to message.text
            )
        }

        db.collection("users")
            .document(user.uid)
            .collection("chats")
            .document(session.id.toString())
            .set(
                mapOf(
                    "title" to session.title,
                    "updatedAt" to System.currentTimeMillis(),
                    "messages" to messages
                )
            )
            .await()

        db.collection("users")
            .document(user.uid)
            .set(
                mapOf(
                    "displayName" to (user.displayName ?: ""),
                    "email" to (user.email ?: ""),
                    "photoUrl" to (user.photoUrl?.toString() ?: ""),
                    "lastSeenAt" to System.currentTimeMillis()
                )
            )
            .await()
    }

    suspend fun loadSessions(): List<ChatSession> {
        val user = auth.currentUser ?: return emptyList()
        val snapshot = db.collection("users")
            .document(user.uid)
            .collection("chats")
            .get()
            .await()

        return snapshot.documents.mapNotNull { doc ->
            val id = doc.id.toLongOrNull() ?: return@mapNotNull null
            val title = doc.getString("title").orEmpty().ifBlank { "AI-FUSION Chat" }
            val messages = (doc.get("messages") as? List<*>)?.mapNotNull { raw ->
                val row = raw as? Map<*, *> ?: return@mapNotNull null
                val messageId = (row["id"] as? Number)?.toLong() ?: return@mapNotNull null
                val fromUser = row["fromUser"] as? Boolean ?: false
                val text = row["text"] as? String ?: ""
                ChatMessage(messageId, fromUser, text)
            }.orEmpty()
            ChatSession(id, title, messages)
        }.sortedByDescending { it.id }
    }

    suspend fun deleteSession(id: Long) {
        val user = auth.currentUser ?: return
        db.collection("users")
            .document(user.uid)
            .collection("chats")
            .document(id.toString())
            .delete()
            .await()
    }

    fun signOut() {
        auth.signOut()
    }
}
