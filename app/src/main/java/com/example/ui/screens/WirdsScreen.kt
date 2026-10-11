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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.CustomWird
import com.example.ui.AppViewModel
import com.example.ui.components.BaqiyatBackButton
import com.example.ui.components.BaqiyatButton
import com.example.ui.components.GlassCard
import com.example.ui.theme.Baqiyat

/** «أوردي»: أوراد يرتبها المستخدم من أذكار التطبيق ويقرؤها كوردٍ واحد. */
@androidx.compose.material3.ExperimentalMaterial3Api
@Composable
fun WirdsScreen(
    viewModel: AppViewModel,
    onNavigateBack: () -> Unit,
    onRead: (String) -> Unit,
    onEdit: (String?) -> Unit
) {
    val wirds by viewModel.customWirds.collectAsStateWithLifecycle()
    var toDelete by remember { mutableStateOf<CustomWird?>(null) }
    val p = Baqiyat.colors

    toDelete?.let { w ->
        AlertDialog(
            onDismissRequest = { toDelete = null },
            title = { Text("حذف الورد") },
            text = { Text("سيُحذف «${w.name}» نهائياً. الأذكار نفسها لا تتأثر.") },
            confirmButton = {
                TextButton(onClick = { viewModel.deleteCustomWird(w.id); toDelete = null }) { Text("حذف") }
            },
            dismissButton = { TextButton(onClick = { toDelete = null }) { Text("إلغاء") } }
        )
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text("أوردي", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = { BaqiyatBackButton(onNavigateBack) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        if (wirds.isEmpty()) {
            Column(
                Modifier.fillMaxSize().padding(padding).padding(32.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(Icons.Default.AutoAwesome, null, Modifier.size(56.dp), tint = p.accent.copy(alpha = .6f))
                Spacer(Modifier.height(16.dp))
                Text("لا أوراد بعد", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(6.dp))
                Text(
                    "اجمع أذكاراً تحبها بترتيبك، واقرأها كوردٍ واحد: وردك بعد الفجر، أو قبل النوم.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(20.dp))
                BaqiyatButton(text = "أنشئ وردك الأول", onClick = { onEdit(null) })
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp),
                contentPadding = PaddingValues(top = 8.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item { BaqiyatButton(text = "ورد جديد", onClick = { onEdit(null) }, modifier = Modifier.fillMaxWidth()) }
                items(wirds, key = { it.id }) { w ->
                    GlassCard(
                        Modifier.fillMaxWidth(),
                        onClick = { onRead(w.id) },
                        clickLabel = "اقرأ ${w.name}",
                        contentPadding = PaddingValues(start = 20.dp, top = 16.dp, bottom = 8.dp, end = 8.dp)
                    ) {
                        Text(w.name, style = MaterialTheme.typography.titleMedium)
                        Text(
                            "${w.ids.size} أذكار",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            IconButton(onClick = { onEdit(w.id) }) {
                                Icon(Icons.Default.Edit, contentDescription = "تعديل", tint = p.primary)
                            }
                            IconButton(onClick = { toDelete = w }) {
                                Icon(Icons.Default.Delete, contentDescription = "حذف", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }
    }
}
