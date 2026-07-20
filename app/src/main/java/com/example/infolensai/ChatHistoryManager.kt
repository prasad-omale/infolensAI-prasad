package com.example.infolensai

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.UUID

object ChatHistoryManager {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    fun createNewSession(callback: (String) -> Unit) {
        val userId = auth.currentUser?.uid ?: return
        val sessionId = UUID.randomUUID().toString()
        val sessionData = hashMapOf(
            "id" to sessionId,
            "userId" to userId,
            "title" to "New Chat",
            "isFavorite" to false,
            "timestamp" to System.currentTimeMillis()
        )

        db.collection("ChatSessions").document(sessionId)
            .set(sessionData)
            .addOnSuccessListener { callback(sessionId) }
    }

    fun updateSessionTitle(sessionId: String, title: String) {
        db.collection("ChatSessions").document(sessionId)
            .update("title", title)
    }

    fun saveMessage(sessionId: String, message: ChatMessage) {
        val userId = auth.currentUser?.uid ?: return
        val timestamp = System.currentTimeMillis()
        val msgData = hashMapOf(
            "sessionId" to sessionId,
            "userId" to userId,
            "text" to message.text,
            "isUser" to message.isUser,
            "timestamp" to timestamp
        )
        // Note: Bitmap images are not stored in Firestore directly in this implementation.
        // For production, upload to Firebase Storage and store the URL.
        
        db.collection("ChatMessages").add(msgData)
        
        // Update session timestamp so it moves to the top of the recents list
        db.collection("ChatSessions").document(sessionId)
            .update("timestamp", timestamp)
    }

    fun getRecentSessions(callback: (List<ChatSession>) -> Unit) {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            callback(emptyList())
            return
        }

        db.collection("ChatSessions")
            .whereEqualTo("userId", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    android.util.Log.e("ChatHistoryManager", "Listen failed.", error)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val sessions = snapshot.documents.mapNotNull { doc ->
                        val id = doc.getString("id") ?: return@mapNotNull null
                        val title = doc.getString("title") ?: "Untitled"
                        val timestamp = doc.getLong("timestamp") ?: 0L
                        val isFavorite = doc.getBoolean("isFavorite") ?: false
                        ChatSession(id, title, timestamp, isFavorite)
                    }.sortedByDescending { it.timestamp }
                    callback(sessions)
                }
            }
    }

    fun getMessages(sessionId: String, callback: (List<ChatMessage>) -> Unit) {
        db.collection("ChatMessages")
            .whereEqualTo("sessionId", sessionId)
            .get()
            .addOnSuccessListener { documents ->
                val messagesWithTime = documents.map { doc ->
                    val msg = ChatMessage(
                        text = doc.getString("text") ?: "",
                        isUser = doc.getBoolean("isUser") ?: false,
                        image = null // Images not persisted in this step
                    )
                    val ts = doc.getLong("timestamp") ?: 0L
                    Pair(msg, ts)
                }
                
                val sortedMessages = messagesWithTime.sortedBy { it.second }.map { it.first }
                callback(sortedMessages)
            }
    }

    fun deleteSession(sessionId: String, callback: (Boolean) -> Unit) {
        // Delete messages first
        db.collection("ChatMessages")
            .whereEqualTo("sessionId", sessionId)
            .get()
            .addOnSuccessListener { documents ->
                val batch = db.batch()
                for (doc in documents) {
                    batch.delete(doc.reference)
                }
                
                // Then delete session
                batch.delete(db.collection("ChatSessions").document(sessionId))
                
                batch.commit().addOnCompleteListener { task ->
                    callback(task.isSuccessful)
                }
            }
            .addOnFailureListener {
                callback(false)
            }
    }

    fun toggleFavorite(sessionId: String, isFavorite: Boolean, callback: (Boolean) -> Unit) {
        db.collection("ChatSessions").document(sessionId)
            .update("isFavorite", isFavorite)
            .addOnCompleteListener { task ->
                callback(task.isSuccessful)
            }
    }

    fun getFavoriteSessions(callback: (List<ChatSession>) -> Unit) {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            callback(emptyList())
            return
        }

        db.collection("ChatSessions")
            .whereEqualTo("userId", userId)
            .whereEqualTo("isFavorite", true)
            .get()
            .addOnSuccessListener { snapshot ->
                val sessions = snapshot.documents.mapNotNull { doc ->
                    val id = doc.getString("id") ?: return@mapNotNull null
                    val title = doc.getString("title") ?: "Untitled"
                    val timestamp = doc.getLong("timestamp") ?: 0L
                    val isFav = doc.getBoolean("isFavorite") ?: false
                    ChatSession(id, title, timestamp, isFav)
                }.sortedByDescending { it.timestamp }
                callback(sessions)
            }
    }
}
