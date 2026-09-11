package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
    
    val keepScreenOn by viewModel.keepScreenOn.collectAsState()
    val hideVirtues by viewModel.hideVirtues.collectAsState()
    val hideSources by viewModel.hideSources.collectAsState()

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
                .verticalScroll(rememberScrollState())
        ) {
            
            SettingsCategory("المظهر")
            
            Text("حجم الخط: ${fontSize.toInt()}", style = MaterialTheme.typography.bodyLarge)
            Slider(
                value = fontSize,
                onValueChange = { viewModel.setFontSize(it) },
                valueRange = 16f..48f,
                steps = 15
            )
            
            SettingsToggleRow("الوضع الليلي", isDarkMode) { viewModel.setDarkMode(it) }

            Spacer(modifier = Modifier.height(24.dp))
            SettingsCategory("التفاعل والنظام")

            SettingsToggleRow("الهزاز عند إتمام الذكر والتسبيح", isVibrationEnabled) { viewModel.setVibration(it) }
            SettingsToggleRow("إبقاء الشاشة مضاءة أثناء القراءة", keepScreenOn) { viewModel.setKeepScreenOn(it) }

            Spacer(modifier = Modifier.height(24.dp))
            SettingsCategory("تخصيص المحتوى")

            SettingsToggleRow("إخفاء فضائل الأعمال", hideVirtues) { viewModel.setHideVirtues(it) }
            SettingsToggleRow("إخفاء المصادر وتخريج الأحاديث", hideSources) { viewModel.setHideSources(it) }
            
        }
    }
}

@Composable
fun SettingsCategory(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(bottom = 16.dp)
    )
}

@Composable
fun SettingsToggleRow(title: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}
