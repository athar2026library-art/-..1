package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AzkarData
import com.example.data.Zekr
import com.example.ui.AppViewModel
import com.example.ui.AudioPlayer

private val searchSuggestions = listOf("الصباح", "المساء", "التحصين", "الاستغفار")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AzkarLibraryScreen(
    viewModel: AppViewModel,
    onOpenAzkar: (String) -> Unit
) {
    var query by remember { mutableStateOf("") }
    var showSaved by remember { mutableStateOf(false) }
    val savedAzkar by viewModel.myAzkar.collectAsState()
    val context = LocalContext.current
    val player = remember(context) { AudioPlayer(context) }
    val playing by player.isPlaying.collectAsState()
    DisposableEffect(player) { onDispose { player.shutdown() } }

    val allAzkar = remember { AzkarData.morningAzkar + AzkarData.eveningAzkar }
    val normalizedQuery = query.trim()
    val results = remember(normalizedQuery, allAzkar) {
        if (normalizedQuery.isBlank()) emptyList()
        else allAzkar.filter { zekr ->
            zekr.text.contains(normalizedQuery, ignoreCase = true) ||
                zekr.source.contains(normalizedQuery, ignoreCase = true) ||
                zekr.fadl.contains(normalizedQuery, ignoreCase = true) ||
                (normalizedQuery.contains("صباح") && zekr.category == "sabah") ||
                (normalizedQuery.contains("مساء") && zekr.category == "masaa") ||
                (normalizedQuery.contains("تحصين") && (zekr.id in 1..2 || zekr.id in 9..10)) ||
                (normalizedQuery.contains("استغفار") && zekr.text.contains("فاغفر لي"))
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("البحث ومكتبتي", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Column(Modifier.padding(18.dp)) {
                        Text("وردك اليوم", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        Spacer(Modifier.height(6.dp))
                        Text("اختر ورد الصباح أو المساء، وسيُحفظ تقدمك تلقائياً على الجهاز.", color = MaterialTheme.colorScheme.onPrimaryContainer)
                        Spacer(Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = { onOpenAzkar("sabah") }) { Text("ورد الصباح") }
                            Button(onClick = { onOpenAzkar("masaa") }) { Text("ورد المساء") }
                        }
                    }
                }
            }
            item {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it; showSaved = false },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("ابحث في نص الذكر أو مصدره") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    shape = RoundedCornerShape(18.dp)
                )
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    searchSuggestions.forEach { suggestion ->
                        FilterChip(
                            selected = query == suggestion,
                            onClick = { query = suggestion; showSaved = false },
                            label = { Text(suggestion) }
                        )
                    }
                }
            }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(onClick = { showSaved = false }) { Text("نتائج البحث") }
                    Button(onClick = { showSaved = true }) { Text("أذكاري المحفوظة (\${savedAzkar.size})") }
                }
            }

            if (showSaved) {
                if (savedAzkar.isEmpty()) {
                    item { Text("لم تحفظ أذكاراً بعد. ابحث عن ذكر ثم اضغط علامة الحفظ.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                }
                items(savedAzkar, key = { it }) { saved ->
                    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) {
                        Column(Modifier.padding(16.dp)) {
                            Text(saved, fontSize = 18.sp, lineHeight = 30.sp, textAlign = TextAlign.Start)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                IconButton(onClick = { if (playing) player.stop() else player.playLoop(saved, 1) }) {
                                    Icon(if (playing) Icons.Default.Stop else Icons.Default.PlayArrow, contentDescription = "استماع")
                                }
                                Button(onClick = { viewModel.removeMyZekr(saved) }) { Text("إزالة") }
                            }
                        }
                    }
                }
            } else if (normalizedQuery.isNotBlank()) {
                if (results.isEmpty()) {
                    item {
                        Text(
                            "لا توجد نتيجة في الأذكار المتاحة حالياً. جرّب كلمة من نص الذكر أو اختر الصباح أو المساء.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                items(results, key = { "\${it.category}-\${it.id}" }) { zekr ->
                    ZekrSearchCard(
                        zekr = zekr,
                        isSaved = savedAzkar.contains(zekr.text),
                        onSave = { viewModel.saveMyZekr(zekr.text) },
                        onPlay = { player.stop(); player.playLoop(zekr.text, 1) }
                    )
                }
            } else {
                item {
                    Text(
                        "ابدأ بكتابة كلمة للبحث. تعرض النتائج نص الذكر والمصدر والعدد الوارد في بيانات التطبيق.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun ZekrSearchCard(
    zekr: Zekr,
    isSaved: Boolean,
    onSave: () -> Unit,
    onPlay: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(zekr.text, fontSize = 19.sp, lineHeight = 32.sp, textAlign = TextAlign.Start)
            Spacer(Modifier.height(10.dp))
            Text("التكرار: \${zekr.repeatCount}", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
            if (zekr.source.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text("المصدر: \${zekr.source}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onPlay) { Icon(Icons.Default.PlayArrow, contentDescription = "استماع") }
                IconButton(onClick = onSave, enabled = !isSaved) {
                    Icon(Icons.Default.Bookmark, contentDescription = "حفظ", tint = if (isSaved) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(if (isSaved) "محفوظ في أذكاري" else "إضافة إلى أذكاري", fontSize = 13.sp)
            }
        }
    }
}
