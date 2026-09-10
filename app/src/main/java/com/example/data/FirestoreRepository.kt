package com.example.data

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class FirestoreRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    suspend fun backupProgress(progressList: List<UserProgress>) {
        val user = auth.currentUser ?: return
        try {
            val batch = firestore.batch()
            for (progress in progressList) {
                val docRef = firestore.collection("users").document(user.uid)
                    .collection("progress").document(progress.date)
                batch.set(docRef, progress)
            }
            batch.commit().await()
        } catch (e: Exception) {
            Log.e("FirestoreRepository", "Backup failed", e)
        }
    }
}
