package com.example.data

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.FirebaseFunctionsException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await

/**
 * Calls the authenticated Firebase callable function. Gemini credentials stay
 * on the server and are never compiled into the Android APK.
 */
class AiRepository {
    private val functions by lazy { FirebaseFunctions.getInstance(REGION) }

    suspend fun ask(message: String): String = callGemini("suggest", message)

    suspend fun explainZekr(zekr: String): String = callGemini("explain", zekr)

    suspend fun suggestZekrForFeeling(feeling: String): String = callGemini("suggest", feeling)

    private suspend fun callGemini(mode: String, text: String): String {
        val clean = text.trim().take(MAX_INPUT_CHARS)
        if (clean.length < 2) return "اكتب سؤالك أو شعورك أولاً."

        val signedIn = runCatching { FirebaseAuth.getInstance().currentUser != null }
            .getOrDefault(false)
        if (!signedIn) return "سجّل الدخول بحسابك لتشغيل المساعد الذكي."

        return try {
            val result = functions
                .getHttpsCallable(FUNCTION_NAME)
                .call(mapOf("mode" to mode, "text" to clean))
                .await()

            val data = result.data as? Map<*, *>
            // The deployed callable returns { reply }, while older deployments may
            // still return { text }. Accept both response shapes.
            val answer = (data?.get("reply") as? String)
                ?: (data?.get("text") as? String)
            answer?.trim()?.takeIf { it.isNotEmpty() }
                ?: "وصل رد فارغ من المساعد. حاول مرة أخرى."
        } catch (error: CancellationException) {
            throw error
        } catch (error: FirebaseFunctionsException) {
            Log.w(TAG, "Callable failed with code=${error.code}")
            when (error.code) {
                FirebaseFunctionsException.Code.UNAUTHENTICATED ->
                    "انتهت جلسة الدخول. سجّل الدخول مجدداً ثم حاول."
                FirebaseFunctionsException.Code.RESOURCE_EXHAUSTED ->
                    "وصلت إلى حد الاستخدام المؤقت للمساعد. حاول لاحقاً."
                FirebaseFunctionsException.Code.UNAVAILABLE,
                FirebaseFunctionsException.Code.DEADLINE_EXCEEDED ->
                    "المساعد غير متاح حالياً. تحقق من اتصال الإنترنت وحاول لاحقاً."
                FirebaseFunctionsException.Code.FAILED_PRECONDITION ->
                    "خدمة المساعد غير مهيأة بالكامل على الخادم."
                FirebaseFunctionsException.Code.INVALID_ARGUMENT ->
                    "لم أتمكن من فهم الطلب. جرّب صياغة أقصر."
                else -> "تعذر الحصول على إجابة من المساعد. حاول مرة أخرى."
            }
        } catch (error: Exception) {
            Log.e(TAG, "Callable request failed", error)
            "حدث خطأ أثناء الاتصال بالمساعد. تحقق من الإنترنت وحاول مرة أخرى."
        }
    }

    private companion object {
        const val TAG = "AiRepository"
        const val FUNCTION_NAME = "generateGemini"
        const val REGION = "europe-west1"
        const val MAX_INPUT_CHARS = 500
    }
}
