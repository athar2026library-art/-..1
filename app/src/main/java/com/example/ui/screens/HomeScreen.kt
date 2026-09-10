package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.AppViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: AppViewModel,
    onNavigateToAzkar: (String) -> Unit,
    onNavigateToStats: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val todayProgress by viewModel.todayProgress.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("حصنك في يومك", fontWeight = FontWeight.Bold) },
                actions = {
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
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val totalTasbeeh = todayProgress?.totalTasbeeh ?: 0
            if (totalTasbeeh > 0) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("مجموع التسبيحات اليوم", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "$totalTasbeeh",
                            style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        )
                    }
                }
            }

            AzkarCard(
                title = "أذكار الصباح ☀️",
                isCompleted = todayProgress?.completedSabah == true,
                onClick = { onNavigateToAzkar("sabah") },
                testTag = "sabah_button"
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            AzkarCard(
                title = "أذكار المساء 🌙",
                isCompleted = todayProgress?.completedMasaa == true,
                onClick = { onNavigateToAzkar("masaa") },
                testTag = "masaa_button"
            )
        }
    }
}

@Composable
fun AzkarCard(
    title: String,
    isCompleted: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp)
            .testTag(testTag),
        colors = CardDefaults.cardColors(
            containerColor = if (isCompleted) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.primaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = title,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isCompleted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onPrimaryContainer
                )
                if (isCompleted) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "تمت القراءة اليوم ✓",
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
