package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.UserProgress
import com.example.ui.AppViewModel
import com.example.ui.Badge
import com.example.ui.MAX_GRACE
import com.example.ui.ShareHelper
import com.example.ui.StreakInfo
import com.example.ui.components.BaqiyatButton
import com.example.ui.components.BaqiyatButtonStyle
import com.example.ui.components.BaqiyatChip
import com.example.ui.components.GlassCard
import com.example.ui.components.ProgressRing
import com.example.ui.computeBadges
import com.example.ui.computeStreak
import com.example.ui.dayDetails
import com.example.ui.heatLevel
import com.example.ui.heatmapDates
import com.example.ui.summaryText
import com.example.ui.theme.AccentTheme
import com.example.ui.theme.Baqiyat
import com.example.ui.theme.NightPalette
import com.example.ui.theme.withAccent
import com.example.ui.todayKey
import com.example.ui.unlockedAccents
import com.example.ui.weeklySummary

@Composable
private fun WeekBars(data: List<Pair<String, Int>>) {
    val maxValue = data.maxOfOrNull { it.second }?.coerceAtLeast(1) ?: 1
    Row(
        modifier = Modifier.fillMaxWidth().height(150.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        data.forEach { (label, value) ->
            Column(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height((96f * value / maxValue).coerceAtLeast(6f).dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Baqiyat.colors.accent)
                )
                Spacer(Modifier.height(6.dp))
                Text(label, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

/** «رحلتي»: السلسلة وأيام العفو، هدف الأسبوع، الخريطة الحرارية، الشارات، ثيمات اللون، وملخص قابل للمشاركة. */
@androidx.compose.material3.ExperimentalMaterial3Api
@Composable
fun JourneyScreen(viewModel: AppViewModel) {
    val all by viewModel.allProgress.collectAsStateWithLifecycle()
    val recent by viewModel.recentProgress.collectAsStateWithLifecycle()
    val weeklyGoal by viewModel.weeklyDaysGoal.collectAsStateWithLifecycle()
    val accent by viewModel.accentTheme.collectAsStateWithLifecycle()
    val today = remember { todayKey() }
    val info = remember(all) { computeStreak(all, today) }
    val badges = remember(all, info) { computeBadges(all, info) }
    val unlocked = remember(badges) { unlockedAccents(badges) }
    val summary = remember(all) { weeklySummary(all, today) }
    val week = remember(recent) { buildWeekChart(recent) }
    val context = LocalContext.current

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text("رحلتي", style = MaterialTheme.typography.titleLarge) },
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            StreakHero(info)
            WeeklyGoalCard(summary.fullDays, weeklyGoal) { viewModel.setWeeklyDaysGoal(it) }
            HeatmapCard(all, today)
            GlassCard(Modifier.fillMaxWidth()) {
                Text("التسبيح آخر 7 أيام", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(12.dp))
                WeekBars(week)
            }
            BadgesSection(badges)
            ThemesCard(accent, unlocked) { viewModel.setAccentTheme(it.key) }
            BaqiyatButton(
                text = "شارك ملخص أسبوعك",
                icon = Icons.Default.Share,
                style = BaqiyatButtonStyle.Glass,
                onClick = {
                    runCatching {
                        ShareHelper.shareZekrAsImage(context, summaryText(summary, info.current), "ملخصي الأسبوعي · الباقيات")
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(96.dp))
        }
    }
}

@Composable
private fun StreakHero(info: StreakInfo) {
    val p = Baqiyat.colors
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(Brush.linearGradient(listOf(p.buttonFrom, p.buttonTo)))
            .padding(24.dp)
    ) {
        Column {
            Text("سلسلة الالتزام", style = MaterialTheme.typography.bodySmall, color = p.onButton.copy(alpha = .8f))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    "${info.current}",
                    style = MaterialTheme.typography.displaySmall.copy(fontSize = 56.sp, lineHeight = 64.sp),
                    color = p.onButton
                )
                Spacer(Modifier.width(8.dp))
                Text("يوماً", style = MaterialTheme.typography.titleMedium, color = p.onButton, modifier = Modifier.padding(bottom = 10.dp))
            }
            Text("أطول سلسلة: ${info.longest} يوماً", style = MaterialTheme.typography.bodyMedium, color = p.onButton.copy(alpha = .85f))
            Spacer(Modifier.height(14.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                repeat(MAX_GRACE) { i ->
                    Icon(
                        if (i < info.graceTokens) Icons.Default.Star else Icons.Default.StarBorder,
                        contentDescription = null,
                        tint = p.onButton.copy(alpha = if (i < info.graceTokens) 1f else .5f),
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(Modifier.width(8.dp))
                Text("أيام العفو المتاحة: ${info.graceTokens}", style = MaterialTheme.typography.labelLarge, color = p.onButton)
            }
            Spacer(Modifier.height(4.dp))
            Text(
                "كل 7 أيام التزام تكسب يوم عفو يحفظ سلسلتك إن فاتك يوم.",
                style = MaterialTheme.typography.bodySmall,
                color = p.onButton.copy(alpha = .8f)
            )
        }
    }
}

@Composable
private fun WeeklyGoalCard(fullDays: Int, goal: Int, onGoal: (Int) -> Unit) {
    val p = Baqiyat.colors
    GlassCard(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            ProgressRing(progress = fullDays / goal.toFloat(), size = 72.dp, stroke = 6.dp) {
                Text("$fullDays/$goal", style = MaterialTheme.typography.labelLarge, color = p.primary)
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text("هدف الأسبوع", style = MaterialTheme.typography.titleMedium)
                Text(
                    "أيام أكملت فيها الصباح والمساء معاً (آخر 7 أيام).",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(3, 5, 7).forEach { g ->
                BaqiyatChip("$g أيام", selected = goal == g, onClick = { onGoal(g) })
            }
        }
    }
}

@Composable
private fun HeatmapCard(all: List<UserProgress>, today: String) {
    val p = Baqiyat.colors
    val byDate = remember(all) { all.associateBy { it.date } }
    val cells = remember(today) { heatmapDates(today, 12) }
    var selected by remember { mutableStateOf<String?>(null) }
    GlassCard(Modifier.fillMaxWidth()) {
        Text("خريطة الالتزام", style = MaterialTheme.typography.titleMedium)
        Text(
            "آخر 12 أسبوعاً. اضغط على يوم لعرض تفاصيله.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(14.dp))
        Row(
            Modifier
                .align(Alignment.CenterHorizontally)
                .semantics { contentDescription = "خريطة الالتزام لآخر 12 أسبوعاً" },
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            for (c in 0 until 12) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    for (r in 0 until 7) {
                        val key = cells[c * 7 + r]
                        val level = if (key == null) -1 else heatLevel(byDate[key])
                        val color = when (level) {
                            -1 -> Color.Transparent
                            0 -> p.line
                            1 -> p.accent.copy(alpha = .35f)
                            2 -> p.accent.copy(alpha = .65f)
                            else -> p.accent
                        }
                        Box(
                            Modifier
                                .size(18.dp)
                                .clip(RoundedCornerShape(5.dp))
                                .background(color)
                                .then(
                                    if (key != null) Modifier
                                        .border(
                                            if (key == selected) 1.5.dp else 0.dp,
                                            p.ink,
                                            RoundedCornerShape(5.dp)
                                        )
                                        .clickable(role = Role.Button, onClickLabel = "تفاصيل $key") { selected = key }
                                    else Modifier
                                )
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(Modifier.align(Alignment.CenterHorizontally), verticalAlignment = Alignment.CenterVertically) {
            Text("أقل", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.width(6.dp))
            listOf(p.line, p.accent.copy(alpha = .35f), p.accent.copy(alpha = .65f), p.accent).forEach {
                Box(Modifier.padding(horizontal = 2.dp).size(12.dp).clip(RoundedCornerShape(3.dp)).background(it))
            }
            Spacer(Modifier.width(6.dp))
            Text("أكثر", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        selected?.let {
            Spacer(Modifier.height(10.dp))
            Text(dayDetails(it, byDate[it]), style = MaterialTheme.typography.bodySmall, color = p.accent)
        }
    }
}

@Composable
private fun BadgesSection(badges: List<Badge>) {
    Column {
        Text(
            "الشارات (${badges.count { it.unlocked }}/${badges.size})",
            style = MaterialTheme.typography.titleLarge
        )
        Spacer(Modifier.height(12.dp))
        badges.chunked(2).forEach { pair ->
            Row(Modifier.fillMaxWidth().padding(bottom = 12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                pair.forEach { BadgeCard(it, Modifier.weight(1f)) }
                if (pair.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun BadgeCard(badge: Badge, modifier: Modifier) {
    val p = Baqiyat.colors
    GlassCard(
        modifier = modifier
            .graphicsLayer { alpha = if (badge.unlocked) 1f else 0.7f }
            .semantics(mergeDescendants = true) {},
        shape = RoundedCornerShape(24.dp),
        contentPadding = PaddingValues(14.dp)
    ) {
        Icon(
            if (badge.unlocked) Icons.Default.EmojiEvents else Icons.Default.Lock,
            contentDescription = if (badge.unlocked) "مفتوحة" else "مقفلة",
            tint = if (badge.unlocked) p.accent else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(28.dp)
        )
        Spacer(Modifier.height(8.dp))
        Text(badge.title, style = MaterialTheme.typography.titleMedium)
        Text(badge.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(10.dp))
        if (badge.unlocked) {
            Text("مفتوحة ✓", style = MaterialTheme.typography.labelMedium, color = p.accent)
        } else {
            LinearProgressIndicator(
                progress = { badge.progress },
                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                color = p.accent,
                trackColor = p.line
            )
        }
    }
}

@Composable
private fun ThemesCard(current: AccentTheme, unlocked: Set<AccentTheme>, onSelect: (AccentTheme) -> Unit) {
    val p = Baqiyat.colors
    var hint by remember { mutableStateOf<String?>(null) }
    GlassCard(Modifier.fillMaxWidth()) {
        Text("ثيم اللون", style = MaterialTheme.typography.titleMedium)
        Text(
            "افتح ثيمات جديدة بجمع الشارات.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(14.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            AccentTheme.entries.forEach { theme ->
                val open = theme in unlocked
                val preview = NightPalette.withAccent(theme)
                Column(
                    Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .clickable(role = Role.RadioButton, onClickLabel = theme.label) {
                            if (open) { onSelect(theme); hint = null } else hint = theme.unlockHint
                        }
                        .padding(6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(Brush.linearGradient(listOf(preview.buttonFrom, preview.buttonTo)))
                            .border(if (theme == current) 3.dp else 1.dp, if (theme == current) p.ink else p.line, CircleShape)
                            .graphicsLayer { alpha = if (open) 1f else 0.45f },
                        contentAlignment = Alignment.Center
                    ) {
                        if (!open) Icon(Icons.Default.Lock, null, tint = NightPalette.onButton, modifier = Modifier.size(22.dp))
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(theme.label, style = MaterialTheme.typography.labelMedium)
                }
            }
        }
        hint?.let {
            Spacer(Modifier.height(10.dp))
            Text(it, style = MaterialTheme.typography.bodySmall, color = p.accent)
        }
    }
}
