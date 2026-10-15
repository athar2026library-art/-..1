package com.example

import android.app.Application
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings

class AzkarApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        try {
            FirebaseApp.initializeApp(this)
            // Offline cache so published content from the owner panel survives brief offline periods.
            runCatching {
                val db = FirebaseFirestore.getInstance()
                db.firestoreSettings = FirebaseFirestoreSettings.Builder()
                    .setPersistenceEnabled(true)
                    .setCacheSizeBytes(FirebaseFirestoreSettings.CACHE_SIZE_UNLIMITED)
                    .build()
            }.onFailure { Log.w("AzkarApplication", "Firestore persistence setup skipped", it) }
            AppCheckInstaller.install()
        } catch (e: Exception) {
            Log.e("AzkarApplication", "Firebase / App Check init failed", e)
        }
    }
}
