package com.example.ui.screens

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.AppViewModel

private val presetDhikr = listOf(
    "سُبْحَانَ اللَّهِ",
    "الْحَمْدُ لِلَّهِ",
    "اللَّهُ أَكْبَرُ",
    "لَا إِلَهَ إِلَّا اللَّهُ",
    "سُبْحَانَ اللَّهِ وَبِحَمْدِهِ",
    "أَسْتَغْفِرُ اللَّهَ"
)

private val targets = listOf(33, 100, 0) // 0 = unlimited

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FreeSebhaScreen(
    viewModel: AppViewModel,
    onNavigateBack: () -> Unit
) {
    val isVibrationEnabled by viewModel.isVibrationEnabled.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var selectedDhikr by remember { mutableIntStateOf(0) }
    var target by remember { mutableIntStateOf(33) }
    var count by remember { mutableIntStateOf(0) }

    // Flush pending tasbeeh when leaving
    DisposableEffect(Unit) {
        onDispose { viewModel.flushPendingTasbeeh() }
    }

    fun onTap() {
        count++
        viewModel.addTasbeeh(1)
        if (isVibrationEnabled) {
            vibrateLight(context)
            if (count % 100 == 0) vibrateCompletion(context)
        }
        if (target > 0 && count >= target) {
            if (isVibrationEnabled) vibrateCompletion(context)
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("مسبحة", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                    }
                },
                actions = {
                    IconButton(onClick = { count = 0 }) {
                        Icon(Icons.Default.Refresh, contentDescription = "تصفير")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Dhikr selector chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                presetDhikr.take(3).forEachIndexed { index, text ->
                    FilterChip(
                        selected = selectedDhikr == index,
                        onClick = { selectedDhikr = index },
                        label = { Text(text, fontSize = 12.sp, maxLines = 1) }
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                presetDhikr.drop(3).forEachIndexed { i, text ->
                    val index = i + 3
                    FilterChip(
                        selected = selectedDhikr == index,
                        onClick = { selectedDhikr = index },
                        label = { Text(text, fontSize = 12.sp, maxLines = 1) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Target selector
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                targets.forEach { t ->
                    FilterChip(
                        selected = target == t,
                        onClick = { target = t; count = 0 },
                        label = {
                            Text(if (t == 0) "حر" else "$t")
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Current dhikr text
            Text(
                text = presetDhikr[selectedDhikr],
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onBackground,
                lineHeight = 40.sp
            )

            Spacer(modifier = Modifier.height(36.dp))

            // Big tappable counter
            Box(
                modifier = Modifier
                    .size(200.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                    .clickable { onTap() },
                contentAlignment = Alignment.Center
            ) {
                if (target > 0) {
                    CircularProgressIndicator(
                        progress = { (count.toFloat() / target).coerceIn(0f, 1f) },
                        modifier = Modifier.size(200.dp),
                        color = MaterialTheme.colorScheme.primary,
                        strokeWidth = 6.dp,
                        trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "$count",
                        fontSize = 56.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (target > 0) {
                        Text(
                            text = "من $target",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "اضغط على الدائرة للتسبيح",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.45f)
            )

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = { onTap() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(18.dp)
            ) {
                Text("سبّح", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(28.dp))
        }
    }
}

private fun vibrateLight(context: Context) {
    val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
        manager.defaultVibrator
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
        val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
        manager.defaultVibrator
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
