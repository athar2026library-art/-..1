package com.example.data

import android.net.Uri
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.tasks.await

class FirestoreRepository {
    private val firestore by lazy { runCatching { FirebaseFirestore.getInstance() }.getOrNull() }
    private val auth by lazy { runCatching { FirebaseAuth.getInstance() }.getOrNull() }

    /** true عند النجاح فقط — لا رسالة نجاح كاذبة عند رفض القواعد. */
    suspend fun backupProgress(progressList: List<UserProgress>): Boolean {
        val user = auth?.currentUser ?: return false
        val db = firestore ?: return false
        if (progressList.isEmpty()) return true
        return try {
            val col = db.collection("users").document(user.uid).collection("progress")
            progressList.chunked(400).forEach { chunk ->
                val batch = db.batch()
                chunk.forEach { p ->
                    if (p.date.isBlank()) return@forEach
                    batch.set(
                        col.document(p.date),
                        mapOf(
                            "date" to p.date,
                            "completedSabah" to p.completedSabah,
                            "completedMasaa" to p.completedMasaa,
                            "totalTasbeeh" to p.totalTasbeeh
                        )
                    )
                }
                batch.commit().await()
            }
            true
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e("FirestoreRepository", "Backup failed", e)
            false
        }
    }

    /** null عند الفشل؛ قائمة (قد تكون فارغة) عند النجاح. */
    suspend fun fetchProgress(): List<UserProgress>? {
        val user = auth?.currentUser ?: return null
        val db = firestore ?: return null
        return try {
            db.collection("users").document(user.uid)
                .collection("progress").get().await()
                .toObjects(UserProgress::class.java)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e("FirestoreRepository", "Fetch failed", e)
            null
        }
    }

    /** onNewToken لا يكفي بعد تسجيل دخول لاحق — نحفظ التوكن صراحةً. */
    suspend fun saveFcmToken() {
        val user = auth?.currentUser ?: return
        val db = firestore ?: return
        try {
            val token = FirebaseMessaging.getInstance().token.await()
            db.collection("users").document(user.uid).set(
                mapOf(
                    "fcmTokens" to FieldValue.arrayUnion(token),
                    "updatedAt" to FieldValue.serverTimestamp()
                ),
                SetOptions.merge()
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e("FirestoreRepository", "Saving FCM token failed", e)
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
    }.distinctUntilChanged()

    suspend fun submitFeedback(draft: FeedbackDraft, attachmentUri: Uri? = null): Result<String> {
        val user = auth?.currentUser ?: return Result.failure(IllegalStateException("AUTH_REQUIRED"))
        val db = firestore ?: return Result.failure(IllegalStateException("FIRESTORE_UNAVAILABLE"))
        return try {
            val ref = db.collection("feedback").document()
            val attachmentUrl = if (attachmentUri != null) {
                val storageRef = FirebaseStorage.getInstance().reference
                    .child("feedback/${user.uid}/${ref.id}/attachment.jpg")
                storageRef.putFile(attachmentUri).await()
                storageRef.downloadUrl.await().toString()
            } else ""
            val data = hashMapOf<String, Any>(
                "userId" to user.uid,
                "type" to draft.type,
                "title" to draft.title,
                "message" to draft.message,
                "status" to "new",
                "adminReply" to "",
                "replyUnread" to false,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (attachmentUrl.isNotEmpty()) data["attachmentUrl"] = attachmentUrl
            db.runBatch { batch ->
                batch.set(ref, data)
                batch.set(
                    db.collection("users").document(user.uid).collection("feedback").document(ref.id),
                    data
                )
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
        if (user == null || db == null) {
            close()
            return@callbackFlow
        }
        val registration = db.collection("feedback").whereEqualTo("userId", user.uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val items = snapshot?.documents.orEmpty().map { doc ->
                    FeedbackItem(
                        id = doc.id,
                        type = doc.getString("type").orEmpty(),
                        title = doc.getString("title").orEmpty(),
                        message = doc.getString("message").orEmpty(),
                        aiSummary = doc.getString("aiSummary").orEmpty(),
                        status = doc.getString("status") ?: "new",
                        adminReply = doc.getString("adminReply").orEmpty(),
                        replyUnread = doc.getBoolean("replyUnread") ?: false,
                        createdAt = doc.getTimestamp("createdAt")?.toDate()?.time ?: 0L
                    )
                }.sortedByDescending { it.createdAt }
                trySend(items)
            }
        awaitClose { registration.remove() }
    }.distinctUntilChanged()

    suspend fun markFeedbackReplyRead(feedbackId: String) {
        val user = auth?.currentUser ?: return
        val db = firestore ?: return
        try {
            db.collection("feedback").document(feedbackId).update("replyUnread", false).await()
            db.collection("users").document(user.uid).collection("feedback")
                .document(feedbackId).update("replyUnread", false).await()
        } catch (e: Exception) {
            Log.e("FirestoreRepository", "Mark reply read failed", e)
        }
    }
}
