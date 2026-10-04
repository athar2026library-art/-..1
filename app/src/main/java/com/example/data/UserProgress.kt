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
    /** قيمة افتراضية مطلوبة لـ Firestore toObjects في release مع R8 */
    @PrimaryKey val date: String = "",
    val completedSabah: Boolean = false,
    val completedMasaa: Boolean = false,
    val totalTasbeeh: Int = 0
)
