package com.example.ui

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.MainActivity
import com.example.data.ProgressRepository
import com.example.data.SettingsRepository
import com.example.data.dataStore
import kotlinx.coroutines.flow.first
import java.util.Calendar

class NotificationWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val progressRepository = ProgressRepository.getInstance(context)
        val settings = SettingsRepository(context.dataStore)
        val todayProgress = progressRepository.getTodayProgress().first()
        val completedSabah = todayProgress?.completedSabah == true
        val completedMasaa = todayProgress?.completedMasaa == true
        val now = Calendar.getInstance()
        val hour = now.get(Calendar.HOUR_OF_DAY)
        val minute = now.get(Calendar.MINUTE)

        val sabahEnabled = settings.remindSabahFlow.first()
        val masaaEnabled = settings.remindMasaaFlow.first()
        val sleepEnabled = settings.remindSleepFlow.first()
        val fridayEnabled = settings.remindFridayFlow.first()
        val sabahTime = settings.reminderSabahTimeFlow.first()
        val masaaTime = settings.reminderMasaaTimeFlow.first()
        val sleepTime = settings.reminderSleepTimeFlow.first()

        var title: String? = null
        var message: String? = null
        var notificationId = 1

        when {
            sabahEnabled && !completedSabah && isWithinReminderWindow(hour, minute, sabahTime) -> {
                title = "🌤️ صباح الخير"
                message = "حان وقت أذكار الصباح. دقائق قليلة تبدأ بها يومك."
                notificationId = 1
            }
            masaaEnabled && !completedMasaa && isWithinReminderWindow(hour, minute, masaaTime) -> {
                title = "🌙 مساء الخير"
                message = "لا تنسَ أذكار المساء."
                notificationId = 2
            }
            sleepEnabled && isWithinReminderWindow(hour, minute, sleepTime) -> {
                title = "🌙 أذكار ما قبل النوم"
                message = "اختم يومك بذكر الله."
                notificationId = 3
            }
            fridayEnabled &&
                now.get(Calendar.DAY_OF_WEEK) == Calendar.FRIDAY &&
                isWithinReminderWindow(hour, minute, "09:00") -> {
                title = "🕌 تذكير الجمعة"
                message = "لا تنسَ الإكثار من الصلاة على النبي ﷺ يوم الجمعة."
                notificationId = 4
            }
        }

        if (title != null && message != null) showNotification(title, message, notificationId)
        return Result.success()
    }

    private fun isWithinReminderWindow(hour: Int, minute: Int, time: String): Boolean {
        val parts = time.split(":")
        if (parts.size != 2) return false
        val target = parts[0].toIntOrNull()?.times(60)?.plus(parts[1].toIntOrNull() ?: return false) ?: return false
        val current = hour * 60 + minute
        return kotlin.math.abs(current - target) <= 20
    }

    private fun showNotification(title: String, message: String, notificationId: Int) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "azkar_reminders"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel(channelId, "تذكيرات الأذكار", NotificationManager.IMPORTANCE_DEFAULT)
            )
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, notificationId, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
        manager.notify(notificationId, notification)
    }
}
