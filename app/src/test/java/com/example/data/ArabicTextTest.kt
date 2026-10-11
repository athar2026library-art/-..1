package com.example.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ArabicTextTest {
    @Test
    fun foldsDiacriticsAndAlefVariants() {
        assertEquals("اللهم", ArabicText.normalize("اللَّهُمَّ"))
        assertEquals("الى", ArabicText.normalize("إِلَىٰ"))
    }

    @Test
    fun collapsesWhitespaceAndLineBreaks() {
        assertEquals(
            "اللهم اغفر لي",
            ArabicText.normalize("  اللَّهُمَّ   اغْفِرْ\n\tلِي  ")
        )
    }

    @Test
    fun matchRangesStillPointToOriginalText() {
        val text = "اللَّهُمَّ    اغْفِرْ لِي"
        val match = ArabicText.matches(text, "اللهم اغفر").single()
        assertTrue(text.substring(match.first, match.last + 1).startsWith("اللَّهُمَّ"))
        assertTrue(text.substring(match.first, match.last + 1).contains("اغْفِر"))
    }
}
