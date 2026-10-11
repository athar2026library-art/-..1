package com.example.data

import org.junit.Assert.assertEquals
import org.junit.Test

class ArabicSearchNormalizerTest {
    @Test
    fun removesDiacriticsAndTatweel() {
        assertEquals(
            "اللهم اغفر لي",
            ArabicSearchNormalizer.normalize("اللَّهُمَّ اغْفِرْ لِي")
        )
    }

    @Test
    fun foldsAlefVariantsAndAlefMaqsura() {
        assertEquals(
            "الي ايات اخري",
            ArabicSearchNormalizer.normalize("إِلَىٰ آيات أُخْرَى")
        )
    }

    @Test
    fun collapsesRepeatedWhitespace() {
        assertEquals(
            "سبحان الله",
            ArabicSearchNormalizer.normalize("  سُبْحَانَ   اللَّهِ  ")
        )
    }
}
