package com.example.ui.screens

import android.icu.util.IslamicCalendar
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AirlineSeatFlat
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
fun HomeScreen(
    viewModel: AppViewModel,
    onNavigateToAzkar: (String) -> Unit,
    onNavigateToSearch: () -> Unit = {}
) {
    val lastReadCategory by viewModel.lastReadCategory.collectAsStateWithLifecycle()
    val lastReadIndex by viewModel.lastReadIndex.collectAsStateWithLifecycle()
    val recentProgress by viewModel.recentProgress.collectAsStateWithLifecycle()
    val todayProgress by viewModel.todayProgress.collectAsStateWithLifecycle()
    val categorySize = AzkarData.forCategory(lastReadCategory).size.coerceAtLeast(1)
    val isWirdActive = lastReadCategory.isNotEmpty() && lastReadIndex < categorySize
    val gregorian = remember {
        SimpleDateFormat("EEEE، d MMMM", Locale.forLanguageTag("ar")).format(Date())
    }
    val hijri = remember { hijriDateLabel() }
    val completedToday = (todayProgress?.completedSabah == true) || (todayProgress?.completedMasaa == true)
    val sabahDone = todayProgress?.completedSabah == true
    val masaaDone = todayProgress?.completedMasaa == true
    val tasbeehToday = todayProgress?.totalTasbeeh ?: 0
    val totalTasbeeh = remember(recentProgress) { recentProgress.sumOf { it.totalTasbeeh } }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onNavigateToSearch,
                icon = { Icon(Icons.Default.Search, contentDescription = null) },
                text = { Text("بحث") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
        ) {
            Spacer(Modifier.height(20.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text("السلام عليكم ورحمة الله", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    Text(gregorian, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(hijri, color = MaterialTheme.colorScheme.primary, fontSize = 13.sp)
                }
                IconButton(onClick = onNavigateToSearch) {
                    Icon(Icons.Default.Search, contentDescription = "بحث في الأذكار", tint = MaterialTheme.colorScheme.primary)
                }
            }
            Spacer(Modifier.height(18.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
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

            Spacer(Modifier.height(18.dp))
            DailyChallengeCard(
                sabahDone = sabahDone,
                masaaDone = masaaDone,
                tasbeehToday = tasbeehToday,
                onOpenSabah = { onNavigateToAzkar("sabah") },
                onOpenMasaa = { onNavigateToAzkar("masaa") }
            )

            if (isWirdActive) {
                Spacer(Modifier.height(16.dp))
                Card(
                    modifier = Modifier.fillMaxWidth().clickable { onNavigateToAzkar(lastReadCategory) },
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = .28f))
                ) {
                    Column(Modifier.padding(20.dp)) {
                        Text("متابعة من حيث توقفت", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "${AzkarData.titleFor(lastReadCategory)} · الذكر ${lastReadIndex + 1} من $categorySize",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 13.sp
                        )
                        Spacer(Modifier.height(12.dp))
                        LinearProgressIndicator(
                            progress = { (lastReadIndex.toFloat() / categorySize).coerceIn(0f, 1f) },
                            Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(8.dp)),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.primary.copy(alpha = .12f)
                        )
                        Spacer(Modifier.height(12.dp))
                        Button(
                            onClick = { onNavigateToAzkar(lastReadCategory) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp)
                        ) { Text("أكمل الورد الآن") }
                    }
                }
            }

            Spacer(Modifier.height(26.dp))
            Text("وردك اليومي", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                WirdCard(Modifier.weight(1f), "الصباح", "ابدأ يومك بنور", Icons.Default.WbSunny) { onNavigateToAzkar("sabah") }
                WirdCard(Modifier.weight(1f), "المساء", "اختم يومك بسكينة", Icons.Default.NightsStay) { onNavigateToAzkar("masaa") }
            }
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                WirdCard(Modifier.weight(1f), "النوم", "طمأنينة قبل النوم", Icons.Default.AirlineSeatFlat) { onNavigateToAzkar("sleep") }
                WirdCard(Modifier.weight(1f), "السفر", "حفظ وأمان", Icons.Default.DirectionsCar) { onNavigateToAzkar("travel") }
            }

            Spacer(Modifier.height(26.dp))
            Text("لمحة عن إنجازك", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard(Modifier.weight(1f), "$totalTasbeeh", "تسبيحة", Icons.Default.AutoAwesome)
                StatCard(Modifier.weight(1f), if (completedToday) "مكتمل" else "ابدأ الآن", "ورد اليوم", Icons.Default.SelfImprovement)
            }
            Spacer(Modifier.height(88.dp))
        }
    }
}

@Composable
private fun DailyChallengeCard(
    sabahDone: Boolean,
    masaaDone: Boolean,
    tasbeehToday: Int,
    onOpenSabah: () -> Unit,
    onOpenMasaa: () -> Unit
) {
    val both = sabahDone && masaaDone
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = .25f))
    ) {
        Column(Modifier.padding(18.dp)) {
            Text("تحدي اليوم", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(4.dp))
            Text(
                if (both) "أحسنت — أكملت ورد الصباح والمساء." else "أكمل أذكار الصباح والمساء لتحصل على نجمة اليوم.",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AssistChip(
                    onClick = onOpenSabah,
                    label = { Text(if (sabahDone) "الصباح ✓" else "الصباح") }
                )
                AssistChip(
                    onClick = onOpenMasaa,
                    label = { Text(if (masaaDone) "المساء ✓" else "المساء") }
                )
                AssistChip(
                    onClick = {},
                    enabled = false,
                    label = { Text("$tasbeehToday تسبيحة") }
                )
            }
        }
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

private fun hijriDateLabel(): String {
    val cal = IslamicCalendar()
    val day = cal.get(IslamicCalendar.DAY_OF_MONTH)
    val month = cal.get(IslamicCalendar.MONTH)
    val year = cal.get(IslamicCalendar.YEAR)
    val months = listOf(
        "محرم", "صفر", "ربيع الأول", "ربيع الآخر",
        "جمادى الأولى", "جمادى الآخرة", "رجب", "شعبان",
        "رمضان", "شوال", "ذو القعدة", "ذو الحجة"
    )
    val monthName = months.getOrElse(month) { "" }
    return "$day $monthName $year هـ"
}
