package com.example.data

import android.content.Context
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ProgressRepository(private val progressDao: ProgressDao) {
    fun getTodayDateStr(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        return sdf.format(Date())
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun getTodayProgress(): Flow<UserProgress?> =
        currentDateFlow().flatMapLatest { progressDao.getProgressByDate(it) }

    private fun currentDateFlow(): Flow<String> = flow {
        while (true) {
            emit(getTodayDateStr())
            delay(60_000L)
        }
    }.distinctUntilChanged()

    fun getRecentProgress(): Flow<List<UserProgress>> = progressDao.getRecentProgress()

    suspend fun getTodayProgressSync(date: String = getTodayDateStr()): UserProgress? =
        progressDao.getProgressByDateSync(date)

    fun getAllProgressFlow(): Flow<List<UserProgress>> = progressDao.getAllProgressFlow()

    suspend fun getAllProgress(): List<UserProgress> = progressDao.getAllProgressSync()

    /** يضمن وجود صف اليوم قبل أي تحديث (مهم بعد منتصف الليل والتطبيق مفتوح). */
    private suspend fun ensureToday(): String {
        val date = getTodayDateStr()
        progressDao.insertProgressIfNotExists(UserProgress(date = date))
        return date
    }

    suspend fun initTodayProgress() {
        progressDao.insertProgressIfNotExists(UserProgress(date = getTodayDateStr()))
    }

    suspend fun completeSabah() {
        progressDao.updateSabah(ensureToday(), true)
    }

    suspend fun completeMasaa() {
        progressDao.updateMasaa(ensureToday(), true)
    }

    suspend fun completeSleep() {
        progressDao.updateSleep(ensureToday(), true)
    }

    suspend fun addTasbeeh(count: Int) {
        progressDao.addTasbeeh(ensureToday(), count)
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
                    completedSleep = local.completedSleep || remote.completedSleep,
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
