package com.example.data

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

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
}
