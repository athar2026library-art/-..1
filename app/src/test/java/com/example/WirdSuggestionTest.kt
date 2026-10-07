package com.example

import com.example.ui.WirdSlot
import com.example.ui.suggestWird
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WirdSuggestionTest {
    @Test fun morningSuggestsSabah() {
        val s = suggestWird(hour = 8, sabahDone = false, masaaDone = false)
        assertEquals(WirdSlot.SABAH, s.slot); assertEquals("sabah", s.category); assertTrue(!s.done)
        assertEquals(WirdSlot.SABAH, suggestWird(3, false, false).slot)
        assertEquals(WirdSlot.SABAH, suggestWird(11, false, false).slot)
    }

    @Test fun doneMorningIsMarkedDone() {
        assertTrue(suggestWird(8, sabahDone = true, masaaDone = false).done)
    }

    @Test fun middayFallsBackToTasbihWhenMorningDone() {
        val s = suggestWird(13, sabahDone = true, masaaDone = false)
        assertEquals(WirdSlot.FREE, s.slot); assertNull(s.category)
    }

    @Test fun middayStillOffersMissedMorning() {
        assertEquals("sabah", suggestWird(13, sabahDone = false, masaaDone = false).category)
    }

    @Test fun afternoonSuggestsMasaa() {
        assertEquals("masaa", suggestWird(16, false, false).category)
        assertTrue(suggestWird(20, false, masaaDone = true).done)
    }

    @Test fun nightSuggestsSleep() {
        for (h in listOf(21, 23, 0, 2)) assertEquals("sleep", suggestWird(h, true, true).category)
    }
}
