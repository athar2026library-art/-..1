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
import kotlinx.coroutines.flow.first
import java.util.Calendar

class NotificationWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val progressRepository = ProgressRepository.getInstance(context)
        
        val todayProgress = progressRepository.getTodayProgress().first()
        val completedSabah = todayProgress?.completedSabah == true
        val completedMasaa = todayProgress?.completedMasaa == true
        
        val cal = Calendar.getInstance()
        val hour = cal.get(Calendar.HOUR_OF_DAY)
        
        var title: String? = null
        var message: String? = null
        
        // Smart conditions
        if (hour in 5..9 && !completedSabah) {
            title = "🌤️ صباح الخير"
            message = "حان وقت أذكار الصباح. دقائق قليلة تبدأ بها يومك."
        } else if (hour in 16..20 && !completedMasaa) {
            title = "🌙 مساء الخير"
            message = if (completedSabah) "أكملت أذكار الصباح، هل نكمل المساء؟" else "لا تنسَ أذكار المساء."
        }
        
        if (title != null && message != null) {
            showNotification(title, message)
        }
        
        return Result.success()
    }

    private fun showNotification(title: String, message: String) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "azkar_reminders"
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "تذكيرات الأذكار",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            manager.createNotificationChannel(channel)
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent, PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        manager.notify(1, notification)
    }
}
