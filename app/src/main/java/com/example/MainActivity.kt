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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
import com.example.ui.SyncWorker
import java.util.concurrent.TimeUnit
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
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
        SyncWorker.enqueue(applicationContext)

        val progressRepository = ProgressRepository.getInstance(applicationContext)
        val settingsRepository = SettingsRepository(applicationContext.dataStore)
        val aiRepository = AiRepository()
        val authRepository = AuthRepository(applicationContext)
        val firestoreRepository = FirestoreRepository()

        FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
            val user = FirebaseAuth.getInstance().currentUser ?: return@addOnSuccessListener
            FirebaseFirestore.getInstance().collection("users").document(user.uid).set(
                mapOf(
                    "fcmTokens" to FieldValue.arrayUnion(token),
                    "updatedAt" to FieldValue.serverTimestamp()
                ),
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
            val onboardingComplete by settingsRepository.onboardingCompleteFlow.collectAsState(initial = false)

            val currentHour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
            val isFajrTime = currentHour in 3..6
            val finalDarkMode = isDarkMode || isFajrTime

            MyApplicationTheme(darkTheme = finalDarkMode, isFajrMode = isFajrTime) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    if (onboardingComplete) {
                        AppNavGraph(viewModel = viewModel)
                    } else {
                        BaqiyatOnboarding(onDone = { lifecycleScope.launch { settingsRepository.setOnboardingComplete() } })
                    }
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

@Composable
private fun BaqiyatOnboarding(onDone: () -> Unit) {
    var page by remember { mutableStateOf(0) }
    val pages = listOf(
        Triple("وردك اليومي بين يديك", "ابدأ بأذكار الصباح والمساء بواجهة هادئة تساعدك على الاستمرار.", com.example.R.drawable.logo_baqiyat),
        Triple("اقرأ بتركيز وراحة", "خصّص حجم الخط، فعّل الوضع الليلي، وتابع آخر موضع وصلت إليه.", com.example.R.drawable.logo_baqiyat),
        Triple("ابقَ على تواصل", "أرسل اقتراحاتك وشكاواك، وتابع ردود فريق الباقيات وإشعارات التحديثات.", com.example.R.drawable.logo_baqiyat)
    )
    val current = pages[page]
    Column(Modifier.fillMaxSize().padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.height(48.dp))
        Image(painterResource(current.third), contentDescription = "شعار الباقيات", modifier = Modifier.size(154.dp))
        Spacer(Modifier.height(30.dp))
        Text("مرحباً بك في الباقيات", color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Spacer(Modifier.height(12.dp))
        Text(current.first, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(12.dp))
        Text(current.second, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 26.sp)
        Spacer(Modifier.weight(1f))
        Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) { pages.indices.forEach { index -> Box(Modifier.size(if (index == page) 26.dp else 8.dp, 8.dp).background(if (index == page) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outline.copy(alpha = .35f), RoundedCornerShape(8.dp))) } }
        Spacer(Modifier.height(24.dp))
        Button(onClick = { if (page == pages.lastIndex) onDone() else page++ }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) { Text(if (page == pages.lastIndex) "ابدأ الآن" else "التالي") }
        if (page < pages.lastIndex) { TextButton(onClick = onDone) { Text("تخطي") } } else { Spacer(Modifier.height(48.dp)) }
    }
}
