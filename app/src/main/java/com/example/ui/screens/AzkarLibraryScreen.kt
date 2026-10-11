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
import androidx.compose.material3.OutlinedButton
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
import com.example.data.ArabicSearchNormalizer
import com.example.data.TopicAzkarData
import com.example.data.Zekr
import com.example.ui.AppViewModel
import com.example.ui.AudioPlayer

private val searchSuggestions = listOf("الصباح", "المساء", "النوم", "السفر", "القلق", "الرزق", "المرض", "الاستيقاظ", "التحصين")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AzkarLibraryScreen(
    viewModel: AppViewModel,
    onOpenAzkar: (String) -> Unit,
    onStartDailyWird: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    var showSaved by remember { mutableStateOf(false) }
    val savedAzkar by viewModel.myAzkar.collectAsState()
    val audioSpeed by viewModel.audioSpeed.collectAsState()
    val context = LocalContext.current
    val player = remember(context) { AudioPlayer(context) }
    val playing by player.isPlaying.collectAsState()
    androidx.compose.runtime.LaunchedEffect(audioSpeed) { player.setSpeechRate(audioSpeed) }
    DisposableEffect(player) { onDispose { player.shutdown() } }

    val allAzkar = remember { AzkarData.morningAzkar + AzkarData.eveningAzkar + TopicAzkarData.items }
    val normalizedQuery = remember(query) {
        ArabicSearchNormalizer.normalize(query)
    }
    val results = remember(normalizedQuery, allAzkar) {
        if (normalizedQuery.isBlank()) {
            emptyList()
        } else {
            val morningIntent = listOf("صباح", "صبح", "فجر").any(normalizedQuery::contains)
            val eveningIntent = listOf("مساء", "عصر", "مغرب").any(normalizedQuery::contains)
            val protectionIntent = listOf("تحصين", "حفظ", "حسد").any(normalizedQuery::contains)
            val sleepIntent = listOf("نوم", "فراش").any(normalizedQuery::contains)
            val travelIntent = listOf("سفر", "سيارة", "طريق", "رحلة", "مسافر").any(normalizedQuery::contains)
            val worryIntent = normalizedQuery == "هم" ||
                listOf("قلق", "خوف", "الهم", "حزن", "ضيق", "كرب", "توتر", "فزع")
                    .any(normalizedQuery::contains)
            val rizqIntent = listOf("رزق", "معيشة", "عمل", "بركة", "مال").any(normalizedQuery::contains)
            val illnessIntent = listOf("مرض", "مريض", "شفاء", "صحة", "وجع").any(normalizedQuery::contains)
            val wakingIntent = listOf("استيقاظ", "استيقظ", "يقظة", "صحوت", "صحونا", "اصبحت", "اصبحنا").any(normalizedQuery::contains)
            val istighfarIntent = normalizedQuery.contains("استغفار")
            allAzkar.filter { zekr ->
                ArabicSearchNormalizer.normalize(zekr.text).contains(normalizedQuery) ||
                    ArabicSearchNormalizer.normalize(zekr.source).contains(normalizedQuery) ||
                    ArabicSearchNormalizer.normalize(zekr.fadl).contains(normalizedQuery) ||
                    (morningIntent && zekr.category == "sabah") ||
                    (eveningIntent && zekr.category == "masaa") ||
                    (protectionIntent && (zekr.id in 1..2 || zekr.id in 9..10)) ||
                    (istighfarIntent && ArabicSearchNormalizer.normalize(zekr.text).contains("فاغفر لي")) ||
                    (sleepIntent && zekr.category == "sleep") ||
                    (travelIntent && zekr.category == "travel") ||
                    (worryIntent && zekr.category == "worry") ||
                    (rizqIntent && zekr.category == "rizq") ||
                    (illnessIntent && zekr.category == "illness") ||
                    (wakingIntent && zekr.category == "waking")
            }
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
                        Spacer(Modifier.height(8.dp))
                        OutlinedButton(onClick = onStartDailyWird, modifier = Modifier.fillMaxWidth()) {
                            Text("ابدأ الورد اليومي المتتابع")
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
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    searchSuggestions.chunked(3).forEach { suggestionsRow ->
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            suggestionsRow.forEach { suggestion ->
                                FilterChip(
                                    selected = query == suggestion,
                                    onClick = { query = suggestion; showSaved = false },
                                    label = { Text(suggestion) }
                                )
                            }
                        }
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
                    Button(onClick = { showSaved = true }) { Text("أذكاري المحفوظة (${savedAzkar.size})") }
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
                items(results, key = { "${it.category}-${it.id}" }) { zekr ->
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
            Text("التكرار: ${zekr.repeatCount}", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
            if (zekr.source.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text("المصدر: ${zekr.source}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
