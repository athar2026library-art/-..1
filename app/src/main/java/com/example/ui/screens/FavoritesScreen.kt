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
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.AzkarData
import com.example.ui.AppViewModel
import com.example.ui.ShareHelper
import com.example.ui.components.BaqiyatBackButton
import com.example.ui.components.BaqiyatButton
import com.example.ui.components.GlassCard
import com.example.ui.theme.Baqiyat
import com.example.ui.theme.ZekrTextStyle

@Composable
fun FavoritesScreen(
    viewModel: AppViewModel,
    onNavigateBack: () -> Unit,
    onRead: () -> Unit
) {
    val ids by viewModel.favorites.collectAsStateWithLifecycle()
    val items = remember(ids) { AzkarData.byIds(ids) }
    val context = LocalContext.current
    val p = Baqiyat.colors

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text("المفضلة", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = { BaqiyatBackButton(onNavigateBack) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        if (items.isEmpty()) {
            Column(
                Modifier.fillMaxSize().padding(padding).padding(32.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(Icons.Default.Favorite, null, Modifier.size(56.dp), tint = p.accent.copy(alpha = .5f))
                Spacer(Modifier.height(16.dp))
                Text("لم تحفظ شيئاً بعد", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(6.dp))
                Text(
                    "اضغط على القلب أثناء القراءة أو من نتائج البحث لتجمع أذكارك المحببة هنا.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp),
                contentPadding = PaddingValues(top = 8.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    BaqiyatButton(
                        text = "اقرأ المفضلة (${items.size})",
                        onClick = onRead,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                items(items, key = { it.id }) { z ->
                    GlassCard(Modifier.fillMaxWidth(), contentPadding = PaddingValues(18.dp)) {
                        Text(AzkarData.titleFor(z.category), style = MaterialTheme.typography.labelMedium, color = p.accent)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            z.text,
                            style = ZekrTextStyle.copy(fontSize = 20.sp, lineHeight = 38.sp),
                            maxLines = 4,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.height(6.dp))
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                if (z.repeatCount > 1) "يُقال ${z.repeatCount} مرات" else z.source,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = { runCatching { ShareHelper.shareZekrAsImage(context, z.text, z.source) } }) {
                                Icon(Icons.Default.Share, contentDescription = "مشاركة", tint = p.primary)
                            }
                            IconButton(onClick = { viewModel.toggleFavorite(z.id) }) {
                                Icon(Icons.Default.Favorite, contentDescription = "إزالة من المفضلة", tint = p.accent)
                            }
                        }
                    }
                }
            }
        }
    }
}
