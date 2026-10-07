package com.example

import com.example.ui.theme.TimeMode
import com.example.ui.theme.resolveTimeMode
import org.junit.Assert.assertEquals
import org.junit.Test

class TimeModeTest {
    @Test fun lightPreferenceIsAlwaysDay() {
        for (h in 0..23) assertEquals(TimeMode.DAY, resolveTimeMode(isDark = false, hour = h))
    }

    @Test fun darkPreferenceFollowsTheClock() {
        assertEquals(TimeMode.FAJR, resolveTimeMode(true, 3))
        assertEquals(TimeMode.FAJR, resolveTimeMode(true, 6))
        assertEquals(TimeMode.NIGHT, resolveTimeMode(true, 7))
        assertEquals(TimeMode.NIGHT, resolveTimeMode(true, 12))
        assertEquals(TimeMode.MAGHRIB, resolveTimeMode(true, 17))
        assertEquals(TimeMode.MAGHRIB, resolveTimeMode(true, 18))
        assertEquals(TimeMode.NIGHT, resolveTimeMode(true, 19))
        assertEquals(TimeMode.NIGHT, resolveTimeMode(true, 1))
    }
}
