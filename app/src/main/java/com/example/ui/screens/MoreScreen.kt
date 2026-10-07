package com.example.ui.screens

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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.BuildConfig
import com.example.ui.AppViewModel
import com.example.ui.components.GlassCard
import com.example.ui.theme.Baqiyat

@Composable
fun MoreScreen(
    viewModel: AppViewModel,
    onOpenAssistant: () -> Unit,
    onOpenFeedback: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val unread by viewModel.unreadFeedbackCount.collectAsStateWithLifecycle()
    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text("المزيد", style = MaterialTheme.typography.titleLarge) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            MoreRow(Icons.AutoMirrored.Outlined.Chat, "المساعد الذكي", "اسأل عن ذكر أو صف شعورك", 0, onOpenAssistant)
            MoreRow(Icons.Default.Email, "تواصل معنا", "اقتراحاتك وملاحظاتك وردود الفريق", unread, onOpenFeedback)
            MoreRow(Icons.Default.Settings, "الإعدادات", "المظهر والقراءة والتذكيرات والحساب", 0, onOpenSettings)
            Spacer(Modifier.height(8.dp))
            Text(
                "الباقيات · الإصدار ${BuildConfig.VERSION_NAME}",
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(96.dp))
        }
    }
}

@Composable
private fun MoreRow(icon: ImageVector, title: String, subtitle: String, badge: Int, onClick: () -> Unit) {
    val p = Baqiyat.colors
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        clickLabel = title,
        contentPadding = PaddingValues(18.dp)
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(48.dp).clip(CircleShape).background(p.accent.copy(alpha = .14f)),
                contentAlignment = Alignment.Center
            ) { Icon(icon, null, Modifier.size(26.dp), tint = p.primary) }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (badge > 0) {
                Box(
                    Modifier.size(24.dp).clip(CircleShape).background(MaterialTheme.colorScheme.error),
                    contentAlignment = Alignment.Center
                ) { Text("$badge", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onError) }
                Spacer(Modifier.width(8.dp))
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
