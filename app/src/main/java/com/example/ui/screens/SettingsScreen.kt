package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    val userSignedIn by viewModel.userSignedIn.collectAsState()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "الإعدادات ⚙️",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(32.dp))

            // 1. التخصيص
            SettingsGroup(title = "🎨 التخصيص") {
                SettingsToggleRow("الوضع الداكن", isDarkMode) { viewModel.setDarkMode(it) }
                SettingsToggleRow("إظهار الفضائل", !hideVirtues) { viewModel.setHideVirtues(!it) }
                SettingsToggleRow("إظهار المصادر", !hideSources) { viewModel.setHideSources(!it) }
                SettingsToggleRow("إبقاء الشاشة مضاءة", keepScreenOn) { viewModel.setKeepScreenOn(it) }
                
                Spacer(modifier = Modifier.height(8.dp))
                Text("حجم الخط", fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
                Slider(
                    value = fontSize,
                    onValueChange = { viewModel.setFontSize(it) },
                    valueRange = 16f..48f,
                    steps = 15,
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary
                    )
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 2. الصوت والتفاعل
            SettingsGroup(title = "🔊 الصوت والتفاعل") {
                SettingsToggleRow("الهزاز عند الضغط", isVibrationEnabled) { viewModel.setVibration(it) }
                // Future placeholders for voice speed, etc can go here
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 3. الحساب والمزامنة
            SettingsGroup(title = "☁️ الحساب والمزامنة") {
                if (userSignedIn) {
                    Text(
                        "تم تسجيل الدخول",
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                    Button(
                        onClick = { viewModel.syncData() },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer)
                    ) {
                        Text("مزامنة البيانات الآن")
                    }
                    Button(
                        onClick = { viewModel.signOut() },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer, contentColor = MaterialTheme.colorScheme.onErrorContainer)
                    ) {
                        Text("تسجيل الخروج")
                    }
                } else {
                    Text(
                        "سجل الدخول لحفظ تقدمك في السحابة ومزامنته بين الأجهزة.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    Button(
                        onClick = { viewModel.signIn() },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("تسجيل الدخول باستخدام Google")
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(48.dp))
        }
    }
}

@Composable
fun SettingsGroup(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = title,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 12.dp, start = 8.dp)
        )
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp)),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                content = content
            )
        }
    }
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
        Text(title, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                checkedTrackColor = MaterialTheme.colorScheme.primary
            )
        )
    }
}
