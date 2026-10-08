package com.example.ui.screens

import com.example.data.UserProgress
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/** دوال إحصاء بسيطة. الشاشة نفسها صارت JourneyScreen، والسلسلة الأذكى في Journey.kt. */
internal fun buildWeekChart(progress: List<UserProgress>): List<Pair<String, Int>> {
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    val dayLabels = listOf("أحد", "إثنين", "ثلاثاء", "أربعاء", "خميس", "جمعة", "سبت")
    val weekData = mutableListOf<Pair<String, Int>>()
    for (i in 6 downTo 0) {
        val calDay = Calendar.getInstance()
        calDay.add(Calendar.DAY_OF_YEAR, -i)
        val dateStr = sdf.format(calDay.time)
        val tasbeeh = progress.find { it.date == dateStr }?.totalTasbeeh ?: 0
        val label = dayLabels[calDay.get(Calendar.DAY_OF_WEEK) - 1]
        weekData.add(Pair(label, tasbeeh))
    }
    return weekData
}

fun calculateStreak(progress: List<UserProgress>): Int {
    if (progress.isEmpty()) return 0
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    val cal = Calendar.getInstance()
    var streak = 0
    val sorted = progress.sortedByDescending { it.date }
    val todayStr = sdf.format(cal.time)
    cal.add(Calendar.DAY_OF_YEAR, -1)
    val yesterdayStr = sdf.format(cal.time)
    val latest = sorted.first()
    if (latest.date != todayStr && latest.date != yesterdayStr) return 0
    var expectedDateStr = latest.date
    val expectedCal = Calendar.getInstance()
    expectedCal.time = sdf.parse(expectedDateStr) ?: Date()
    for (p in sorted) {
        if (p.date == expectedDateStr && (p.completedSabah || p.completedMasaa || p.totalTasbeeh > 0)) {
            streak++
            expectedCal.add(Calendar.DAY_OF_YEAR, -1)
            expectedDateStr = sdf.format(expectedCal.time)
        } else break
    }
    return streak
}

fun calculateLongestStreak(progress: List<UserProgress>): Int {
    if (progress.isEmpty()) return 0
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    val sorted = progress.sortedBy { it.date }
    var maxStreak = 0
    var current = 0
    var prevDate: Calendar? = null
    for (p in sorted) {
        val active = p.completedSabah || p.completedMasaa || p.totalTasbeeh > 0
        if (!active) {
            current = 0
            prevDate = null
            continue
        }
        val cal = Calendar.getInstance()
        cal.time = sdf.parse(p.date) ?: continue
        if (prevDate == null) current = 1
        else {
            val expected = prevDate.clone() as Calendar
            expected.add(Calendar.DAY_OF_YEAR, 1)
            current = if (cal.get(Calendar.YEAR) == expected.get(Calendar.YEAR) &&
                cal.get(Calendar.DAY_OF_YEAR) == expected.get(Calendar.DAY_OF_YEAR)
            ) current + 1 else 1
        }
        if (current > maxStreak) maxStreak = current
        prevDate = cal
    }
    return maxStreak
}
