package com.example.ui

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.data.FirestoreRepository
import com.example.data.ProgressRepository
import com.google.firebase.auth.FirebaseAuth
import java.util.concurrent.TimeUnit

/** Offline-first background backup: merge remote then upload local. */
class SyncWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        if (FirebaseAuth.getInstance().currentUser == null) {
            return Result.success()
        }
        return try {
            val progress = ProgressRepository.getInstance(applicationContext)
            val cloud = FirestoreRepository()
            val remote = cloud.fetchProgress()
            if (remote == null) {
                // فشل الشبكة/القواعد — أعد المحاولة بدل اعتبارها نجاحاً
                return if (runAttemptCount < 3) Result.retry() else Result.failure()
            }
            progress.syncProgress(remote)
            val local = progress.getAllProgress()
            val ok = cloud.backupProgress(local)
            if (ok) Result.success()
            else if (runAttemptCount < 3) Result.retry()
            else Result.failure()
        } catch (_: Exception) {
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }

    companion object {
        fun enqueue(context: Context) {
            val req = PeriodicWorkRequestBuilder<SyncWorker>(6, TimeUnit.HOURS).build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "BaqiyatAutoSync",
                ExistingPeriodicWorkPolicy.KEEP,
                req
            )
        }
    }
}
