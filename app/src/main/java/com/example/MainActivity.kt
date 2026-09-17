package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.data.AiRepository
import com.example.data.AppDatabase
import com.example.data.AuthRepository
import com.example.data.FirestoreRepository
import com.example.data.ProgressRepository
import com.example.data.SettingsRepository
import com.example.data.dataStore
import com.example.ui.AppNavGraph
import com.example.ui.AppViewModel
import com.example.ui.AppViewModelFactory
import com.example.ui.NotificationWorker
import com.example.ui.theme.MyApplicationTheme
import java.util.Calendar
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
            }
        }
        
        val workRequest = PeriodicWorkRequestBuilder<NotificationWorker>(2, TimeUnit.HOURS).build()
        WorkManager.getInstance(applicationContext).enqueueUniquePeriodicWork(
            "AzkarNotifications",
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )

        val progressDao = AppDatabase.getDatabase(this).progressDao()
        val progressRepository = ProgressRepository(progressDao)
        val settingsRepository = SettingsRepository(this.dataStore)
        val aiRepository = AiRepository()
        val authRepository = AuthRepository(this)
        val firestoreRepository = FirestoreRepository()

        setContent {
            val viewModel: AppViewModel = viewModel(
                factory = AppViewModelFactory(
                    progressRepository,
                    settingsRepository,
                    aiRepository,
                    authRepository,
                    firestoreRepository
                )
            )
            
            val isDarkMode by viewModel.isDarkMode.collectAsState()
            
            val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
            val isFajrTime = currentHour in 3..6
            val finalDarkMode = isDarkMode || isFajrTime
            
            MyApplicationTheme(darkTheme = finalDarkMode, isFajrMode = isFajrTime) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavGraph(viewModel = viewModel)
                }
            }
        }
    }
}
