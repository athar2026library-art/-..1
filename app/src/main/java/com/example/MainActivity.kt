package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.ProgressRepository
import com.example.data.SettingsRepository
import com.example.data.dataStore
import com.example.data.AiRepository
import com.example.data.AuthRepository
import com.example.data.FirestoreRepository
import com.example.ui.AppNavGraph
import com.example.ui.AppViewModel
import com.example.ui.AppViewModelFactory
import com.example.ui.theme.MyApplicationTheme
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.ui.NotificationWorker
import java.util.concurrent.TimeUnit
import android.Manifest
import android.os.Build
import androidx.core.app.ActivityCompat
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat

class MainActivity : ComponentActivity() {
    @androidx.compose.material3.ExperimentalMaterial3Api
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
            androidx.work.ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
        
        val progressRepository = ProgressRepository.getInstance(applicationContext)
        val settingsRepository = SettingsRepository(applicationContext.dataStore)
        val aiRepository = AiRepository()
        val authRepository = AuthRepository(applicationContext)
        val firestoreRepository = FirestoreRepository()

        FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
            val user = FirebaseAuth.getInstance().currentUser ?: return@addOnSuccessListener
            FirebaseFirestore.getInstance().collection("users").document(user.uid).set(
                mapOf("fcmTokens" to FieldValue.arrayUnion(token), "notificationsEnabled" to true, "updatedAt" to FieldValue.serverTimestamp()),
                com.google.firebase.firestore.SetOptions.merge()
            )
        }
        
        setContent {
            var showSplash by remember { mutableStateOf(true) }
            LaunchedEffect(Unit) { kotlinx.coroutines.delay(1400); showSplash = false }
            if (showSplash) { BaqiyatSplash() } else {
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
            
            val currentHour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
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
}

@Composable
private fun BaqiyatSplash() {
    var revealed by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { revealed = true }
    val scale = animateFloatAsState(if (revealed) 1f else .82f, animationSpec = tween(800), label = "splashScale")
    Box(Modifier.fillMaxSize().background(Color(0xFFFBFAF3)), contentAlignment = Alignment.Center) {
        Image(painterResource(com.example.R.drawable.logo_baqiyat), contentDescription = "شعار الباقيات", modifier = Modifier.size(148.dp).scale(scale.value))
    }
}
