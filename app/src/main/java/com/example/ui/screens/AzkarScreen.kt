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
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.AzkarData
import com.example.ui.AppViewModel
import com.example.ui.AudioPlayer
import com.example.ui.ShareHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AzkarScreen(
    category: String,
    viewModel: AppViewModel,
    onNavigateBack: () -> Unit
) {
    val fallbackAzkar = AzkarData.forCategory(category)
    val liveAzkar by remember(category) { viewModel.observePublishedAzkar(category) }
        .collectAsStateWithLifecycle(initialValue = emptyList())
    val azkarList = liveAzkar.takeIf { it.isNotEmpty() } ?: fallbackAzkar
    val title = AzkarData.titleFor(category)

    val lastReadCategory by viewModel.lastReadCategory.collectAsStateWithLifecycle()
    val lastReadIndex by viewModel.lastReadIndex.collectAsStateWithLifecycle()
    val lastReadRemaining by viewModel.lastReadRemaining.collectAsStateWithLifecycle()
    val fontSize by viewModel.fontSize.collectAsStateWithLifecycle()
    val isVibrationEnabled by viewModel.isVibrationEnabled.collectAsStateWithLifecycle()
    val keepScreenOn by viewModel.keepScreenOn.collectAsStateWithLifecycle()
    val autoDnd by viewModel.autoDnd.collectAsStateWithLifecycle()
    val hideVirtues by viewModel.hideVirtues.collectAsStateWithLifecycle()
    val hideSources by viewModel.hideSources.collectAsStateWithLifecycle()
    val autoPlayEnabled by viewModel.autoPlayEnabled.collectAsStateWithLifecycle()

    var currentIndex by remember { mutableIntStateOf(0) }
    var countRemaining by remember { mutableIntStateOf(1) }
    var isInitialized by remember { mutableStateOf(false) }
    var showBottomSheet by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val view = LocalView.current

    val audioPlayer = remember { AudioPlayer(context.applicationContext) }
    val isPlaying by audioPlayer.isPlaying.collectAsStateWithLifecycle()
    DisposableEffect(Unit) {
        onDispose {
            audioPlayer.shutdown()
            viewModel.flushPendingTasbeeh()
        }
    }

    DisposableEffect(keepScreenOn) {
        if (keepScreenOn) view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }

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

    val latestCountRemaining by rememberUpdatedState(countRemaining)
    DisposableEffect(category, currentIndex, isInitialized) {
        onDispose {
            if (isInitialized && currentIndex < azkarList.size) {
                viewModel.saveLastReadState(category, currentIndex, latestCountRemaining)
            }
        }
    }

    if (!isInitialized) return

    if (currentIndex >= azkarList.size) {
        LaunchedEffect(Unit) {
            viewModel.flushPendingTasbeeh()
            if (category == "sabah") viewModel.completeSabah()
            else if (category == "masaa") viewModel.completeMasaa()
            viewModel.clearLastReadState()
        }
        Scaffold(containerColor = MaterialTheme.colorScheme.background) { padding ->
            Column(
                modifier = Modifier.fillMaxSize().padding(padding),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("🌿", fontSize = 64.sp)
                Spacer(modifier = Modifier.height(24.dp))
                Text("تقبل الله طاعتك", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(32.dp))
                Button(onClick = onNavigateBack, shape = RoundedCornerShape(24.dp)) {
                    Text("العودة للرئيسية", modifier = Modifier.padding(8.dp))
                }
            }
        }
        return
    }

    val currentZekr = azkarList[currentIndex]
    val completedItems = currentIndex +
        ((currentZekr.repeatCount - countRemaining).toFloat() / currentZekr.repeatCount.coerceAtLeast(1))
    val readingProgress = (completedItems / azkarList.size.coerceAtLeast(1)).coerceIn(0f, 1f)

    LaunchedEffect(currentIndex) {
        if (isInitialized) {
            countRemaining = currentZekr.repeatCount
            audioPlayer.stop()
            if (autoPlayEnabled) {
                delay(350)
                audioPlayer.playAndWait(currentZekr.text)
            }
        }
    }

    val onDecrement = {
        if (countRemaining > 0) {
            countRemaining--
            viewModel.addTasbeeh(1)
            if (isVibrationEnabled) vibrateLight(context)
            if (countRemaining == 0) {
                if (isVibrationEnabled) vibrateCompletion(context)
                coroutineScope.launch {
                    delay(if (autoPlayEnabled) 1200 else 300)
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
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                        }
                    },
                    actions = {
                        FilterChip(
                            selected = autoPlayEnabled,
                            onClick = { viewModel.setAutoPlay(!autoPlayEnabled) },
                            label = { Text(if (autoPlayEnabled) "تلقائي" else "يدوي", fontSize = 12.sp) }
                        )
                        TextButton(onClick = { viewModel.setFontSize((fontSize - 2f).coerceAtLeast(16f)) }) {
                            Text("A-", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                        TextButton(onClick = { viewModel.setFontSize((fontSize + 2f).coerceAtMost(48f)) }) {
                            Text("A+", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
                )
                LinearProgressIndicator(
                    progress = { readingProgress },
                    modifier = Modifier.fillMaxWidth().height(2.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                )
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "${currentIndex + 1} / ${azkarList.size}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                    )
                    if (autoPlayEnabled) {
                        Text("قراءة مستمرة", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        },
        bottomBar = {
            Surface(color = MaterialTheme.colorScheme.background) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                        .navigationBarsPadding()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FilledTonalIconButton(onClick = {
                            if (isPlaying) audioPlayer.stop()
                            else coroutineScope.launch { audioPlayer.playAndWait(currentZekr.text) }
                        }) {
                            Icon(
                                if (isPlaying) Icons.Default.Stop else Icons.Default.VolumeUp,
                                contentDescription = if (isPlaying) "إيقاف" else "استماع"
                            )
                        }
                        FilledTonalIconButton(onClick = { audioPlayer.playLoop(currentZekr.text, 3) }) {
                            Icon(Icons.Default.Repeat, contentDescription = "تكرار 3 مرات")
                        }
                        FilledTonalIconButton(onClick = { audioPlayer.playLoop(currentZekr.text, 5) }) {
                            Icon(Icons.Default.RepeatOne, contentDescription = "تكرار 5 مرات")
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = { if (currentIndex > 0) currentIndex-- }, enabled = currentIndex > 0) {
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
                            Spacer(Modifier.width(4.dp))
                            Text("السابق")
                        }
                        TextButton(onClick = {
                            countRemaining = currentZekr.repeatCount
                            if (isVibrationEnabled) vibrateLight(context)
                        }) {
                            Icon(Icons.Default.Refresh, contentDescription = null)
                            Spacer(Modifier.width(4.dp))
                            Text("إعادة")
                        }
                        IconButton(onClick = { showBottomSheet = true }) {
                            Icon(Icons.Default.Info, contentDescription = "معلومات", tint = MaterialTheme.colorScheme.primary)
                        }
                        TextButton(onClick = { if (currentIndex < azkarList.size - 1) currentIndex++ }) {
                            Text("التالي")
                            Spacer(Modifier.width(4.dp))
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                        }
                    }
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
            Column(
                modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(32.dp))
                        .clickable(onClick = onDecrement)
                        .testTag("dhikr-card"),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(0.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                ) {
                    Column(modifier = Modifier.padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        AnimatedContent(
                            targetState = currentZekr,
                            transitionSpec = { fadeIn(tween(400)) togetherWith fadeOut(tween(400)) },
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

                Spacer(modifier = Modifier.height(36.dp))

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
                        "$countRemaining",
                        modifier = Modifier.testTag("dhikr-counter"),
                        fontSize = 40.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))
                Text(
                    "اضغط على البطاقة أو الزر للمتابعة",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)
                )
                Spacer(modifier = Modifier.height(14.dp))
                Button(
                    onClick = onDecrement,
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Text(
                        if (countRemaining > 1) "سبّح • متبقي $countRemaining" else "تمّ الذكر",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        if (showBottomSheet) {
            ModalBottomSheet(
                onDismissRequest = { showBottomSheet = false },
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(24.dp).padding(bottom = 24.dp)) {
                    Text("شرح وفضل الذكر", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(24.dp))

                    if (!hideVirtues && currentZekr.fadl.isNotEmpty()) {
                        Text("الفضل:", fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(currentZekr.fadl, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 24.sp)
                        Spacer(modifier = Modifier.height(16.dp))
                    } else if (!hideVirtues) {
                        Text("لم يرد فضل محدد نصاً لهذا الذكر.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    if (!hideSources && currentZekr.source.isNotEmpty()) {
                        Text("المصدر:", fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(currentZekr.source, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 24.sp)
                    }

                    Spacer(modifier = Modifier.height(22.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(
                            onClick = { copyText(context, currentZekr.text); showBottomSheet = false },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(Icons.Default.ContentCopy, null, Modifier.size(18.dp))
                            Spacer(Modifier.width(7.dp))
                            Text("نسخ")
                        }
                        Button(
                            onClick = {
                                try {
                                    ShareHelper.shareZekrAsImage(
                                        context,
                                        currentZekr.text,
                                        currentZekr.source
                                    )
                                } catch (_: Exception) {
                                    // ignore
                                }
                                showBottomSheet = false
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(Icons.Default.Share, null, Modifier.size(18.dp))
                            Spacer(Modifier.width(7.dp))
                            Text("مشاركة صورة")
                        }
                    }
                }
            }
        }
    }
}

private fun vibrateLight(context: Context) {
    val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        vibrator.vibrate(VibrationEffect.createOneShot(30, VibrationEffect.DEFAULT_AMPLITUDE))
    } else {
        @Suppress("DEPRECATION")
        vibrator.vibrate(30)
    }
}

private fun vibrateCompletion(context: Context) {
    val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        vibrator.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 40, 60, 40), -1))
    } else {
        @Suppress("DEPRECATION")
        vibrator.vibrate(longArrayOf(0, 40, 60, 40), -1)
    }
}

private fun copyText(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
    clipboard.setPrimaryClip(android.content.ClipData.newPlainText("zekr", text))
}
