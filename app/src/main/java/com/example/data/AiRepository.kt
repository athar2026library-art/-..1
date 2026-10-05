package com.example.data

import android.util.Log
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.FirebaseFunctionsException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

/**
 * مسار واحد للذكاء الاصطناعي: Cloud Function [generateGemini]
 * (App Check + auth + rate limit + Secret على السيرفر).
 * لا مفتاح ولا Firebase AI Logic داخل الـ APK.
 */
class AiRepository {

    private val functions by lazy {
        FirebaseFunctions.getInstance("europe-west1")
    }

    suspend fun ask(message: String): String = callGemini("suggest", message)

    suspend fun explainZekr(zekr: String): String = callGemini("explain", zekr)

    suspend fun suggestZekrForFeeling(feeling: String): String = callGemini("suggest", feeling)

    private suspend fun callGemini(mode: String, text: String): String = withContext(Dispatchers.IO) {
        val clean = text.trim().take(MAX_INPUT_CHARS)
        if (clean.length < 2) return@withContext "اكتب سؤالك أو شعورك أولاً."

        try {
            val result = functions
                .getHttpsCallable("generateGemini")
                .call(mapOf("mode" to mode, "text" to clean))
                .await()

            @Suppress("UNCHECKED_CAST")
            val data = result.data as? Map<String, Any?>
            val out = data?.get("text") as? String
            out?.trim().takeUnless { it.isNullOrEmpty() }
                ?: "عذراً، لم أتمكن من استخراج الإجابة."
        } catch (e: CancellationException) {
            throw e
        } catch (e: FirebaseFunctionsException) {
            Log.e("AiRepository", "generateGemini ${e.code}", e)
            when (e.code) {
                FirebaseFunctionsException.Code.UNAUTHENTICATED ->
                    "سجّل الدخول أولاً لاستخدام المساعد الذكي."
                FirebaseFunctionsException.Code.RESOURCE_EXHAUSTED ->
                    "وصلت لحد الاستخدام. حاول بعد قليل."
                FirebaseFunctionsException.Code.INVALID_ARGUMENT ->
                    "النص غير مقبول. اختصر أو أعد الصياغة."
                FirebaseFunctionsException.Code.UNAVAILABLE ->
                    "عذراً، لا يوجد اتصال بالإنترنت. تحقق من الشبكة 🌐"
                else ->
                    "تعذر الحصول على إجابة. حاول لاحقاً."
            }
        } catch (e: Exception) {
            Log.e("AiRepository", "generateGemini failed", e)
            "حدث خطأ غير متوقع أثناء الاتصال. حاول مرة أخرى ⚠️"
        }
    }

    private companion object {
        const val MAX_INPUT_CHARS = 500
    }
}
