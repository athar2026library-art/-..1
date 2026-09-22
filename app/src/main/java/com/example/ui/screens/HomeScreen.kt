package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.AzkarData
import com.example.ui.AppViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HomeScreen(viewModel: AppViewModel, onNavigateToAzkar: (String) -> Unit) {
    val lastReadCategory by viewModel.lastReadCategory.collectAsStateWithLifecycle()
    val lastReadIndex by viewModel.lastReadIndex.collectAsStateWithLifecycle()
    val recentProgress by viewModel.recentProgress.collectAsStateWithLifecycle()
    val todayProgress by viewModel.todayProgress.collectAsStateWithLifecycle()
    val categorySize = if (lastReadCategory == "sabah") AzkarData.morningAzkar.size else AzkarData.eveningAzkar.size
    val isWirdActive = lastReadCategory.isNotEmpty() && lastReadIndex < categorySize
    val date = SimpleDateFormat("EEEE، d MMMM", Locale.forLanguageTag("ar")).format(Date())
    val completedToday = (todayProgress?.completedSabah == true) || (todayProgress?.completedMasaa == true)

    Column(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
    ) {
        Spacer(Modifier.height(28.dp))
        Text("السلام عليكم ورحمة الله", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(4.dp))
        Text(date, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(22.dp))

        Card(
            modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
        ) {
            Box(Modifier.fillMaxWidth().padding(22.dp)) {
                Column(Modifier.fillMaxWidth(0.78f)) {
                    Text("وَاذْكُرْ رَبَّكَ إِذَا نَسِيتَ", color = MaterialTheme.colorScheme.onPrimary.copy(alpha = .9f), fontSize = 14.sp)
                    Spacer(Modifier.height(10.dp))
                    Text("اجعل لليوم نصيباً من الذكر", color = MaterialTheme.colorScheme.onPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold, lineHeight = 30.sp)
                    Spacer(Modifier.height(6.dp))
                    Text("دقائق قليلة تصنع فرقاً كبيراً في قلبك.", color = MaterialTheme.colorScheme.onPrimary.copy(alpha = .8f), fontSize = 14.sp)
                }
                Icon(Icons.Default.SelfImprovement, null, Modifier.align(Alignment.TopEnd).size(58.dp), tint = MaterialTheme.colorScheme.onPrimary.copy(alpha = .25f))
            }
        }

        Spacer(Modifier.height(26.dp))
        Text("وردك اليومي", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            WirdCard(Modifier.weight(1f), "الصباح", "ابدأ يومك بنور", Icons.Default.WbSunny) { onNavigateToAzkar("sabah") }
            WirdCard(Modifier.weight(1f), "المساء", "اختم يومك بسكينة", Icons.Default.NightsStay) { onNavigateToAzkar("masaa") }
        }

        if (isWirdActive) {
            Spacer(Modifier.height(18.dp))
            Card(
                modifier = Modifier.fillMaxWidth().clickable { onNavigateToAzkar(lastReadCategory) },
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = .18f))
            ) {
                Column(Modifier.padding(18.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("متابعة من حيث توقفت", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                        Text("${lastReadIndex + 1} / $categorySize", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                    }
                    Spacer(Modifier.height(12.dp))
                    LinearProgressIndicator(
                        progress = { (lastReadIndex.toFloat() / categorySize).coerceIn(0f, 1f) },
                        Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(8.dp)),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.primary.copy(alpha = .12f)
                    )
                }
            }
        }

        Spacer(Modifier.height(26.dp))
        Text("لمحة عن إنجازك", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard(Modifier.weight(1f), "${recentProgress.sumOf { it.totalTasbeeh }}", "تسبيحة", Icons.Default.AutoAwesome)
            StatCard(Modifier.weight(1f), if (completedToday) "مكتمل" else "ابدأ الآن", "ورد اليوم", Icons.Default.SelfImprovement)
        }
        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun WirdCard(modifier: Modifier, title: String, subtitle: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Card(modifier.clickable(onClick = onClick), shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = .25f))) {
        Column(Modifier.padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, null, Modifier.size(32.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(12.dp))
            Text(title, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Spacer(Modifier.height(3.dp))
            Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun StatCard(modifier: Modifier, value: String, label: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Card(modifier, shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, Modifier.size(26.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(12.dp))
            Column {
                Text(value, fontWeight = FontWeight.Bold, fontSize = 20.sp, color = MaterialTheme.colorScheme.primary)
                Text(label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
