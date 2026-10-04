package com.example

import com.example.data.UserProgress
import com.example.ui.screens.calculateStreak
import org.junit.Assert.assertEquals
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class StreakCalculationTest {
    private val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    private fun day(daysAgo: Int, done: Boolean = true): UserProgress {
        val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -daysAgo) }
        return UserProgress(date = sdf.format(cal.time), completedSabah = done)
    }

    @Test
    fun empty_returnsZero() {
        assertEquals(0, calculateStreak(emptyList()))
    }

    @Test
    fun consecutiveDays_areAllCounted() {
        assertEquals(3, calculateStreak(listOf(day(0), day(1), day(2))))
    }

    @Test
    fun gap_stopsTheStreak() {
        assertEquals(2, calculateStreak(listOf(day(0), day(1), day(3))))
    }

    @Test
    fun streakMayStartYesterday() {
        assertEquals(2, calculateStreak(listOf(day(1), day(2))))
    }

    @Test
    fun staleLatestDay_returnsZero() {
        assertEquals(0, calculateStreak(listOf(day(3))))
    }

    @Test
    fun emptyDay_breaksTheStreak() {
        assertEquals(1, calculateStreak(listOf(day(0), day(1, done = false), day(2))))
    }
}
