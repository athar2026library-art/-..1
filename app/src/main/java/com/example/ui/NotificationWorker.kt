package com.example.ui

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.MainActivity
import com.example.R
import com.example.data.SettingsRepository
import com.example.data.dataStore
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/** Local reminders driven by per-category settings and deduplicated for each date. */
class NotificationWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val settings = SettingsRepository(applicationContext.dataStore)
        if (!settings.notificationsEnabledFlow.first()) return Result.success()
        // Don't claim a reminder day until Android notifications are enabled.
        if (!NotificationManagerCompat.from(applicationContext).areNotificationsEnabled()) {
            return Result.success()
        }

        val now = Calendar.getInstance()
        val currentMinutes = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        val isFriday = now.get(Calendar.DAY_OF_WEEK) == Calendar.FRIDAY

        val reminders = listOf(
            Reminder(
                category = "sabah",
                enabled = settings.remindSabahFlow.first(),
                time = settings.reminderSabahTimeFlow.first(),
                title = "🌤️ أذكار الصباح",
                body = "حان وقت أذكار الصباح. ابدأ يومك بذكر الله.",
                notificationId = 1001
            ),
            Reminder(
                category = "masaa",
                enabled = settings.remindMasaaFlow.first(),
                time = settings.reminderMasaaTimeFlow.first(),
                title = "🌙 أذكار المساء",
                body = "حان وقت أذكار المساء. اختم يومك بذكر الله.",
                notificationId = 1002
            ),
            Reminder(
                category = "sleep",
                enabled = settings.remindSleepFlow.first(),
                time = settings.reminderSleepTimeFlow.first(),
                title = "🌙 أذكار ما قبل النوم",
                body = "اختم يومك بذكر الله قبل النوم.",
                notificationId = 1003
            ),
            Reminder(
                category = "friday",
                enabled = isFriday && settings.remindFridayFlow.first(),
                time = settings.reminderFridayTimeFlow.first(),
                title = "🕌 تذكير الجمعة",
                body = "تذكّر الإكثار من الصلاة على النبي ﷺ يوم الجمعة.",
                notificationId = 1004
            )
        )

        for (reminder in reminders) {
            if (!reminder.enabled || !isWithinDeliveryWindow(currentMinutes, reminder.time)) continue
            if (settings.claimReminderForDate(reminder.category, today)) {
                showNotification(reminder.title, reminder.body, reminder.notificationId)
            }
        }
        return Result.success()
    }

    private fun isWithinDeliveryWindow(currentMinutes: Int, time: String): Boolean {
        val pieces = time.split(":")
        if (pieces.size != 2) return false
        val hour = pieces[0].toIntOrNull() ?: return false
        val minute = pieces[1].toIntOrNull() ?: return false
        if (hour !in 0..23 || minute !in 0..59) return false
        val targetMinutes = hour * 60 + minute

        // WorkManager runs inexactly; accept the first run within 60 minutes after the chosen time.
        val elapsedMinutes = currentMinutes - targetMinutes
        return elapsedMinutes in 0 until DELIVERY_WINDOW_MINUTES
    }

    private fun showNotification(title: String, body: String, notificationId: Int) {
        ensureChannel()
        val intent = PendingIntent.getActivity(
            applicationContext,
            notificationId,
            Intent(applicationContext, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_azkar)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(intent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        try {
            NotificationManagerCompat.from(applicationContext).notify(notificationId, notification)
        } catch (_: SecurityException) {
            // Android 13+ notifications are optional until POST_NOTIFICATIONS is granted.
        }
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            "تذكيرات الأذكار",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "تذكيرات قابلة للتخصيص للصباح والمساء والنوم والجمعة"
        }
        applicationContext.getSystemService(NotificationManager::class.java)
            .createNotificationChannel(channel)
    }

    private data class Reminder(
        val category: String,
        val enabled: Boolean,
        val time: String,
        val title: String,
        val body: String,
        val notificationId: Int
    )

    private companion object {
        const val CHANNEL_ID = "azkar_reminders"
        const val DELIVERY_WINDOW_MINUTES = 60
    }
}
