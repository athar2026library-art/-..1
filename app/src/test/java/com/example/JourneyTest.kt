package com.example

import com.example.data.UserProgress
import com.example.ui.StreakInfo
import com.example.ui.computeBadges
import com.example.ui.computeStreak
import com.example.ui.heatLevel
import com.example.ui.heatmapDates
import com.example.ui.shiftKey
import com.example.ui.unlockedAccents
import com.example.ui.weeklySummary
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Calendar

class JourneyTest {
    private val today = "2026-10-08"

    private fun d(
        daysAgo: Int, sabah: Boolean = true, masaa: Boolean = false, sleep: Boolean = false, tasbeeh: Int = 0
    ) = UserProgress(shiftKey(today, -daysAgo), sabah, masaa, tasbeeh, sleep)

    @Test fun emptyHistory() {
        assertEquals(StreakInfo(0, 0, 0, 0), computeStreak(emptyList(), today))
    }

    @Test fun tenDaysEarnOneGraceDay() {
        val info = computeStreak((0..9).map { d(it) }, today)
        assertEquals(10, info.current); assertEquals(10, info.longest); assertEquals(1, info.graceTokens)
    }

    @Test fun graceDayProtectsOneMissedDay() {
        val days = (listOf(9, 8, 7, 6, 5, 4, 3) + listOf(1, 0)).map { d(it) }
        val info = computeStreak(days, today)
        assertEquals(9, info.current); assertEquals(1, info.graceUsed); assertEquals(0, info.graceTokens)
    }

    @Test fun missWithoutGraceBreaksStreak() {
        assertEquals(2, computeStreak((listOf(5, 4, 3) + listOf(1, 0)).map { d(it) }, today).current)
    }

    @Test fun unfinishedTodayDoesNotBreak() {
        assertEquals(4, computeStreak((1..4).map { d(it) }, today).current)
    }

    @Test fun oneTokenCoversOnlyOneMiss() {
        assertEquals(3, computeStreak((listOf(12, 11, 10, 9, 8, 7, 6) + listOf(2, 1, 0)).map { d(it) }, today).current)
    }

    @Test fun badgesAndAccents() {
        val days = (0..9).map { d(it) }
        val badges = computeBadges(days, computeStreak(days, today))
        assertTrue(badges.first { it.id == "week" }.unlocked)
        assertFalse(badges.first { it.id == "month" }.unlocked)
        assertEquals(setOf("gold", "rose"), unlockedAccents(badges).map { it.key }.toSet())
    }

    @Test fun heatLevels() {
        assertEquals(0, heatLevel(null))
        assertEquals(1, heatLevel(UserProgress("x", totalTasbeeh = 5)))
        assertEquals(3, heatLevel(UserProgress("x", true, true, 0, true)))
    }

    @Test fun heatmapEndsToday_andStartsSaturday() {
        val cells = heatmapDates(today, 12)
        assertEquals(84, cells.size)
        assertEquals(today, cells.last { it != null })
        val cal = Calendar.getInstance().apply { time = SimpleDateFormat("yyyy-MM-dd").parse(cells[0]!!)!! }
        assertEquals(Calendar.SATURDAY, cal.get(Calendar.DAY_OF_WEEK))
    }

    @Test fun weeklySummaryCounts() {
        val s = weeklySummary(listOf(d(0, masaa = true, sleep = true, tasbeeh = 100), d(1, tasbeeh = 50), d(9)), today)
        assertEquals(2, s.activeDays); assertEquals(150, s.tasbeeh); assertEquals(4, s.wirds)
        assertEquals(1, s.perfectDays); assertEquals(1, s.fullDays)
    }
}
