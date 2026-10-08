package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.ArabicText
import com.example.data.AzkarData
import com.example.data.CustomWird
import com.example.data.CustomWirds
import com.example.ui.AppViewModel
import com.example.ui.components.BaqiyatBackButton
import com.example.ui.components.BaqiyatButton
import com.example.ui.components.GlassCard
import com.example.ui.theme.Baqiyat
import com.example.ui.theme.ReaderFont
import com.example.ui.theme.zekrStyle

/** محرر الورد: اسم واختيار أذكار بالترتيب الذي تُضغط به. wirdId = null لورد جديد. */
@Composable
fun WirdEditorScreen(viewModel: AppViewModel, wirdId: String?, onDone: () -> Unit) {
    val existing = remember(wirdId) { viewModel.customWirds.value.firstOrNull { it.id == wirdId } }
    var name by rememberSaveable(wirdId) { mutableStateOf(existing?.name.orEmpty()) }
    var selected by rememberSaveable(wirdId) { mutableStateOf(ArrayList(existing?.ids.orEmpty())) }
    var query by rememberSaveable { mutableStateOf("") }
    val all = remember { AzkarData.all() }
    val shown = remember(query) {
        val q = ArabicText.normalize(query)
        if (q.isEmpty()) all
        else all.filter {
            ArabicText.normalize(it.text).contains(q) ||
                ArabicText.normalize(it.source).contains(q) ||
                ArabicText.normalize(AzkarData.titleFor(it.category)).contains(q)
        }
    }
    val p = Baqiyat.colors
    val canSave = CustomWirds.cleanName(name).isNotEmpty() && selected.isNotEmpty()

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text(if (existing != null) "تعديل الورد" else "ورد جديد", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = { BaqiyatBackButton(onDone) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        bottomBar = {
            Column(Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
                BaqiyatButton(
                    text = "حفظ الورد (${selected.size})",
                    enabled = canSave,
                    onClick = {
                        viewModel.saveCustomWird(
                            CustomWird(
                                id = wirdId ?: CustomWirds.newId(),
                                name = CustomWirds.cleanName(name),
                                ids = selected.toList()
                            )
                        )
                        onDone()
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp),
            contentPadding = PaddingValues(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.take(CustomWirds.MAX_NAME) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("اسم الورد") },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp)
                )
            }
            item {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("ابحث لتضيف أذكاراً…") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(50)
                )
            }
            item {
                Text(
                    "اختر الأذكار بالترتيب الذي تريد قراءتها به. المختار: ${selected.size}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            items(shown, key = { it.id }) { z ->
                val index = selected.indexOf(z.id)
                val checked = index >= 0
                GlassCard(
                    Modifier.fillMaxWidth().clickable(role = Role.Checkbox) {
                        selected = if (checked) ArrayList(selected - z.id) else ArrayList(selected + z.id)
                    },
                    contentPadding = PaddingValues(14.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = checked, onCheckedChange = null)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                AzkarData.titleFor(z.category) + if (checked) " · رقم ${index + 1}" else "",
                                style = MaterialTheme.typography.labelMedium,
                                color = p.accent
                            )
                            Text(
                                z.text,
                                style = zekrStyle(ReaderFont.AMIRI, 17f),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}
