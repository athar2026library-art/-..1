package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.ui.AppViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: AppViewModel,
    onNavigateBack: () -> Unit
) {
    val fontSize by viewModel.fontSize.collectAsState()
    val isDarkMode by viewModel.isDarkMode.collectAsState()
    val isVibrationEnabled by viewModel.isVibrationEnabled.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("الإعدادات ⚙️") },
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
            Text("حجم الخط: ${fontSize.toInt()}", style = MaterialTheme.typography.titleMedium)
            Slider(
                value = fontSize,
                onValueChange = { viewModel.setFontSize(it) },
                valueRange = 16f..48f,
                steps = 15
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("الوضع الليلي", style = MaterialTheme.typography.titleMedium)
                Switch(
                    checked = isDarkMode,
                    onCheckedChange = { viewModel.setDarkMode(it) }
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("الهزاز عند إتمام الذكر", style = MaterialTheme.typography.titleMedium)
                Switch(
                    checked = isVibrationEnabled,
                    onCheckedChange = { viewModel.setVibration(it) }
                )
            }
        }
    }
}
