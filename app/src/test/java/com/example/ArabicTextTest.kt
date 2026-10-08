package com.example

import com.example.data.ArabicText
import com.example.data.AzkarData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ArabicTextTest {
    private val sample = "اللَّهُمَّ أَنْتَ رَبِّي لَا إِلَهَ إِلَّا أَنْتَ"

    @Test fun normalizeStripsMarksAndFoldsLetters() {
        assertEquals("اللهم", ArabicText.normalize("اللَّهُمَّ"))
        assertEquals("اعوذ", ArabicText.normalize("أَعُوذُ"))
        assertEquals("رحمه", ArabicText.normalize("رحمة"))
    }

    @Test fun matchesIgnoreTashkeelAndCoverMarks() {
        val m = ArabicText.matches(sample, "الله")
        assertEquals(1, m.size)
        assertEquals("اللَّهُ", sample.substring(m[0].first, m[0].last + 1))
        assertEquals(2, ArabicText.matches(sample, "انت").size)
        assertTrue(ArabicText.matches(sample, "   ").isEmpty())
    }

    @Test fun searchFindsDhikrWithoutTypingTashkeel() {
        assertTrue(AzkarData.search("اللهم").isNotEmpty())
        assertEquals(AzkarData.search("اللهم").map { it.id }, AzkarData.search("اللَّهُمَّ").map { it.id })
    }
}
