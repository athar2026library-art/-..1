package com.example

import android.app.Application
import com.google.firebase.Firebase
import com.google.firebase.initialize

class AzkarApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // Initialize Firebase before installing the build-variant App Check provider.
        Firebase.initialize(this)
        AppCheckInstaller.install()
    }
}
