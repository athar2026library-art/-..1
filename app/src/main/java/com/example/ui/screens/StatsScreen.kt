package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = "تقدمي 🌿",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            
            Spacer(modifier = Modifier.height(48.dp))
            
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
                text = "سلسلة الالتزام",
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
            )
            
            Spacer(modifier = Modifier.height(48.dp))
            
            // This week
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "هذا الأسبوع",
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    // Just a mock display for 7 days based on recent progress
                    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                    val cal = Calendar.getInstance()
                    // Get last 7 days
                    for (i in 6 downTo 0) {
                        val calDay = Calendar.getInstance()
                        calDay.add(Calendar.DAY_OF_YEAR, -i)
                        val dateStr = sdf.format(calDay.time)
                        val progress = recentProgress.find { it.date == dateStr }
                        val done = progress?.completedSabah == true || progress?.completedMasaa == true
                        
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(
                                    if (done) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (done) {
                                Text("✓", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
                            } else {
                                Text("·", color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(48.dp))
            
            // Sabah / Masaa completion
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Card(
                    modifier = Modifier.weight(1f).clip(RoundedCornerShape(24.dp)),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp).fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("الصباح", fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("$sabahPercent%", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                }
                
                Card(
                    modifier = Modifier.weight(1f).clip(RoundedCornerShape(24.dp)),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp).fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("المساء", fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("$masaaPercent%", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
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
                    Text("أذكار مقروءة", fontSize = 14.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("$totalAzkar", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("أيام مكتملة", fontSize = 14.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("$completedDays", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// Keeping the helper functions from before
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
        if (p.date == expectedDateStr && (p.completedSabah || p.completedMasaa)) {
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
