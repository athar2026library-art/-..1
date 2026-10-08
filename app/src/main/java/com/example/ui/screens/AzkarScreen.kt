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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.example.ui.components.BaqiyatButton
import com.example.ui.components.MihrabCard
import com.example.ui.components.ProgressRing
import com.example.ui.theme.ZekrTextStyle
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
import com.example.data.CategoryDefaults
import com.example.data.CustomWirds
import com.example.data.Zekr
import com.example.ui.AppViewModel
import com.example.ui.AudioPlayer
import com.example.ui.ShareHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AzkarScreen(
    category: String,
    viewModel: AppViewModel,
    onNavigateBack: () -> Unit
) {
    val customWirds by viewModel.customWirds.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val isCustomWird = category.startsWith("wird_")
    val isSnapshotMode = category == "favorites" || isCustomWird
    val customWird = if (isCustomWird) customWirds.firstOrNull { "wird_${it.id}" == category } else null
    val fallbackAzkar = remember(category, customWirds) {
        when {
            category == "favorites" -> AzkarData.byIds(viewModel.favorites.value)
            isCustomWird -> customWirds.firstOrNull { "wird_${it.id}" == category }
                ?.let { CustomWirds.resolve(it, AzkarData.all()) }.orEmpty()
            else -> AzkarData.builtInOrEmpty(category)
        }
    }
    val liveAzkar by remember(category) {
        if (isSnapshotMode) flowOf<List<Zekr>?>(null)
        else viewModel.observePublishedAzkar(category).map<List<Zekr>, List<Zekr>?> { it }
    }.collectAsStateWithLifecycle(initialValue = null)
    val azkarList = liveAzkar?.takeIf { it.isNotEmpty() } ?: fallbackAzkar
    val isLoadingContent = liveAzkar == null && !isSnapshotMode && fallbackAzkar.isEmpty()
    val title = when {
        category == "favorites" -> "المفضلة"
        isCustomWird -> customWird?.name ?: "وردي"
        CategoryDefaults.isBuiltIn(category) -> AzkarData.titleFor(category)
        else -> categories.firstOrNull { it.id == category }?.title ?: "الأذكار"
    }

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

    LaunchedEffect(azkarList, category) {
        if (!isSnapshotMode && lastReadCategory == category && lastReadIndex < azkarList.size) {
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
            if (isInitialized && !isSnapshotMode && currentIndex < azkarList.size) {
                viewModel.saveLastReadState(category, currentIndex, latestCountRemaining)
            }
        }
    }

    if (!isInitialized) return

    if (isLoadingContent) {
        Scaffold(containerColor = androidx.compose.ui.graphics.Color.Transparent) { padding ->
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        return
    }

    if (azkarList.isEmpty()) {
        Scaffold(
            containerColor = androidx.compose.ui.graphics.Color.Transparent,
            topBar = {
                TopAppBar(
                    title = { Text(title) },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = androidx.compose.ui.graphics.Color.Transparent)
                )
            }
        ) { padding ->
            Column(
                Modifier.fillMaxSize().padding(padding).padding(32.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("لا أذكار بعد", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                Text(
                    if (isCustomWird) "هذا الورد فارغ أو حُذفت أذكاره."
                    else "لم تُنشر أذكار لهذا التصنيف بعد.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(20.dp))
                BaqiyatButton(text = "العودة", onClick = onNavigateBack)
            }
        }
        return
    }

    if (currentIndex >= azkarList.size) {
        LaunchedEffect(Unit) {
            viewModel.flushPendingTasbeeh()
            if (!isSnapshotMode) {
                if (category == "sabah") viewModel.completeSabah()
                else if (category == "masaa") viewModel.completeMasaa()
                else if (category == "sleep") viewModel.completeSleep()
                viewModel.clearLastReadState()
            }
        }
        Scaffold(containerColor = androidx.compose.ui.graphics.Color.Transparent) { padding ->
            Column(
                modifier = Modifier.fillMaxSize().padding(padding),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("🌿", fontSize = 64.sp)
                Spacer(modifier = Modifier.height(24.dp))
                Text("تقبل الله طاعتك", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(32.dp))
                BaqiyatButton(
                    text = "العودة للرئيسية",
                    onClick = onNavigateBack,
                    modifier = Modifier.padding(horizontal = 40.dp)
                )
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
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
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
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = androidx.compose.ui.graphics.Color.Transparent)
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
            Surface(color = androidx.compose.ui.graphics.Color.Transparent) {
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            MihrabCard(
                modifier = Modifier.fillMaxWidth().testTag("dhikr-card"),
                onClick = onDecrement,
                clickLabel = "إنقاص العدّاد"
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 26.dp, end = 26.dp, top = 92.dp, bottom = 30.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    AnimatedContent(
                        targetState = currentZekr,
                        transitionSpec = { fadeIn(tween(400)) togetherWith fadeOut(tween(400)) },
                        label = "zekr_text"
                    ) { zekr ->
                        Text(
                            text = zekr.text,
                            style = ZekrTextStyle.copy(fontSize = fontSize.sp, lineHeight = (fontSize * 1.95f).sp),
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Spacer(Modifier.height(28.dp))
                    ProgressRing(
                        progress = if (currentZekr.repeatCount == 0) 1f
                        else (currentZekr.repeatCount - countRemaining).toFloat() / currentZekr.repeatCount,
                        size = 96.dp
                    ) {
                        Text(
                            "$countRemaining",
                            modifier = Modifier.testTag("dhikr-counter"),
                            style = MaterialTheme.typography.headlineSmall.copy(fontSize = 34.sp),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Spacer(Modifier.height(18.dp))
            Text(
                "اضغط على القوس أو الزر للمتابعة",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(14.dp))
            BaqiyatButton(
                text = if (countRemaining > 1) "سبّح • متبقي $countRemaining" else "تمّ الذكر",
                onClick = onDecrement,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))
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
                                } catch (_: Exception) { }
                                showBottomSheet = false
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(Icons.Default.Share, null, Modifier.size(18.dp))
                            Spacer(Modifier.width(7.dp))
                            Text("مشاركة")
                        }
                    }
                }
            }
        }
    }
}

private fun vibrateLight(context: Context) {
    try {
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
    } catch (_: Exception) { }
}

private fun vibrateCompletion(context: Context) {
    try {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 40, 40, 40), -1))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(longArrayOf(0, 40, 40, 40), -1)
        }
    } catch (_: Exception) { }
}

private fun copyText(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
    clipboard.setPrimaryClip(android.content.ClipData.newPlainText("zekr", text))
}
