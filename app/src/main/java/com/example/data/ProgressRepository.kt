package com.example.data

import android.content.Context
import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ProgressRepository(private val progressDao: ProgressDao) {
    fun getTodayDateStr(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        return sdf.format(Date())
    }

    fun getTodayProgress(): Flow<UserProgress?> = progressDao.getProgressByDate(getTodayDateStr())
    fun getRecentProgress(): Flow<List<UserProgress>> = progressDao.getRecentProgress()

    suspend fun getTodayProgressSync(date: String = getTodayDateStr()): UserProgress? =
        progressDao.getProgressByDateSync(date)

    suspend fun getAllProgress(): List<UserProgress> = progressDao.getAllProgressSync()

    suspend fun initTodayProgress() {
        val date = getTodayDateStr()
        progressDao.insertProgressIfNotExists(UserProgress(date = date))
    }

    suspend fun completeSabah() {
        progressDao.updateSabah(getTodayDateStr(), true)
    }

    suspend fun completeMasaa() {
        progressDao.updateMasaa(getTodayDateStr(), true)
    }

    suspend fun addTasbeeh(count: Int) {
        progressDao.addTasbeeh(getTodayDateStr(), count)
    }

    suspend fun clearLocalProgress() {
        progressDao.clearAll()
    }

    suspend fun syncProgress(remoteList: List<UserProgress>) {
        for (remote in remoteList) {
            if (remote.date.isBlank()) continue
            val local = progressDao.getProgressByDateSync(remote.date)
            if (local == null) {
                progressDao.insertProgress(remote)
            } else {
                val merged = local.copy(
                    completedSabah = local.completedSabah || remote.completedSabah,
                    completedMasaa = local.completedMasaa || remote.completedMasaa,
                    totalTasbeeh = maxOf(local.totalTasbeeh, remote.totalTasbeeh)
                )
                progressDao.insertProgress(merged)
            }
        }
    }

    companion object {
        @Volatile
        private var instance: ProgressRepository? = null

        fun getInstance(context: Context): ProgressRepository {
            return instance ?: synchronized(this) {
                instance ?: ProgressRepository(
                    AppDatabase.getDatabase(context).progressDao()
                ).also { instance = it }
            }
        }
    }
}
