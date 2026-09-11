package com.example.ui.screens

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.WindowManager
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
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
import com.example.data.AzkarData
import com.example.ui.AppViewModel
import com.example.ui.AudioPlayer
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class RepeatState {
    IDLE, READING, WAITING
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AzkarScreen(
    category: String,
    viewModel: AppViewModel,
    onNavigateBack: () -> Unit
) {
    val azkarList = if (category == "sabah") AzkarData.morningAzkar else AzkarData.eveningAzkar
    
    val initialLastReadIndex = remember { if (viewModel.lastReadCategory.value == category) viewModel.lastReadIndex.value else 0 }
    val initialLastReadRemaining = remember { if (viewModel.lastReadCategory.value == category) viewModel.lastReadRemaining.value else -1 }
    
    var currentIndex by remember { mutableIntStateOf(initialLastReadIndex) }
    
    val context = LocalContext.current
    val keepScreenOn by viewModel.keepScreenOn.collectAsState()
    
    // Audio Player
    val audioPlayer = remember { AudioPlayer(context) }
    var isAutoPlaying by remember { mutableStateOf(false) }
    var repeatState by remember { mutableStateOf(RepeatState.IDLE) }
    
    DisposableEffect(keepScreenOn) {
        val activity = context as? Activity
        if (keepScreenOn) {
            activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            audioPlayer.shutdown()
        }
    }

    if (currentIndex >= azkarList.size) {
        // Completed
        LaunchedEffect(Unit) {
            viewModel.clearLastReadState()
            if (category == "sabah") {
                viewModel.completeSabah()
            } else {
                viewModel.completeMasaa()
            }
        }
        AlertDialog(
            onDismissRequest = onNavigateBack,
            title = { Text("تقبل الله") },
            text = { Text("لقد أتممت الأذكار بنجاح.") },
            confirmButton = {
                Button(onClick = onNavigateBack) {
                    Text("العودة")
                }
            }
        )
        return
    }

    val currentZekr = azkarList[currentIndex]
    var countRemaining by remember(currentIndex) { 
        mutableIntStateOf(
            if (currentIndex == initialLastReadIndex && initialLastReadRemaining > 0) {
                initialLastReadRemaining
            } else {
                currentZekr.repeatCount
            }
        )
    }
    
    LaunchedEffect(currentIndex, countRemaining) {
        if (currentIndex < azkarList.size) {
            viewModel.saveLastReadState(category, currentIndex, countRemaining)
        }
    }
    
    val fontSize by viewModel.fontSize.collectAsState()
    val isVibrationEnabled by viewModel.isVibrationEnabled.collectAsState()
    val hideVirtues by viewModel.hideVirtues.collectAsState()
    val hideSources by viewModel.hideSources.collectAsState()
    
    val coroutineScope = rememberCoroutineScope()
    
    // Auto-Play "Repeat with Me" Logic
    LaunchedEffect(isAutoPlaying, currentIndex) {
        if (isAutoPlaying && currentIndex < azkarList.size) {
            while (countRemaining > 0 && isAutoPlaying) {
                repeatState = RepeatState.READING
                val start = System.currentTimeMillis()
                val success = audioPlayer.playAndWait(azkarList[currentIndex].text)
                
                if (!success || !isAutoPlaying) {
                    isAutoPlaying = false
                    repeatState = RepeatState.IDLE
                    break
                }
                
                val duration = System.currentTimeMillis() - start
                
                countRemaining--
                viewModel.addTasbeeh(1)
                if (isVibrationEnabled) vibrate(context)
                
                if (countRemaining > 0) {
                    // Pause for user to repeat (give them 1 second more than it took to read)
                    repeatState = RepeatState.WAITING
                    delay(duration + 1000)
                } else {
                    // Finished this Zekr, wait briefly and move to next
                    repeatState = RepeatState.WAITING
                    delay(1500)
                    currentIndex++
                    repeatState = RepeatState.IDLE
                }
            }
        } else {
            repeatState = RepeatState.IDLE
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (category == "sabah") "أذكار الصباح" else "أذكار المساء") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        val shareText = "🌿\n${currentZekr.text}\n\n${currentZekr.repeatCount} مرات\n\n📖 ${currentZekr.source}\n\n- تمت القراءة عبر تطبيق أذكار 🌴"
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, shareText)
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "مشاركة الذكر"))
                    }) {
                        Icon(Icons.Default.Share, contentDescription = "مشاركة")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Progress Header
            LinearProgressIndicator(
                progress = { currentIndex.toFloat() / azkarList.size },
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            )
            
            Text("${currentIndex + 1} / ${azkarList.size}", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(16.dp))

            // Main Content Area
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(24.dp)
                            .fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = currentZekr.text,
                            fontSize = fontSize.sp,
                            lineHeight = (fontSize * 1.5).sp,
                            textAlign = TextAlign.Center,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        Text(
                            text = "× ${currentZekr.repeatCount}",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        
                        if ((currentZekr.fadl.isNotEmpty() && !hideVirtues) || (currentZekr.source.isNotEmpty() && !hideSources)) {
                            Divider(modifier = Modifier.padding(vertical = 16.dp))
                            
                            var expanded by remember(currentIndex) { mutableStateOf(false) }
                            val toggleText = if (currentZekr.fadl.isNotEmpty() && !hideVirtues) "🌿 فضله" else "📖 المصدر"
                            
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { expanded = !expanded }
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(toggleText, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                Icon(if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            }
                            
                            AnimatedVisibility(visible = expanded) {
                                Column(
                                    horizontalAlignment = Alignment.Start,
                                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
                                ) {
                                    if (currentZekr.fadl.isNotEmpty() && !hideVirtues) {
                                        Text(
                                            text = "🌿 فضله",
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.titleMedium
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = currentZekr.fadl,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            textAlign = TextAlign.Start,
                                            style = MaterialTheme.typography.bodyLarge
                                        )
                                        Spacer(modifier = Modifier.height(16.dp))
                                    }
                                    if (currentZekr.source.isNotEmpty() && !hideSources) {
                                        Text(
                                            text = "📖 المصدر",
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.titleSmall
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = currentZekr.source,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                            textAlign = TextAlign.Start,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    }
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        AnimatedVisibility(visible = isAutoPlaying) {
                            val isReading = repeatState == RepeatState.READING
                            val stateColor = if (isReading) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary
                            
                            val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                            val pulseAlpha by infiniteTransition.animateFloat(
                                initialValue = 0.5f,
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
                                    .padding(bottom = 16.dp)
                                    .alpha(pulseAlpha),
                                colors = CardDefaults.cardColors(containerColor = stateColor.copy(alpha = 0.15f)),
                                border = androidx.compose.foundation.BorderStroke(1.5.dp, stateColor)
                            ) {
                                AnimatedContent(
                                    targetState = repeatState,
                                    transitionSpec = {
                                        fadeIn(animationSpec = tween(300)) togetherWith fadeOut(animationSpec = tween(300))
                                    },
                                    label = "stateTransition"
                                ) { state ->
                                    Text(
                                        text = if (state == RepeatState.READING) "🔊 استمع..." else "🗣️ الآن دورك — ردد",
                                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                                        textAlign = TextAlign.Center,
                                        fontWeight = FontWeight.Bold,
                                        color = stateColor,
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                }
                            }
                        }
                        
                        Button(
                            onClick = { 
                                if (isAutoPlaying) {
                                    isAutoPlaying = false
                                    repeatState = RepeatState.IDLE
                                    audioPlayer.stop()
                                } else {
                                    isAutoPlaying = true
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isAutoPlaying) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Icon(if (isAutoPlaying) Icons.Default.Stop else Icons.Default.PlayArrow, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (isAutoPlaying) "إيقاف" else "🗣️ ردّد معي", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Manual Counter Button
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .clickable {
                        if (countRemaining > 0) {
                            countRemaining--
                            viewModel.addTasbeeh(1)
                            if (countRemaining == 0) {
                                audioPlayer.stop()
                                isAutoPlaying = false
                                repeatState = RepeatState.IDLE
                                if (isVibrationEnabled) {
                                    vibrate(context)
                                }
                                coroutineScope.launch {
                                    delay(300)
                                    currentIndex++
                                }
                            }
                        }
                    }
            ) {
                CircularProgressIndicator(
                    progress = { (currentZekr.repeatCount - countRemaining).toFloat() / currentZekr.repeatCount },
                    modifier = Modifier.fillMaxSize(),
                    strokeWidth = 8.dp
                )
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(100.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = countRemaining.toString(),
                            fontSize = 36.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
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
        vibrator.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE))
    } else {
        @Suppress("DEPRECATION")
        vibrator.vibrate(100)
    }
}
