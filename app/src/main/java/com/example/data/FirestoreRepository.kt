package com.example.data

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class FirestoreRepository {
    private val firestore by lazy {
        try { FirebaseFirestore.getInstance() } catch (e: Exception) { null }
    }
    private val auth by lazy {
        try { FirebaseAuth.getInstance() } catch (e: Exception) { null }
    }

    suspend fun backupProgress(progressList: List<UserProgress>) {
        val user = auth?.currentUser ?: return
        val db = firestore ?: return
        try {
            val batch = db.batch()
            for (progress in progressList) {
                val docRef = db.collection("users").document(user.uid)
                    .collection("progress").document(progress.date)
                batch.set(docRef, progress)
            }
            batch.commit().await()
        } catch (e: Exception) {
            Log.e("FirestoreRepository", "Backup failed", e)
        }
    }
    
    suspend fun fetchProgress(): List<UserProgress> {
        val user = auth?.currentUser ?: return emptyList()
        val db = firestore ?: return emptyList()
        return try {
            val snapshot = db.collection("users").document(user.uid)
                .collection("progress").get().await()
            snapshot.toObjects(UserProgress::class.java)
        } catch (e: Exception) {
            Log.e("FirestoreRepository", "Fetch failed", e)
            emptyList()
        }
    }

    suspend fun fetchPublishedAzkar(category: String): List<Zekr> {
        val db = firestore ?: return emptyList()
        return try {
            db.collection("content").document("azkar").collection("items")
                .whereEqualTo("category", category)
                .whereEqualTo("published", true)
                .get().await()
                .documents
                .sortedBy { it.getLong("order") ?: Long.MAX_VALUE }
                .mapIndexed { index, doc ->
                    Zekr(
                        id = (doc.getLong("id") ?: index.toLong()).toInt(),
                        text = doc.getString("text").orEmpty(),
                        source = doc.getString("source").orEmpty(),
                        fadl = doc.getString("fadl").orEmpty(),
                        repeatCount = (doc.getLong("repeat") ?: 1L).toInt().coerceAtLeast(1),
                        category = category
                    )
                }
                .filter { it.text.isNotBlank() }
        } catch (e: Exception) {
            Log.e("FirestoreRepository", "Content fetch failed", e)
            emptyList()
        }
    }

    fun observePublishedAzkar(category: String): Flow<List<Zekr>> = callbackFlow {
        val db = firestore
        if (db == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        val registration = db.collection("content").document("azkar").collection("items")
            .whereEqualTo("category", category)
            .whereEqualTo("published", true)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("FirestoreRepository", "Realtime content listener failed", error)
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val items = snapshot?.documents.orEmpty()
                    .sortedBy { it.getLong("order") ?: Long.MAX_VALUE }
                    .mapIndexed { index, doc ->
                        Zekr(
                            id = (doc.getLong("id") ?: index.toLong()).toInt(),
                            text = doc.getString("text").orEmpty(),
                            source = doc.getString("source").orEmpty(),
                            fadl = doc.getString("fadl").orEmpty(),
                            repeatCount = (doc.getLong("repeat") ?: 1L).toInt().coerceAtLeast(1),
                            category = category
                        )
                    }
                    .filter { it.text.isNotBlank() }
                trySend(items)
            }
        awaitClose { registration.remove() }
    }
    suspend fun submitFeedback(draft: FeedbackDraft): Result<String> {
        val user = auth?.currentUser ?: return Result.failure(IllegalStateException("AUTH_REQUIRED"))
        val db = firestore ?: return Result.failure(IllegalStateException("FIRESTORE_UNAVAILABLE"))
        return try {
            val ref = db.collection("feedback").document()
            val data = hashMapOf(
                "id" to ref.id,
                "userId" to user.uid,
                "userEmail" to (user.email ?: ""),
                "type" to draft.type,
                "title" to draft.title,
                "message" to draft.message,
                "aiSummary" to draft.aiSummary,
                "aiCategory" to draft.aiCategory,
                "priority" to draft.priority,
                "status" to "new",
                "adminReply" to "",
                "createdAt" to com.google.firebase.firestore.FieldValue.serverTimestamp(),
                "updatedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
            )
            db.runBatch { batch ->
                batch.set(ref, data)
                batch.set(db.collection("users").document(user.uid).collection("feedback").document(ref.id), data)
            }.await()
            Result.success(ref.id)
        } catch (e: Exception) {
            Log.e("FirestoreRepository", "Feedback submission failed", e)
            Result.failure(e)
        }
    }

    fun observeMyFeedback(): Flow<List<FeedbackItem>> = callbackFlow {
        val user = auth?.currentUser
        val db = firestore
        if (user == null || db == null) { close(); return@callbackFlow }
        val registration = db.collection("feedback").whereEqualTo("userId", user.uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                val items = snapshot?.documents.orEmpty().map { doc ->
                    FeedbackItem(
                        id = doc.id,
                        type = doc.getString("type").orEmpty(),
                        title = doc.getString("title").orEmpty(),
                        message = doc.getString("message").orEmpty(),
                        aiSummary = doc.getString("aiSummary").orEmpty(),
                        status = doc.getString("status") ?: "new",
                        adminReply = doc.getString("adminReply").orEmpty(),
                        createdAt = doc.getTimestamp("createdAt")?.toDate()?.time ?: 0L
                    )
                }.sortedByDescending { it.createdAt }
                trySend(items)
            }
        awaitClose { registration.remove() }
    }

}
