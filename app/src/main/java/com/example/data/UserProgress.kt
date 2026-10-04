package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * قيمة افتراضية لـ date مطلوبة لـ Firestore toObjects (constructor بلا معاملات).
 */
@Entity(tableName = "user_progress")
data class UserProgress(
    @PrimaryKey val date: String = "",
    val completedSabah: Boolean = false,
    val completedMasaa: Boolean = false,
    val totalTasbeeh: Int = 0
)
