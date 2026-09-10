package com.example.data

import android.content.Context
import androidx.room.Room
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

    suspend fun initTodayProgress() {
        val date = getTodayDateStr()
        progressDao.insertProgress(UserProgress(date = date))
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

    companion object {
        @Volatile
        private var instance: ProgressRepository? = null

        fun getInstance(context: Context): ProgressRepository {
            return instance ?: synchronized(this) {
                instance ?: buildRepository(context).also { instance = it }
            }
        }

        private fun buildRepository(context: Context): ProgressRepository {
            val db = Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "azkar_db"
            ).build()
            return ProgressRepository(db.progressDao())
        }
    }
}
