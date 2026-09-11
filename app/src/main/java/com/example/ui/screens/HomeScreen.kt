package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.AppViewModel
import com.example.data.AzkarData
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: AppViewModel,
    onNavigateToAzkar: (String) -> Unit
) {
    val lastReadCategory by viewModel.lastReadCategory.collectAsState()
    val lastReadIndex by viewModel.lastReadIndex.collectAsState()
    val recentProgress by viewModel.recentProgress.collectAsState()
    
    val categorySize = if (lastReadCategory == "sabah") AzkarData.morningAzkar.size else AzkarData.eveningAzkar.size
    val isWirdActive = lastReadCategory.isNotEmpty() && lastReadIndex < categorySize

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = "حصنك في يومك",
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "صباحك ومساءك بطمأنينة 🌿",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            
            Spacer(modifier = Modifier.height(48.dp))
            
            // "وردك اليوم" Card
            if (isWirdActive) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .clickable { onNavigateToAzkar(lastReadCategory) },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Text(
                            text = "وردك اليوم",
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        val categoryName = if (lastReadCategory == "sabah") "🌅 أذكار الصباح" else "🌙 أذكار المساء"
                        Text(
                            text = categoryName,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "${lastReadIndex} من $categorySize",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        LinearProgressIndicator(
                            progress = { lastReadIndex.toFloat() / categorySize },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "متابعة الورد",
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
            
            // Quick Start Cards
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Sabah Card
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(24.dp))
                        .clickable { onNavigateToAzkar("sabah") },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("🌅", fontSize = 32.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("الصباح", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("ابدأ", color = MaterialTheme.colorScheme.primary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    }
                }
                
                // Masaa Card
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(24.dp))
                        .clickable { onNavigateToAzkar("masaa") },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("🌙", fontSize = 32.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("المساء", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("ابدأ", color = MaterialTheme.colorScheme.primary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            val streak = calculateStreak(recentProgress)
            if (streak > 0) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "🔥 $streak أيام متتالية",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "واصل وردك اليومي",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                    )
                }
            } else {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "🌱 ابدأ رحلتك اليوم",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "حافظ على أذكارك لتنمو سلسلة أيامك",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}
