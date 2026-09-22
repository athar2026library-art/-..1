package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.AppViewModel
import com.example.data.UserProgress
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
    viewModel: AppViewModel,
    onNavigateBack: () -> Unit
) {
    val recentProgress by viewModel.recentProgress.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("تقدمي 🌿", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val streak = calculateStreak(recentProgress)
            val longestStreak = calculateLongestStreak(recentProgress)
            val totalAzkar = recentProgress.sumOf { it.totalTasbeeh }
            val completedDays = recentProgress.count { it.completedSabah || it.completedMasaa }

            Text(
                text = "🔥 $streak يومًا",
                fontSize = 40.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "أيام الالتزام المتتالية",
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
            )
            if (longestStreak > streak) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "أطول سلسلة: $longestStreak يوم",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.secondary
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Monthly calendar
            MonthlyCalendar(recentProgress)

            Spacer(modifier = Modifier.height(28.dp))

            // Weekly chart
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "إحصائيات التسبيح الأسبوعية",
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.Bottom
                ) {
                    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                    val dayFormat = SimpleDateFormat("EE", Locale.forLanguageTag("ar"))

                    var maxTasbeeh = 1
                    val weekData = mutableListOf<Pair<String, Int>>()
                    for (i in 6 downTo 0) {
                        val calDay = Calendar.getInstance()
                        calDay.add(Calendar.DAY_OF_YEAR, -i)
                        val dateStr = sdf.format(calDay.time)
                        val progress = recentProgress.find { it.date == dateStr }
                        val tasbeeh = progress?.totalTasbeeh ?: 0
                        if (tasbeeh > maxTasbeeh) maxTasbeeh = tasbeeh
                        weekData.add(Pair(dayFormat.format(calDay.time), tasbeeh))
                    }

                    for ((dayName, tasbeehCount) in weekData) {
                        val heightFraction = (tasbeehCount.toFloat() / maxTasbeeh.toFloat()).coerceIn(0.1f, 1f)
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Bottom,
                            modifier = Modifier.fillMaxHeight()
                        ) {
                            if (tasbeehCount > 0) {
                                Text(text = tasbeehCount.toString(), fontSize = 10.sp, color = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.height(4.dp))
                            }
                            Box(
                                modifier = Modifier
                                    .width(16.dp)
                                    .fillMaxHeight(heightFraction)
                                    .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                                    .background(
                                        if (tasbeehCount > 0) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.surfaceVariant
                                    )
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(text = dayName, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("إجمالي التسبيح", fontSize = 14.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("$totalAzkar", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("أيام مكتملة", fontSize = 14.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("$completedDays", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "الإنجازات",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.align(Alignment.Start)
            )
            Spacer(modifier = Modifier.height(12.dp))

            AchievementRow(
                title = "أسبوع متواصل",
                subtitle = "7 أيام التزام",
                unlocked = longestStreak >= 7 || streak >= 7,
                icon = Icons.Default.Star
            )
            Spacer(modifier = Modifier.height(10.dp))
            AchievementRow(
                title = "أربعون يوماً",
                subtitle = "سلسلة 40 يوماً",
                unlocked = longestStreak >= 40,
                icon = Icons.Default.EmojiEvents
            )
            Spacer(modifier = Modifier.height(10.dp))
            AchievementRow(
                title = "ألف تسبيحة",
                subtitle = "إجمالي 1000 تسبيحة",
                unlocked = totalAzkar >= 1000,
                icon = Icons.Default.Star
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun MonthlyCalendar(progress: List<UserProgress>) {
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    val monthTitleFmt = SimpleDateFormat("MMMM yyyy", Locale.forLanguageTag("ar"))
    val cal = Calendar.getInstance()
    val year = cal.get(Calendar.YEAR)
    val month = cal.get(Calendar.MONTH)
    val today = cal.get(Calendar.DAY_OF_MONTH)

    val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    cal.set(Calendar.DAY_OF_MONTH, 1)
    // Calendar.SUNDAY=1 … Saturday=7 — shift so week starts Saturday for AR feel optional; use Sunday=0 grid
    val firstWeekday = (cal.get(Calendar.DAY_OF_WEEK) + 5) % 7 // Mon=0 … Sun=6 simplified: use SUNDAY based
    // Standard: first cell offset = dayOfWeek - 1 (Sunday first)
    cal.set(Calendar.DAY_OF_MONTH, 1)
    val offset = cal.get(Calendar.DAY_OF_WEEK) - 1

    val byDate = progress.associateBy { it.date }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp)
    ) {
        Text(
            text = monthTitleFmt.format(Calendar.getInstance().time),
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(12.dp))

        val dayNames = listOf("ح", "ن", "ث", "ر", "خ", "ج", "س")
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            dayNames.forEach { d ->
                Text(
                    d,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))

        val cells = mutableListOf<Int?>()
        repeat(offset) { cells.add(null) }
        for (day in 1..daysInMonth) cells.add(day)
        while (cells.size % 7 != 0) cells.add(null)

        cells.chunked(7).forEach { week ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                week.forEach { day ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .padding(2.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (day != null) {
                            val dateStr = String.format(Locale.US, "%04d-%02d-%02d", year, month + 1, day)
                            val p = byDate[dateStr]
                            val both = p?.completedSabah == true && p.completedMasaa == true
                            val one = p != null && (p.completedSabah || p.completedMasaa || p.totalTasbeeh > 0)
                            val bg = when {
                                both -> MaterialTheme.colorScheme.primary
                                one -> MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
                                day == today -> MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                                else -> Color.Transparent
                            }
                            val fg = when {
                                both -> MaterialTheme.colorScheme.onPrimary
                                else -> MaterialTheme.colorScheme.onSurface
                            }
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape)
                                    .background(bg),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "$day",
                                    fontSize = 12.sp,
                                    color = fg,
                                    fontWeight = if (day == today) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally)
        ) {
            LegendDot(MaterialTheme.colorScheme.primary, "صباح+مساء")
            LegendDot(MaterialTheme.colorScheme.primary.copy(alpha = 0.35f), "نشاط")
        }
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(Modifier.width(6.dp))
        Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun AchievementRow(
    title: String,
    subtitle: String,
    unlocked: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (unlocked)
                MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
            else
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = if (unlocked) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f),
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text(
                    title,
                    fontWeight = FontWeight.Bold,
                    color = if (unlocked) MaterialTheme.colorScheme.onSurface
                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
                )
                Text(subtitle, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(modifier = Modifier.weight(1f))
            Text(if (unlocked) "✓" else "🔒", fontSize = 18.sp)
        }
    }
}

fun calculateStreak(progress: List<UserProgress>): Int {
    if (progress.isEmpty()) return 0
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    val cal = Calendar.getInstance()
    var streak = 0
    val sorted = progress.sortedByDescending { it.date }
    val todayStr = sdf.format(cal.time)
    cal.add(Calendar.DAY_OF_YEAR, -1)
    val yesterdayStr = sdf.format(cal.time)
    val latest = sorted.first()
    if (latest.date != todayStr && latest.date != yesterdayStr) return 0
    var expectedDateStr = latest.date
    val expectedCal = Calendar.getInstance()
    expectedCal.time = sdf.parse(expectedDateStr) ?: Date()
    for (p in sorted) {
        if (p.date == expectedDateStr && (p.completedSabah || p.completedMasaa || p.totalTasbeeh > 0)) {
            streak++
            expectedCal.add(Calendar.DAY_OF_YEAR, -1)
            expectedDateStr = sdf.format(expectedCal.time)
        } else break
    }
    return streak
}

fun calculateLongestStreak(progress: List<UserProgress>): Int {
    if (progress.isEmpty()) return 0
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    val sorted = progress.sortedBy { it.date }
    var maxStreak = 0
    var current = 0
    var prevDate: Calendar? = null
    for (p in sorted) {
        val active = p.completedSabah || p.completedMasaa || p.totalTasbeeh > 0
        if (!active) {
            current = 0
            prevDate = null
            continue
        }
        val cal = Calendar.getInstance()
        cal.time = sdf.parse(p.date) ?: continue
        if (prevDate == null) current = 1
        else {
            val expected = prevDate.clone() as Calendar
            expected.add(Calendar.DAY_OF_YEAR, 1)
            current = if (cal.get(Calendar.YEAR) == expected.get(Calendar.YEAR) &&
                cal.get(Calendar.DAY_OF_YEAR) == expected.get(Calendar.DAY_OF_YEAR)
            ) current + 1 else 1
        }
        if (current > maxStreak) maxStreak = current
        prevDate = cal
    }
    return maxStreak
}
