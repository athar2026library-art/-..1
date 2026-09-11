package com.example.ui.screens

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.AppViewModel
import com.example.data.AzkarData
import com.example.ui.AudioPlayer
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class RepeatState {
    IDLE, READING, WAITING_USER
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AzkarScreen(
    category: String,
    viewModel: AppViewModel,
    onNavigateBack: () -> Unit
) {
    val azkarList = if (category == "sabah") AzkarData.morningAzkar else AzkarData.eveningAzkar
    val title = if (category == "sabah") "🌅 الصباح" else "🌙 المساء"

    val lastReadCategory by viewModel.lastReadCategory.collectAsState()
    val lastReadIndex by viewModel.lastReadIndex.collectAsState()
    val lastReadRemaining by viewModel.lastReadRemaining.collectAsState()
    val fontSize by viewModel.fontSize.collectAsState()
    val isVibrationEnabled by viewModel.isVibrationEnabled.collectAsState()
    val hideVirtues by viewModel.hideVirtues.collectAsState()
    val hideSources by viewModel.hideSources.collectAsState()

    var currentIndex by remember { mutableStateOf(0) }
    var countRemaining by remember { mutableStateOf(1) }
    var isInitialized by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val audioPlayer = remember { AudioPlayer(context) }

    // Raddid Ma'y state
    var isAutoPlaying by remember { mutableStateOf(false) }
    var repeatState by remember { mutableStateOf(RepeatState.IDLE) }

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

    DisposableEffect(Unit) {
        onDispose {
            audioPlayer.stop()
            audioPlayer.shutdown()
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
                Text("🎉", fontSize = 64.sp)
                Spacer(modifier = Modifier.height(16.dp))
                Text("تقبل الله طاعتك", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(24.dp))
                Button(onClick = onNavigateBack) {
                    Text("العودة للرئيسية", fontSize = 18.sp)
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

    // Auto-play logic
    LaunchedEffect(isAutoPlaying, currentIndex, repeatState) {
        if (isAutoPlaying && countRemaining > 0) {
            if (repeatState == RepeatState.IDLE) {
                repeatState = RepeatState.READING
            }

            if (repeatState == RepeatState.READING) {
                audioPlayer.playAndWait(currentZekr.text)
                repeatState = RepeatState.WAITING_USER
            } else if (repeatState == RepeatState.WAITING_USER) {
                // Wait for user to tap the big button to continue
            }
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(title, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            // Big Tap Button Area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Button(
                    onClick = {
                        if (countRemaining > 0) {
                            countRemaining--
                            viewModel.addTasbeeh(1)
                            
                            if (isVibrationEnabled) {
                                vibrate(context)
                            }
                            
                            if (isAutoPlaying && repeatState == RepeatState.WAITING_USER) {
                                repeatState = RepeatState.READING
                            }

                            if (countRemaining == 0) {
                                audioPlayer.stop()
                                isAutoPlaying = false
                                repeatState = RepeatState.IDLE
                                coroutineScope.launch {
                                    delay(300)
                                    currentIndex++
                                }
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (repeatState == RepeatState.WAITING_USER) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    AnimatedContent(
                        targetState = countRemaining,
                        transitionSpec = {
                            fadeIn() togetherWith fadeOut()
                        },
                        label = "button_content"
                    ) { count ->
                        if (count == 0) {
                            Icon(Icons.Default.Check, contentDescription = "Done", modifier = Modifier.size(32.dp))
                        } else {
                            val text = if (isAutoPlaying && repeatState == RepeatState.WAITING_USER) "ردّدت (${count})" else "اضغط ($count)"
                            Text(
                                text = text,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold
                            )
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
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Zekr Progress
            Text(
                text = "الذكر ${currentIndex + 1} من ${azkarList.size}",
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                modifier = Modifier.padding(vertical = 16.dp)
            )
            
            HorizontalDivider(modifier = Modifier.padding(horizontal = 48.dp), color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f))

            Spacer(modifier = Modifier.height(32.dp))

            // Raddid Ma'y Mode Indicator
            AnimatedVisibility(visible = isAutoPlaying) {
                val isReading = repeatState == RepeatState.READING
                val stateColor = if (isReading) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary
                
                val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                val pulseAlpha by infiniteTransition.animateFloat(
                    initialValue = 0.6f,
                    targetValue = 1f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(800, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "pulseAlpha"
                )

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .padding(bottom = 24.dp)
                        .alpha(if (isReading) 1f else pulseAlpha),
                    colors = CardDefaults.cardColors(containerColor = stateColor.copy(alpha = 0.1f)),
                    border = BorderStroke(1.5.dp, stateColor)
                ) {
                    AnimatedContent(
                        targetState = repeatState,
                        transitionSpec = {
                            fadeIn(animationSpec = tween(300)) togetherWith fadeOut(animationSpec = tween(300))
                        },
                        label = "stateTransition"
                    ) { state ->
                        Text(
                            text = if (state == RepeatState.READING) "🔊 استمع..." else "🗣️ دورك الآن — ردد الذكر",
                            modifier = Modifier
                                .padding(16.dp)
                                .fillMaxWidth(),
                            textAlign = TextAlign.Center,
                            fontWeight = FontWeight.Bold,
                            color = stateColor,
                            fontSize = 18.sp
                        )
                    }
                }
            }

            // The Zekr Text
            Text(
                text = currentZekr.text,
                fontSize = fontSize.sp,
                lineHeight = (fontSize * 1.6f).sp,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(horizontal = 24.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))
            HorizontalDivider(modifier = Modifier.padding(horizontal = 48.dp), color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f))
            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "🔁 ${currentZekr.repeatCount} مرات",
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.height(24.dp))

            // Raddid Ma'y Toggle
            Card(
                modifier = Modifier
                    .wrapContentWidth()
                    .clickable {
                        if (isAutoPlaying) {
                            isAutoPlaying = false
                            repeatState = RepeatState.IDLE
                            audioPlayer.stop()
                        } else {
                            isAutoPlaying = true
                        }
                    },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isAutoPlaying) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surface
                ),
                border = BorderStroke(1.dp, if (isAutoPlaying) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        if (isAutoPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = if (isAutoPlaying) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isAutoPlaying) "إيقاف الترديد" else "🗣️ ردّد معي",
                        fontWeight = FontWeight.Bold,
                        color = if (isAutoPlaying) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Virtues and Sources
            if (currentZekr.fadl.isNotEmpty() && !hideVirtues) {
                Column(modifier = Modifier.padding(horizontal = 24.dp).fillMaxWidth()) {
                    Text(
                        text = "🌿 فضله",
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = currentZekr.fadl,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                        fontSize = 14.sp
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
            
            if (currentZekr.source.isNotEmpty() && !hideSources) {
                Column(modifier = Modifier.padding(horizontal = 24.dp).fillMaxWidth()) {
                    Text(
                        text = "📖 المصدر",
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = currentZekr.source,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                        fontSize = 14.sp
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
            }

            // extra space for the bottom tap button
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

fun vibrate(context: Context) {
    val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
        vibratorManager.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }
    
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
    } else {
        @Suppress("DEPRECATION")
        vibrator.vibrate(50)
    }
}
