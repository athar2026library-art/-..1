package com.example.data

import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.FirebaseFunctionsException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class AiRepository {
    private val functions by lazy { FirebaseFunctions.getInstance("us-central1") }

    suspend fun explainZekr(zekr: String): String = withContext(Dispatchers.IO) {
        val prompt = "قم بشرح وتدبر هذا الذكر بأسلوب إيماني، ميسر ومختصر جداً: \n\n\"$zekr\""
        callGemini(prompt)
    }

    suspend fun suggestZekrForFeeling(feeling: String): String = withContext(Dispatchers.IO) {
        val prompt = "أشعر بـ ($feeling) أو أحتاج إلى دعاء بهذا الخصوص. اقترح لي ذكراً أو دعاءً من الأحاديث الصحيحة وحصن المسلم يناسب حالتي.\nنرجو الرد بالتنسيق التالي حصراً:\nالذكر: [النص]\nفضله: [شرح مبسط ومختصر لفضله]\nالمصدر والتخريج: [الكتاب الراوي واسم المرجع كحصن المسلم]"
        callGemini(prompt)
    }

    private suspend fun callGemini(prompt: String): String {
        return try {
            val result = functions
                .getHttpsCallable("generateGemini")
                .call(mapOf("prompt" to prompt))
                .await()
            val data = result.data as? Map<*, *>
            data?.get("text") as? String ?: "عذراً، لم أتمكن من استخراج الإجابة."
        } catch (e: Exception) {
            when {
                e is FirebaseFunctionsException && e.code == FirebaseFunctionsException.Code.RESOURCE_EXHAUSTED ->
                    "عذراً، تم تجاوز حد الاستخدام المؤقت للمساعد الذكي. يرجى المحاولة بعد قليل. ⏳"
                e is FirebaseFunctionsException && e.code == FirebaseFunctionsException.Code.DEADLINE_EXCEEDED ->
                    "عذراً، انتهت مهلة الاستجابة. يرجى المحاولة مرة أخرى. ⏱️"
                e is FirebaseFunctionsException && e.code == FirebaseFunctionsException.Code.UNAVAILABLE ->
                    "عذراً، خدمة المساعد غير متاحة مؤقتاً. يرجى المحاولة لاحقاً. 🌐"
                else -> "حدث خطأ أثناء الاتصال بالمساعد الذكي. تحقق من الاتصال وحاول مرة أخرى لاحقاً. ⚠️"
            }
        }
    }
}
