package com.example

import android.app.Application
import com.google.firebase.Firebase
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.appCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import com.google.firebase.initialize

class AzkarApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // Initialize Firebase (required before App Check)
        Firebase.initialize(this)

        val firebaseAppCheck = Firebase.appCheck
        firebaseAppCheck.installAppCheckProviderFactory(
            if (BuildConfig.DEBUG) {
                // Use debug provider during development
                DebugAppCheckProviderFactory.getInstance()
            } else {
                // Production: Play Integrity
                PlayIntegrityAppCheckProviderFactory.getInstance()
            }
        )
    }
}
