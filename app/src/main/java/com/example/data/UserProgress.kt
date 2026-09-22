package com.example.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "user_progress",
    indices = [
        Index(value = ["date"], unique = true),
        Index(value = ["completedSabah"]),
        Index(value = ["completedMasaa"])
    ]
)
data class UserProgress(
    @PrimaryKey val date: String, // format yyyy-MM-dd
    val completedSabah: Boolean = false,
    val completedMasaa: Boolean = false,
    val totalTasbeeh: Int = 0
)
