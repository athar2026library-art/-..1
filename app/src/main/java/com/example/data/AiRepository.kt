package com.example.data

import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.FirebaseFunctionsException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

/**
 * Gemini فقط عبر Callable Cloud Function (europe-west1).
 * لا مفتاح في الـ APK — المصادقة + App Check + حد معدل على الخادم.
 */
class AiRepository {
    private val functions by lazy { FirebaseFunctions.getInstance("europe-west1") }

    suspend fun explainZekr(zekr: String): String =
        ask(mode = "explain", text = zekr)

    suspend fun suggestZekrForFeeling(feeling: String): String =
        ask(mode = "suggest", text = feeling)

    private suspend fun ask(mode: String, text: String): String = withContext(Dispatchers.IO) {
        try {
            val result = functions
                .getHttpsCallable("generateGemini")
                .call(mapOf("mode" to mode, "text" to text))
                .await()
            val data = result.data as? Map<*, *>
            val body = data?.get("text") as? String
            when {
                body.isNullOrBlank() -> "عذراً، لم أتمكن من استخراج الإجابة."
                else -> body
            }
        } catch (e: FirebaseFunctionsException) {
            when (e.code) {
                FirebaseFunctionsException.Code.UNAUTHENTICATED ->
                    "سجّل الدخول للمساعد"
                FirebaseFunctionsException.Code.RESOURCE_EXHAUSTED ->
                    "وصلت للحد، حاول بعد قليل"
                FirebaseFunctionsException.Code.UNAVAILABLE ->
                    "لا اتصال"
                FirebaseFunctionsException.Code.INVALID_ARGUMENT ->
                    "النص غير صالح أو طويل جداً"
                FirebaseFunctionsException.Code.DEADLINE_EXCEEDED ->
                    "انتهت مهلة الاستجابة. حاول مرة أخرى."
                else ->
                    "حدث خطأ أثناء الاتصال بالمساعد. حاول لاحقاً."
            }
        } catch (_: Exception) {
            "لا اتصال"
        }
    }
}
