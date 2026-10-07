package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.AppViewModel
import com.example.ui.components.BaqiyatButton
import com.example.ui.components.BaqiyatButtonStyle
import com.example.ui.components.BaqiyatChip
import com.example.ui.components.ProgressRing
import com.example.ui.components.pressScale
import com.example.ui.theme.Baqiyat

/**
 * مسبحة رقمية بسيطة: عدّاد كبير وهدف (33 / 99 / 100 / حر) وجولات.
 * (الأهداف المحفوظة والسجل والاهتزاز المميز تأتي في المرحلة 3)
 */
@Composable
fun TasbihScreen(viewModel: AppViewModel) {
    val vibration by viewModel.isVibrationEnabled.collectAsStateWithLifecycle()
    val today by viewModel.todayProgress.collectAsStateWithLifecycle()
    var count by rememberSaveable { mutableStateOf(0) }
    var rounds by rememberSaveable { mutableStateOf(0) }
    var target by rememberSaveable { mutableStateOf(33) } // 0 = حر
    val haptic = LocalHapticFeedback.current
    val p = Baqiyat.colors
    val source = remember { MutableInteractionSource() }

    DisposableEffect(Unit) { onDispose { viewModel.flushPendingTasbeeh() } }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text("المسبحة", style = MaterialTheme.typography.titleLarge) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(33, 99, 100, 0).forEach { t ->
                    BaqiyatChip(
                        text = if (t == 0) "حرّ" else "$t",
                        selected = target == t,
                        onClick = { target = t; count = 0; rounds = 0 }
                    )
                }
            }
            Spacer(Modifier.height(32.dp))

            Box(
                modifier = Modifier
                    .size(280.dp)
                    .pressScale(source, 0.96f)
                    .clip(CircleShape)
                    .background(p.glass)
                    .clickable(
                        interactionSource = source,
                        indication = null,
                        role = Role.Button,
                        onClickLabel = "تسبيحة"
                    ) {
                        val roundDone = target > 0 && count >= target
                        count = if (roundDone) 1 else count + 1
                        viewModel.addTasbeeh(1)
                        val hit = if (target > 0) count == target else count % 100 == 0
                        if (hit && target > 0) rounds++
                        if (vibration) {
                            haptic.performHapticFeedback(
                                if (hit) HapticFeedbackType.LongPress else HapticFeedbackType.TextHandleMove
                            )
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                ProgressRing(
                    progress = if (target > 0) count / target.toFloat() else (count % 100) / 100f,
                    size = 280.dp,
                    stroke = 10.dp
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("$count", fontSize = 72.sp, color = p.primary, style = MaterialTheme.typography.displaySmall.copy(fontSize = 72.sp, lineHeight = 84.sp))
                        Text(
                            if (target > 0) "من $target" else "تسبيحة",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(Modifier.height(28.dp))
            Text(
                if (target > 0) "الجولات المكتملة: $rounds" else "اضغط على الدائرة للتسبيح",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "تسبيحات اليوم: ${today?.totalTasbeeh ?: 0}",
                style = MaterialTheme.typography.titleMedium,
                color = p.accent
            )
            Spacer(Modifier.height(24.dp))
            BaqiyatButton(
                text = "إعادة العدّ",
                icon = Icons.Default.Refresh,
                style = BaqiyatButtonStyle.Glass,
                onClick = { count = 0; rounds = 0 },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(96.dp))
        }
    }
}
