package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
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
        topBar = {
            TopAppBar(
                title = { Text("سجل الإنجاز 📅") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            val streak = calculateStreak(recentProgress)
            val thisMonthDays = calculateThisMonthDays(recentProgress)
            val sabahPercent = calculateSabahPercent(recentProgress)
            val masaaPercent = calculateMasaaPercent(recentProgress)

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("التزامك الحالي 🔥", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "$streak أيام",
                        style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("هذا الشهر", style = MaterialTheme.typography.labelMedium)
                            Text("$thisMonthDays/30", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("الصباح", style = MaterialTheme.typography.labelMedium)
                            Text("$sabahPercent%", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("المساء", style = MaterialTheme.typography.labelMedium)
                            Text("$masaaPercent%", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            Text("بستان الإنجاز 🌴", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))

            if (recentProgress.isEmpty()) {
                Text("لم تبدأ بعد، استعن بالله وابدأ اليوم!", modifier = Modifier.padding(16.dp))
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 64.dp),
                    contentPadding = PaddingValues(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(recentProgress.size) { index ->
                        val progress = recentProgress[index]
                        val hasCompleted = progress.completedSabah || progress.completedMasaa
                        
                        val emoji = when {
                            !hasCompleted -> "🌱" // Did nothing
                            streak >= 100 -> "🌳✨"
                            streak >= 30 -> "🌳"
                            streak >= 7 -> "🌴"
                            streak >= 3 -> "🌿"
                            else -> "🌱"
                        }
                        
                        Box(
                            modifier = Modifier
                                .aspectRatio(1f)
                                .background(
                                    if (hasCompleted) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                    RoundedCornerShape(8.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (hasCompleted) emoji else "🌱",
                                fontSize = 32.sp
                            )
                        }
                    }
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
    
    // Sort descending by date just to be sure
    val sorted = progress.sortedByDescending { it.date }
    
    // Check if the latest record is today or yesterday
    val todayStr = sdf.format(cal.time)
    cal.add(Calendar.DAY_OF_YEAR, -1)
    val yesterdayStr = sdf.format(cal.time)
    
    val latest = sorted.first()
    if (latest.date != todayStr && latest.date != yesterdayStr) {
        return 0 // Streak broken if no activity today or yesterday
    }
    
    var expectedDateStr = latest.date
    val expectedCal = Calendar.getInstance()
    expectedCal.time = sdf.parse(expectedDateStr) ?: Date()
    
    for (p in sorted) {
        if (p.date == expectedDateStr && (p.completedSabah || p.completedMasaa)) {
            streak++
            expectedCal.add(Calendar.DAY_OF_YEAR, -1)
            expectedDateStr = sdf.format(expectedCal.time)
        } else {
            break // Date gap or no completion
        }
    }
    return streak
}

fun calculateThisMonthDays(progress: List<UserProgress>): Int {
    val sdf = SimpleDateFormat("yyyy-MM", Locale.US)
    val currentMonth = sdf.format(Date())
    return progress.count { 
        it.date.startsWith(currentMonth) && (it.completedSabah || it.completedMasaa) 
    }
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
