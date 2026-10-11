package com.example.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AzkarDataSearchTest {
    @Test
    fun findsDiacritizedTextWithoutDiacritics() {
        val results = AzkarData.search("اللهم إني أسألك علما نافعا")
        assertTrue(results.any { it.category == "rizq" && it.source.contains("ابن ماجه") })
    }

    @Test
    fun mapsFeelingKeywordsToTrustedOfflineAzkar() {
        assertTrue(AzkarData.search("قلق وخوف").any { it.category == "worry" })
        assertTrue(AzkarData.search("الشفاء من المرض").any { it.category == "illness" })
        assertTrue(AzkarData.search("رزق وبركة").any { it.category == "rizq" })
    }

    @Test
    fun mapsTravelAndSleepAliases() {
        assertTrue(AzkarData.search("السفر بالسيارة").any { it.category == "travel" })
        assertTrue(AzkarData.search("قبل النوم").any { it.category == "sleep" })
    }

    @Test
    fun topicOnlyCategoriesAreReservedFromDynamicContent() {
        assertTrue(CategoryDefaults.isBuiltIn("worry"))
        assertTrue(CategoryDefaults.isBuiltIn("rizq"))
        assertFalse(CategoryDefaults.isDynamicId("illness"))
    }
}
