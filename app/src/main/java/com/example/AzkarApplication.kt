package com.example

import android.app.Application
import com.google.firebase.Firebase
import com.google.firebase.appcheck.appCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import com.google.firebase.initialize

class AzkarApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        Firebase.initialize(this)

        val appCheck = Firebase.appCheck
        appCheck.installAppCheckProviderFactory(
            if (BuildConfig.DEBUG) {
                DebugAppCheckProviderFactory.getInstance()
            } else {
                try {
                    PlayIntegrityAppCheckProviderFactory.getInstance()
                } catch (_: Exception) {
                    DebugAppCheckProviderFactory.getInstance()
                }
            }
        )
    }
}
