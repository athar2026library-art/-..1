package com.example.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EverydayAzkarDataTest {
    @Test
    fun everyAddedDhikrHasSourceAndValidRepeatCount() {
        assertTrue(EverydayAzkarData.items.isNotEmpty())
        EverydayAzkarData.items.forEach { zekr ->
            assertFalse("Missing source for dhikr ${zekr.id}", zekr.source.isBlank())
            assertTrue("Invalid repeat count for dhikr ${zekr.id}", zekr.repeatCount >= 1)
            assertTrue("No built-in category for ${zekr.id}", CategoryDefaults.isBuiltIn(zekr.category))
        }
    }

    @Test
    fun addedDhikrIdsAreUniqueAndReachableOffline() {
        val addedIds = EverydayAzkarData.items.map { it.id }
        assertEquals(addedIds.size, addedIds.toSet().size)

        EverydayAzkarData.items.map { it.category }.distinct().forEach { category ->
            assertTrue(
                "Category $category should expose its offline dhikr",
                AzkarData.builtInOrEmpty(category).isNotEmpty()
            )
        }
    }

    @Test
    fun searchFindsNewOccasionsOffline() {
        assertTrue(AzkarData.search("الوضوء").any { it.category == "wudu" })
        assertTrue(AzkarData.search("المطر").any { it.category == "weather" })
        assertTrue(AzkarData.search("بعد الصلاة").any { it.category == "prayer" })
    }
}
