package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.AppViewModel
import com.example.ui.screens.calculateStreak
import com.example.data.AzkarData

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: AppViewModel,
    onNavigateToAzkar: (String) -> Unit,
    onNavigateToStats: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToAiServices: () -> Unit
) {
    val todayProgress by viewModel.todayProgress.collectAsState()
    val recentProgress by viewModel.recentProgress.collectAsState()
    
    val lastReadCategory by viewModel.lastReadCategory.collectAsState()
    val lastReadIndex by viewModel.lastReadIndex.collectAsState()
    
    val streak = calculateStreak(recentProgress)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("حصنك في يومك", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = onNavigateToAiServices, modifier = Modifier.testTag("ai_button")) {
                        Text("✨")
                    }
                    IconButton(onClick = onNavigateToStats, modifier = Modifier.testTag("stats_button")) {
                        Icon(Icons.Default.DateRange, contentDescription = "الإحصائيات")
                    }
                    IconButton(onClick = onNavigateToSettings, modifier = Modifier.testTag("settings_button")) {
                        Icon(Icons.Default.Settings, contentDescription = "الإعدادات")
                    }
                }
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
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start
            ) {
                Column {
                    Text(
                        "السلام عليكم 👋",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "وردك اليوم",
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))

            if (lastReadCategory.isNotEmpty()) {
                val totalAzkar = if (lastReadCategory == "sabah") AzkarData.morningAzkar.size else AzkarData.eveningAzkar.size
                val categoryName = if (lastReadCategory == "sabah") "أذكار الصباح ☀️" else "أذكار المساء 🌙"
                
                ResumeCard(
                    categoryName = categoryName,
                    currentIndex = lastReadIndex + 1,
                    total = totalAzkar,
                    onClick = { onNavigateToAzkar(lastReadCategory) }
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Sabah Azkar Card
            val isSabahCompleted = todayProgress?.completedSabah == true
            DashboardCard(
                title = "أذكار الصباح ☀️",
                progress = if (isSabahCompleted) 1f else 0f,
                progressText = if (isSabahCompleted) "100%" else "لم تبدأ بعد",
                onClick = { onNavigateToAzkar("sabah") },
                isCompleted = isSabahCompleted
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Masaa Azkar Card
            val isMasaaCompleted = todayProgress?.completedMasaa == true
            DashboardCard(
                title = "أذكار المساء 🌙",
                progress = if (isMasaaCompleted) 1f else 0f,
                progressText = if (isMasaaCompleted) "100%" else "لم تبدأ بعد",
                onClick = { onNavigateToAzkar("masaa") },
                isCompleted = isMasaaCompleted
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            Divider(modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp))
            Spacer(modifier = Modifier.height(32.dp))

            // Streak Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onNavigateToStats),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(24.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "🔥 سلسلة الالتزام",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Text(
                        "$streak يومًا",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
fun ResumeCard(
    categoryName: String,
    currentIndex: Int,
    total: Int,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier.padding(20.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "👋 أكمل من حيث توقفت",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.tertiary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = categoryName,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "الذكر $currentIndex من $total",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f)
                )
            }
            CircularProgressIndicator(
                progress = { currentIndex.toFloat() / total },
                color = MaterialTheme.colorScheme.tertiary,
                trackColor = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.2f)
            )
        }
    }
}

@Composable
fun DashboardCard(
    title: String,
    progress: Float,
    progressText: String,
    onClick: () -> Unit,
    isCompleted: Boolean
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (isCompleted) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.primaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = if (isCompleted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onPrimaryContainer
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .weight(1f)
                        .height(10.dp)
                        .clip(RoundedCornerShape(5.dp)),
                    color = if (isCompleted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = progressText,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isCompleted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}
