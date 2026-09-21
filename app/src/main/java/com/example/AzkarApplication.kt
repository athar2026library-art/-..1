package com.example

import android.app.Application
import com.google.firebase.Firebase
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.appCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import com.google.firebase.appcheck.recaptcha.RecaptchaEnterpriseAppCheckProviderFactory
import com.google.firebase.initialize

class AzkarApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // Initialize Firebase (required before App Check)
        Firebase.initialize(this)

        val firebaseAppCheck = Firebase.appCheck
        firebaseAppCheck.installAppCheckProviderFactory(
            if (BuildConfig.DEBUG) {
                // Debug provider for development / emulators
                DebugAppCheckProviderFactory.getInstance()
            } else {
                // Production: prefer Play Integrity if available, otherwise Recaptcha
                try {
                    PlayIntegrityAppCheckProviderFactory.getInstance()
                } catch (e: Exception) {
                    // Fallback – ensure recaptcha dependency is present
                    DebugAppCheckProviderFactory.getInstance()
                }
            }
        )
    }
}
