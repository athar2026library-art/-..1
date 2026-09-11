package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AzkarListScreen(
    onNavigateToAzkar: (String) -> Unit
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("الأذكار", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp),
            contentPadding = PaddingValues(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                AzkarCategoryCard(
                    title = "أذكار الصباح",
                    emoji = "🌅",
                    description = "تُقرأ من بعد صلاة الفجر حتى شروق الشمس",
                    onClick = { onNavigateToAzkar("sabah") }
                )
            }
            item {
                AzkarCategoryCard(
                    title = "أذكار المساء",
                    emoji = "🌙",
                    description = "تُقرأ من بعد صلاة العصر حتى غروب الشمس",
                    onClick = { onNavigateToAzkar("masaa") }
                )
            }
            // Placeholders for future azkar
            item {
                AzkarCategoryCard(
                    title = "أذكار النوم",
                    emoji = "😴",
                    description = "تُقرأ قبل النوم مباشرة",
                    onClick = { /* TODO */ }
                )
            }
            item {
                AzkarCategoryCard(
                    title = "أذكار بعد الصلاة",
                    emoji = "🕌",
                    description = "تُقرأ دبر كل صلاة مكتوبة",
                    onClick = { /* TODO */ }
                )
            }
        }
    }
}

@Composable
fun AzkarCategoryCard(
    title: String,
    emoji: String,
    description: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(20.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = emoji,
                fontSize = 32.sp,
                modifier = Modifier.padding(end = 16.dp)
            )
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}
