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
import java.util.Calendar

/** تذكيرات صباح/مساء محلية — لا تعتمد على FCM. */
class NotificationWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val settings = SettingsRepository(applicationContext.dataStore)
        if (!settings.notificationsEnabledFlow.first()) return Result.success()

        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val (title, body) = when (hour) {
            in 4..10 -> "أذكار الصباح" to "حان وقت أذكار الصباح. ابدأ يومك بذكر الله."
            in 15..21 -> "أذكار المساء" to "حان وقت أذكار المساء. اختم يومك بذكر الله."
            else -> return Result.success()
        }

        ensureChannel()
        val intent = PendingIntent.getActivity(
            applicationContext,
            0,
            Intent(applicationContext, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_azkar)
            .setContentTitle(title)
            .setContentText(body)
            .setContentIntent(intent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        try {
            NotificationManagerCompat.from(applicationContext)
                .notify(NOTIFICATION_ID + hour, notification)
        } catch (_: SecurityException) {
            // إذن الإشعارات غير ممنوح
        }
        return Result.success()
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            "تذكيرات الأذكار",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply { description = "تذكير بأذكار الصباح والمساء" }
        applicationContext.getSystemService(NotificationManager::class.java)
            .createNotificationChannel(channel)
    }

    companion object {
        private const val CHANNEL_ID = "azkar_reminders"
        private const val NOTIFICATION_ID = 1000
    }
}
