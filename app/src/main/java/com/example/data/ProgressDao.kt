package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ProgressDao {
    @Query("SELECT * FROM user_progress WHERE date = :date LIMIT 1")
    fun getProgressByDate(date: String): Flow<UserProgress?>

    @Query("SELECT * FROM user_progress WHERE date = :date LIMIT 1")
    suspend fun getProgressByDateSync(date: String): UserProgress?

    @Query("SELECT * FROM user_progress ORDER BY date DESC LIMIT 30")
    fun getRecentProgress(): Flow<List<UserProgress>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProgress(progress: UserProgress)

    /** Insert only if the day row does not exist — prevents wiping today's progress. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertProgressIfNotExists(progress: UserProgress)

    @Query("UPDATE user_progress SET completedSabah = :completed WHERE date = :date")
    suspend fun updateSabah(date: String, completed: Boolean)

    @Query("UPDATE user_progress SET completedMasaa = :completed WHERE date = :date")
    suspend fun updateMasaa(date: String, completed: Boolean)

    @Query("UPDATE user_progress SET totalTasbeeh = totalTasbeeh + :count WHERE date = :date")
    suspend fun addTasbeeh(date: String, count: Int)
}
