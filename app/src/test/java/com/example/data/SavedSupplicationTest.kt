package com.example.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertNotNull
import org.junit.Test

class SavedSupplicationTest {
    @Test
    fun parsesOnlyTheDuaAndItsSource() {
        val reply = """
            الذكر: [اللَّهُمَّ إِنِّي أَعُوذُ بِكَ مِنَ الْهَمِّ وَالْحَزَنِ]
            فضله: دعاء للاستعاذة من الهم والحزن.
            المصدر: صحيح البخاري

            إجابة آلية، ليست فتوى.
        """.trimIndent()

        val parsed = AssistantSupplicationParser.parse(reply)
        assertNotNull(parsed)
        assertEquals("اللَّهُمَّ إِنِّي أَعُوذُ بِكَ مِنَ الْهَمِّ وَالْحَزَنِ", parsed!!.text)
        assertEquals("صحيح البخاري", parsed.source)
    }

    @Test
    fun ignoresAnswersWithoutAnExplicitSource() {
        assertNull(AssistantSupplicationParser.parse("الذكر: سبحان الله\nفضله: ذكر عظيم"))
    }

    @Test
    fun codecRoundTripsArabicTextAndSource() {
        val original = SavedSupplication("سبحان الله وبحمده", "صحيح مسلم")
        assertEquals(original, SavedSupplicationCodec.decode(SavedSupplicationCodec.encode(original)))
    }
}
