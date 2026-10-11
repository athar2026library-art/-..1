package com.example.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TopicAzkarSearchTest {
    @Test
    fun searchesCommonIntentWordsOffline() {
        val cases = listOf(
            "الصباح" to "sabah",
            "evening" to "masaa",
            "sleep" to "sleep",
            "travel" to "travel",
            "الخوف" to "worry",
            "القلق" to "worry",
            "الرزق" to "rizq",
            "مرض" to "illness",
            "الاستيقاظ" to "waking"
        )

        cases.forEach { (query, category) ->
            assertTrue(
                "Expected '$query' to return category '$category'",
                AzkarData.search(query).any { it.category == category }
            )
        }
    }

    @Test
    fun topicResultsKeepSourceAndRepeatCount() {
        listOf("الخوف", "الرزق", "المرض", "الاستيقاظ").forEach { query ->
            val result = AzkarData.search(query).first { it.category in setOf("worry", "rizq", "illness", "waking") }
            assertFalse("Missing source for '$query'", result.source.isBlank())
            assertTrue("Invalid repeat count for '$query'", result.repeatCount >= 1)
        }
    }

    @Test
    fun topicCategoriesOpenTheirOfflineContent() {
        listOf("worry", "rizq", "illness", "waking").forEach { category ->
            assertTrue(
                "Expected built-in content for '$category'",
                AzkarData.builtInOrEmpty(category).isNotEmpty()
            )
            assertTrue(CategoryDefaults.isBuiltIn(category))
        }
    }

    @Test
    fun emptyQueryDoesNotReturnEveryDhikr() {
        assertTrue(AzkarData.search("   ").isEmpty())
    }
}
