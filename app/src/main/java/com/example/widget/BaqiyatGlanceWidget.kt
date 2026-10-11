package com.example.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.example.MainActivity
import com.example.data.ProgressRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Lightweight Glance widget – reads Room once and shows today's progress.
 * Designed for low battery / fast update (no continuous work).
 */
class BaqiyatGlanceWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val summary = withContext(Dispatchers.IO) {
            loadTodaySummary(context)
        }

        provideContent {
            GlanceTheme {
                WidgetContent(summary)
            }
        }
    }

    private suspend fun loadTodaySummary(context: Context): WidgetSummary {
        return try {
            val repo = ProgressRepository.getInstance(context)
            val date = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
            val progress = repo.getTodayProgressSync(date)
            WidgetSummary(
                sabahDone = progress?.completedSabah == true,
                masaaDone = progress?.completedMasaa == true,
                tasbeeh = progress?.totalTasbeeh ?: 0
            )
        } catch (_: Exception) {
            WidgetSummary()
        }
    }
}

data class WidgetSummary(
    val sabahDone: Boolean = false,
    val masaaDone: Boolean = false,
    val tasbeeh: Int = 0
)

@Composable
private fun WidgetContent(summary: WidgetSummary) {
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(GlanceTheme.colors.background)
            .padding(16.dp)
            .clickable(actionStartActivity<MainActivity>()),
        verticalAlignment = Alignment.CenterVertically,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "الباقيات",
            style = TextStyle(
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = GlanceTheme.colors.primary
            )
        )
        Spacer(GlanceModifier.height(12.dp))
        Row(modifier = GlanceModifier.fillMaxWidth()) {
            StatusChip("صباح", summary.sabahDone)
            Spacer(GlanceModifier.width(8.dp))
            StatusChip("مساء", summary.masaaDone)
        }
        Spacer(GlanceModifier.height(10.dp))
        Text(
            text = "التسبيح اليوم: ${summary.tasbeeh}",
            style = TextStyle(
                fontSize = 14.sp,
                color = GlanceTheme.colors.onSurface
            )
        )
        Spacer(GlanceModifier.height(6.dp))
        Text(
            text = "اضغط لفتح التطبيق",
            style = TextStyle(
                fontSize = 11.sp,
                color = GlanceTheme.colors.onSurfaceVariant
            )
        )
    }
}

@Composable
private fun StatusChip(label: String, done: Boolean) {
    Text(
        text = if (done) "$label ✓" else "$label ○",
        style = TextStyle(
            fontWeight = FontWeight.Medium,
            fontSize = 13.sp,
            color = if (done) GlanceTheme.colors.primary else GlanceTheme.colors.onSurfaceVariant
        )
    )
}

class BaqiyatGlanceWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = BaqiyatGlanceWidget()
}
