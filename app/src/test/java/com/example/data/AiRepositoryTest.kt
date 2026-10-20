package com.example.data

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test

class AiRepositoryTest {
    private val repository = AiRepository()

    @Test
    fun morningQuestionReturnsBundledTextAndSource() = runBlocking {
        val answer = repository.ask("ما أذكار الصباح؟")
        assertTrue(answer.contains("أذكار الصباح"))
        assertTrue(answer.contains("المصدر:"))
        assertTrue(answer.contains("دون اتصال"))
    }

    @Test
    fun travelQuestionUsesOfflineTravelData() = runBlocking {
        val answer = repository.ask("أنا مسافر، ماذا أقول؟")
        assertTrue(answer.contains("أذكار السفر"))
        assertTrue(answer.contains("المصدر:"))
    }

    @Test
    fun unknownQuestionDoesNotPretendToContactGemini() = runBlocking {
        val answer = repository.ask("أريد معلومات عن كوكب زحل")
        assertTrue(answer.contains("قاعدة الأذكار المحلية"))
        assertTrue(!answer.contains("Gemini"))
    }
}
