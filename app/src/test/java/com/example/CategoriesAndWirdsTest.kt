package com.example

import com.example.data.AzkarCategory
import com.example.data.CategoryDefaults
import com.example.data.CustomWird
import com.example.data.CustomWirds
import com.example.data.Zekr
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CategoriesAndWirdsTest {
    private fun cat(id: String, order: Int = 0, title: String = "تصنيف $id") =
        AzkarCategory(id, title, "", "star", order)

    @Test fun mergeKeepsBuiltInsFirstAndSortsRemote() {
        val merged = CategoryDefaults.merge(listOf(cat("home", 2), cat("food", 1)))
        assertEquals(listOf("sabah", "masaa", "sleep", "travel", "food", "home"), merged.map { it.id })
    }

    @Test fun mergeDropsReservedDuplicateAndUntitled() {
        val merged = CategoryDefaults.merge(
            listOf(cat("sabah"), cat("favorites"), cat("wird_x"), cat("home"), cat("home", 9), cat("blank", title = " "))
        )
        assertEquals(listOf("sabah", "masaa", "sleep", "travel", "home"), merged.map { it.id })
        assertTrue(merged.first { it.id == "sabah" }.builtIn)
    }

    @Test fun dynamicIdRules() {
        assertTrue(CategoryDefaults.isDynamicId("home"))
        assertFalse(CategoryDefaults.isDynamicId("sabah"))
        assertFalse(CategoryDefaults.isDynamicId("wird_a1"))
        assertFalse(CategoryDefaults.isDynamicId("favorites"))
    }

    private val z = (1..5).map { Zekr(it, "ذكر $it", "", "", 1, "sabah") }

    @Test fun encodeDecodeRoundTrip() {
        val list = listOf(CustomWird("w1", "وردي بعد الفجر", listOf(3, 1, 2)), CustomWird("w2", "قبل النوم", listOf(5)))
        assertEquals(list, CustomWirds.decode(CustomWirds.encode(list)))
    }

    @Test fun namesAreSanitizedAndCorruptLinesIgnored() {
        assertEquals("اسم بتبويب", CustomWirds.cleanName("  اسم\tبتبويب\n "))
        val decoded = CustomWirds.decode("w1\tجيد\t1,2\nbroken line\n\tبلا معرّف\t1\nw3\tغلط\t1,x,3")
        assertEquals(listOf("w1", "w3"), decoded.map { it.id })
        assertEquals(listOf(1, 3), decoded.last().ids)
        assertTrue(CustomWirds.decode(null).isEmpty())
    }

    @Test fun resolveKeepsSavedOrderAndSkipsMissing() {
        val ids = CustomWirds.resolve(CustomWird("w", "x", listOf(4, 99, 2)), z).map { it.id }
        assertEquals(listOf(4, 2), ids)
    }

    @Test fun upsertReplacesOrAppends() {
        val a = CustomWird("a", "أ", listOf(1))
        val b = CustomWird("b", "ب", listOf(2))
        assertEquals(listOf(a, b), CustomWirds.upsert(listOf(a), b))
        val a2 = a.copy(name = "جديد")
        assertEquals(listOf(a2, b), CustomWirds.upsert(listOf(a, b), a2))
        assertNull(CustomWirds.upsert(listOf(a), a2).firstOrNull { it.name == "أ" })
    }
}
