package com.example.ui.screens

import android.icu.util.IslamicCalendar
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.AzkarData
import com.example.ui.AppViewModel
import com.example.ui.components.BaqiyatButton
import com.example.ui.components.BaqiyatChip
import com.example.ui.components.GlassCard
import com.example.ui.components.ProgressRing
import com.example.ui.theme.Baqiyat
import com.example.ui.theme.ZekrTextStyle
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HomeScreen(
    viewModel: AppViewModel,
    onNavigateToAzkar: (String) -> Unit,
    onNavigateToSearch: () -> Unit = {},
    onNavigateToTasbih: () -> Unit = {},
    onNavigateToAssistant: () -> Unit = {},
    onNavigateToJourney: () -> Unit = {},
    onNavigateToFavorites: () -> Unit = {}
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
    val p = Baqiyat.colors

    Scaffold(
        containerColor = Color.Transparent,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onNavigateToSearch,
                icon = { Icon(Icons.Default.Search, contentDescription = null) },
                text = { Text("بحث") },
                shape = CircleShape,
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
            Spacer(Modifier = Modifier.height(20.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text("السلام عليكم ورحمة الله", style = MaterialTheme.typography.headlineSmall)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(gregorian, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(hijri, style = MaterialTheme.typography.bodySmall, color = p.accent)
                }
                IconButton(onClick = onNavigateToSearch) {
                    Icon(Icons.Default.Search, contentDescription = "بحث في الأذكار", tint = p.primary)
                }
            }
            Spacer(modifier = Modifier.height(20.dp))

            HeroCard()

            Spacer(modifier = Modifier.height(18.dp))
            DailyChallengeCard(
                sabahDone = sabahDone,
                masaaDone = masaaDone,
                tasbeehToday = tasbeehToday,
                onOpenSabah = { onNavigateToAzkar("sabah") },
                onOpenMasaa = { onNavigateToAzkar("masaa") }
            )

            if (isWirdActive) {
                Spacer(modifier = Modifier.height(16.dp))
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { onNavigateToAzkar(lastReadCategory) },
                    clickLabel = "متابعة الورد"
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        ProgressRing(
                            progress = lastReadIndex.toFloat() / categorySize,
                            size = 64.dp,
                            stroke = 5.dp
                        ) {
                            Text("${lastReadIndex + 1}", style = MaterialTheme.typography.titleMedium, color = p.primary)
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier.weight(1f)) {
                            Text("متابعة من حيث توقفت", style = MaterialTheme.typography.titleMedium, color = p.primary)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                "${AzkarData.titleFor(lastReadCategory)} · الذكر ${lastReadIndex + 1} من $categorySize",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    BaqiyatButton(
                        text = "أكمل الورد الآن",
                        onClick = { onNavigateToAzkar(lastReadCategory) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))
            Text("وردك اليومي", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                WirdCard(Modifier.weight(1f), "الصباح", "ابدأ يومك بنور", Icons.Default.WbSunny) { onNavigateToAzkar("sabah") }
                WirdCard(Modifier.weight(1f), "المساء", "اختم يومك بسكينة", Icons.Default.NightsStay) { onNavigateToAzkar("masaa") }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                WirdCard(Modifier.weight(1f), "النوم", "طمأنينة قبل النوم", Icons.Default.AirlineSeatFlat) { onNavigateToAzkar("sleep") }
                WirdCard(Modifier.weight(1f), "السفر", "حفظ وأمان", Icons.Default.DirectionsCar) { onNavigateToAzkar("travel") }
            }

            Spacer(modifier = Modifier.height(28.dp))
            Text("لمحة عن إنجازك", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard(Modifier.weight(1f), "$totalTasbeeh", "تسبيحة", Icons.Default.AutoAwesome)
                StatCard(Modifier.weight(1f), if (completedToday) "مكتمل" else "ابدأ الآن", "ورد اليوم", Icons.Default.SelfImprovement)
            }
            Spacer(modifier = Modifier.height(96.dp))
        }
    }
}

@Composable
private fun HeroCard() {
    val p = Baqiyat.colors
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(Brush.linearGradient(listOf(p.buttonFrom, p.buttonTo)))
            .padding(24.dp)
    ) {
        Icon(
            Icons.Default.SelfImprovement, null,
            Modifier.align(Alignment.TopEnd).size(64.dp),
            tint = p.onButton.copy(alpha = .22f)
        )
        Column(Modifier.fillMaxWidth(0.8f)) {
            Text(
                "وَاذْكُرْ رَبَّكَ إِذَا نَسِيتَ",
                style = ZekrTextStyle.copy(fontSize = 22.sp, lineHeight = 40.sp),
                color = p.onButton
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text("اجعل لليوم نصيباً من الذكر", style = MaterialTheme.typography.titleMedium, color = p.onButton)
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                "دقائق قليلة تصنع فرقاً كبيراً في قلبك.",
                style = MaterialTheme.typography.bodyMedium,
                color = p.onButton.copy(alpha = .82f)
            )
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
    GlassCard(modifier.fillMaxWidth()) {
        Text("تحدي اليوم", style = MaterialTheme.typography.titleMedium, color = Baqiyat.colors.primary)
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            if (both) "أحسنت — أكملت ورد الصباح والمساء." else "أكمل أذكار الصباح والمساء لتحصل على نجمة اليوم.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            BaqiyatChip(if (sabahDone) "الصباح ✓" else "الصباح", selected = sabahDone, onClick = onOpenSabah)
            BaqiyatChip(if (masaaDone) "المساء ✓" else "المساء", selected = masaaDone, onClick = onOpenMasaa)
            BaqiyatChip("$tasbeehToday تسبيحة", enabled = false)
        }
    }
}

@Composable
private fun WirdCard(modifier: Modifier, title: String, subtitle: String, icon: ImageVector, onClick: () -> Unit) {
    val p = Baqiyat.colors
    GlassCard(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        onClick = onClick,
        clickLabel = title,
        contentPadding = PaddingValues(18.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            Modifier.size(52.dp).clip(CircleShape).background(p.accent.copy(alpha = .14f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, Modifier.size(28.dp), tint = p.primary)
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(title, style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun StatCard(modifier: Modifier, value: String, label: String, icon: ImageVector) {
    val p = Baqiyat.colors
    GlassCard(modifier, shape = RoundedCornerShape(24.dp), contentPadding = PaddingValues(16.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, Modifier.size(26.dp), tint = p.accent)
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(value, style = MaterialTheme.typography.titleMedium, color = p.primary)
                Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
