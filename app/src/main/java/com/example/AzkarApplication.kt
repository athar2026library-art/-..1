package com.example

import android.app.Application
import android.util.Log
import com.google.firebase.FirebaseApp

class AzkarApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        try {
            FirebaseApp.initializeApp(this)
            AppCheckInstaller.install()
        } catch (e: Exception) {
            Log.e("AzkarApplication", "Firebase / App Check init failed", e)
        }
    }
}
