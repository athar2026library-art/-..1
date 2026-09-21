package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    val recentProgress by viewModel.recentProgress.collectAsState()

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
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val streak = calculateStreak(recentProgress)
            val sabahPercent = calculateSabahPercent(recentProgress)
            val masaaPercent = calculateMasaaPercent(recentProgress)
            val totalAzkar = recentProgress.sumOf { it.totalTasbeeh }
            val completedDays = recentProgress.count { it.completedSabah || it.completedMasaa }

            // Streak Card
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
            
            Spacer(modifier = Modifier.height(48.dp))
            
            // This week Bar Chart
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
                    
                    // Find max tasbeeh to scale the bars
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
                                Text(
                                    text = tasbeehCount.toString(), 
                                    fontSize = 10.sp, 
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                            }
                            Box(
                                modifier = Modifier
                                    .width(16.dp)
                                    .fillMaxHeight(heightFraction)
                                    .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                                    .background(
                                        if (tasbeehCount > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                                    )
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(text = dayName, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Total Azkar and Days
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
    if (latest.date != todayStr && latest.date != yesterdayStr) {
        return 0
    }
    
    var expectedDateStr = latest.date
    val expectedCal = Calendar.getInstance()
    expectedCal.time = sdf.parse(expectedDateStr) ?: Date()
    
    for (p in sorted) {
        if (p.date == expectedDateStr && (p.completedSabah || p.completedMasaa || p.totalTasbeeh > 0)) {
            streak++
            expectedCal.add(Calendar.DAY_OF_YEAR, -1)
            expectedDateStr = sdf.format(expectedCal.time)
        } else {
            break
        }
    }
    return streak
}

fun calculateSabahPercent(progress: List<UserProgress>): Int {
    if (progress.isEmpty()) return 0
    val count = progress.count { it.completedSabah }
    return (count * 100) / progress.size
}

fun calculateMasaaPercent(progress: List<UserProgress>): Int {
    if (progress.isEmpty()) return 0
    val count = progress.count { it.completedMasaa }
    return (count * 100) / progress.size
}
