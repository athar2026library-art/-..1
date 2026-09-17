package com.example.ui.screens

import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.AppViewModel
import com.example.data.AzkarData
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AzkarScreen(
    category: String,
    viewModel: AppViewModel,
    onNavigateBack: () -> Unit
) {
    val azkarList = if (category == "sabah") AzkarData.morningAzkar else AzkarData.eveningAzkar
    val title = if (category == "sabah") "أذكار الصباح" else "أذكار المساء"
    val lastReadCategory by viewModel.lastReadCategory.collectAsState()
    val lastReadIndex by viewModel.lastReadIndex.collectAsState()
    val lastReadRemaining by viewModel.lastReadRemaining.collectAsState()
    val fontSize by viewModel.fontSize.collectAsState()
    val isVibrationEnabled by viewModel.isVibrationEnabled.collectAsState()
    val keepScreenOn by viewModel.keepScreenOn.collectAsState()
    val autoDnd by viewModel.autoDnd.collectAsState()

    var currentIndex by remember { mutableStateOf(0) }
    var countRemaining by remember { mutableStateOf(1) }
    var isInitialized by remember { mutableStateOf(false) }
    var showBottomSheet by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val view = LocalView.current

    // Screen On Logic
    DisposableEffect(keepScreenOn) {
        if (keepScreenOn) {
            view.keepScreenOn = true
        }
        onDispose {
            view.keepScreenOn = false
        }
    }

    // Auto DND Logic
    DisposableEffect(autoDnd) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        var originalFilter = -1
        
        if (autoDnd && notificationManager.isNotificationPolicyAccessGranted) {
            originalFilter = notificationManager.currentInterruptionFilter
            notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALARMS)
        }
        
        onDispose {
            if (autoDnd && notificationManager.isNotificationPolicyAccessGranted && originalFilter != -1) {
                notificationManager.setInterruptionFilter(originalFilter)
            }
        }
    }

    LaunchedEffect(Unit) {
        if (lastReadCategory == category && lastReadIndex < azkarList.size) {
            currentIndex = lastReadIndex
            countRemaining = lastReadRemaining
        } else {
            currentIndex = 0
            countRemaining = azkarList.firstOrNull()?.repeatCount ?: 1
        }
        isInitialized = true
    }

    LaunchedEffect(currentIndex, countRemaining) {
        if (isInitialized) {
            viewModel.saveLastReadState(category, currentIndex, countRemaining)
        }
    }

    if (!isInitialized) return

    if (currentIndex >= azkarList.size) {
        LaunchedEffect(Unit) {
            if (category == "sabah") viewModel.completeSabah() else viewModel.completeMasaa()
            viewModel.clearLastReadState()
        }
        Scaffold(containerColor = MaterialTheme.colorScheme.background) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("🌿", fontSize = 64.sp)
                Spacer(modifier = Modifier.height(24.dp))
                Text("تقبل الله طاعتك", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                Spacer(modifier = Modifier.height(32.dp))
                Button(
                    onClick = onNavigateBack,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Text("العودة للرئيسية", fontSize = 16.sp, modifier = Modifier.padding(8.dp))
                }
            }
        }
        return
    }

    val currentZekr = azkarList[currentIndex]

    LaunchedEffect(currentIndex) {
        if (isInitialized) {
            countRemaining = currentZekr.repeatCount
        }
    }

    val onDecrement = {
        if (countRemaining > 0) {
            countRemaining--
            viewModel.addTasbeeh(1)
            
            if (isVibrationEnabled) {
                vibrateLight(context)
            }
            
            if (countRemaining == 0) {
                coroutineScope.launch {
                    delay(300)
                    currentIndex++
                }
            }
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Column {
                TopAppBar(
                    title = { Text(title, fontWeight = FontWeight.Bold, fontSize = 20.sp) },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        TextButton(onClick = { viewModel.setFontSize((fontSize - 2f).coerceAtLeast(16f)) }) {
                            Text("A-", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                        TextButton(onClick = { viewModel.setFontSize((fontSize + 2f).coerceAtMost(48f)) }) {
                            Text("A+", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                )
                // Thin Progress Bar
                LinearProgressIndicator(
                    progress = { (currentIndex.toFloat() / azkarList.size).coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth().height(2.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                )
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    Text(
                        text = "${currentIndex + 1} / ${azkarList.size}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                    )
                }
            }
        },
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.background,
                tonalElevation = 0.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 24.dp)
                        .navigationBarsPadding(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Previous
                    IconButton(
                        onClick = { if (currentIndex > 0) currentIndex-- },
                        enabled = currentIndex > 0
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Previous") // ArrowForward is RTL "Previous"
                    }
                    
                    // Reset
                    IconButton(
                        onClick = { countRemaining = currentZekr.repeatCount }
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Reset")
                    }
                    
                    // Info / Virtues
                    IconButton(
                        onClick = { showBottomSheet = true }
                    ) {
                        Icon(Icons.Default.Info, contentDescription = "Info", tint = MaterialTheme.colorScheme.primary)
                    }

                    // Next
                    IconButton(
                        onClick = { if (currentIndex < azkarList.size - 1) currentIndex++ }
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Next") // ArrowBack is RTL "Next"
                    }
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = { onDecrement() }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Main Zekr Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(32.dp)),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                ) {
                    Column(
                        modifier = Modifier.padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        AnimatedContent(
                            targetState = currentZekr,
                            transitionSpec = {
                                fadeIn(animationSpec = tween(400)) togetherWith fadeOut(animationSpec = tween(400))
                            },
                            label = "zekr_text"
                        ) { zekr ->
                            Text(
                                text = zekr.text,
                                fontSize = fontSize.sp,
                                lineHeight = (fontSize * 1.8f).sp,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(48.dp))
                
                // Big Counter Circle
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        progress = { 
                            if (currentZekr.repeatCount == 0) 1f 
                            else (currentZekr.repeatCount - countRemaining).toFloat() / currentZekr.repeatCount 
                        },
                        modifier = Modifier.size(120.dp),
                        color = MaterialTheme.colorScheme.primary,
                        strokeWidth = 4.dp
                    )
                    Text(
                        text = "$countRemaining",
                        fontSize = 40.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "اضغط في أي مكان",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)
                )
            }
        }

        if (showBottomSheet) {
            ModalBottomSheet(
                onDismissRequest = { showBottomSheet = false },
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                        .padding(bottom = 24.dp)
                ) {
                    Text("شرح وفضل الذكر", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    if (currentZekr.fadl.isNotEmpty()) {
                        Text("الفضل:", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(currentZekr.fadl, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 24.sp)
                        Spacer(modifier = Modifier.height(16.dp))
                    } else {
                        Text("لم يرد فضل محدد نصاً لهذا الذكر، وهو من مجمل ذكر الله تعالى.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                    
                    if (currentZekr.source.isNotEmpty()) {
                        Text("المصدر:", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(currentZekr.source, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 24.sp)
                    }
                }
            }
        }
    }
}

fun vibrateLight(context: Context) {
    val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
        vibratorManager.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }
    
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        vibrator.vibrate(VibrationEffect.createOneShot(20, VibrationEffect.DEFAULT_AMPLITUDE))
    } else {
        @Suppress("DEPRECATION")
        vibrator.vibrate(20)
    }
}
