package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
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
import com.example.data.AzkarData
import com.example.ui.AppViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AzkarScreen(
    category: String,
    viewModel: AppViewModel,
    onNavigateBack: () -> Unit
) {
    val azkarList = if (category == "sabah") AzkarData.morningAzkar else AzkarData.eveningAzkar
    var currentIndex by remember { mutableIntStateOf(0) }
    
    if (currentIndex >= azkarList.size) {
        // Completed
        LaunchedEffect(Unit) {
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
    var countRemaining by remember(currentIndex) { mutableIntStateOf(currentZekr.repeatCount) }
    
    val fontSize by viewModel.fontSize.collectAsState()
    val isVibrationEnabled by viewModel.isVibrationEnabled.collectAsState()
    
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

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
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, "${currentZekr.text}\n\n${currentZekr.source}\n\n- تمت القراءة عبر تطبيق أذكار 🌴")
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
            // Progress
            LinearProgressIndicator(
                progress = { currentIndex.toFloat() / azkarList.size },
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            )
            
            Text("${currentIndex + 1} / ${azkarList.size}", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(16.dp))

            // Text content
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = currentZekr.text,
                        fontSize = fontSize.sp,
                        lineHeight = (fontSize * 1.5).sp,
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    if (currentZekr.fadl.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            text = "الفضل: ${currentZekr.fadl}",
                            color = MaterialTheme.colorScheme.primary,
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                    if (currentZekr.source.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = currentZekr.source,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Counter Button
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
                                if (isVibrationEnabled) {
                                    vibrate(context)
                                }
                                coroutineScope.launch {
                                    // slight delay before auto next
                                    kotlinx.coroutines.delay(300)
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
