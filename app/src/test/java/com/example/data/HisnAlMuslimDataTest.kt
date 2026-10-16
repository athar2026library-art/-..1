package com.example.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HisnAlMuslimDataTest {
    @Test
    fun importsAllStructuredBookChaptersAndDhikr() {
        assertEquals(133, HisnAlMuslimData.chapters.size)
        assertEquals(302, HisnAlMuslimData.items.size)
        assertEquals(HisnAlMuslimData.chapters.size, HisnAlMuslimData.chapterIds.size)
    }

    @Test
    fun everyChapterHasItemsAndEveryDhikrKeepsItsReferenceField() {
        val allIds = HisnAlMuslimData.items.map { it.id }
        assertEquals(allIds.size, allIds.toSet().size)
        HisnAlMuslimData.chapters.forEach { chapter ->
            assertTrue(chapter.title.isNotBlank())
            assertTrue(chapter.items.isNotEmpty())
            assertTrue(chapter.audioUrl?.startsWith("https://") == true)
            chapter.items.forEach { zekr ->
                assertEquals(chapter.id, zekr.category)
                assertTrue(zekr.repeatCount >= 1)
                assertFalse(zekr.text.isBlank())
                assertFalse("Missing reference for dhikr ${zekr.id}", zekr.source.isBlank())
            }
        }
    }

    @Test
    fun chapterNavigationUsesBuiltInOfflineData() {
        val first = HisnAlMuslimData.chapters.first()
        assertTrue(CategoryDefaults.isBuiltIn(first.id))
        assertEquals(first.items, AzkarData.builtInOrEmpty(first.id))
        assertEquals(first.title, AzkarData.titleFor(first.id))
        assertFalse(CategoryDefaults.builtIn.any { it.id == "hisn" })
        assertTrue(CategoryDefaults.isBuiltIn("hisn")) // legacy route remains reserved
        assertTrue(CategoryDefaults.merge(listOf(AzkarCategory(first.id, first.title, "", "book"))).none { it.id == first.id })
    }
    @Test
    fun bookMorningEveningAndSleepDhikrAreMergedIntoExistingCategories() {
        val morning = AzkarData.forCategory("sabah")
        val evening = AzkarData.forCategory("masaa")
        val sleep = AzkarData.forCategory("sleep")

        assertTrue(morning.any { it.id >= 300000 && it.category == "hisn_027" })
        assertTrue(evening.any { it.id >= 300000 && it.category == "hisn_028" })
        assertTrue(sleep.any { it.id >= 300000 && it.category in setOf("hisn_029", "hisn_030", "hisn_031", "hisn_032") })
        assertTrue(CategoryDefaults.builtIn.none { it.id == "hisn" })
    }

}
