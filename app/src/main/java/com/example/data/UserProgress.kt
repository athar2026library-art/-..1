package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_progress")
data class UserProgress(
    @PrimaryKey val date: String, // format yyyy-MM-dd
    val completedSabah: Boolean = false,
    val completedMasaa: Boolean = false,
    val totalTasbeeh: Int = 0
)
