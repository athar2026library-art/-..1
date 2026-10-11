package com.example.data

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.FirebaseFunctionsException
import kotlinx.coroutines.tasks.await
import kotlin.coroutines.cancellation.CancellationException

/**
 * Calls the authenticated Firebase callable function.
 * The Gemini API key stays in Cloud Functions Secret Manager and is never shipped in the APK.
 */
class AiRepository {
    private val auth by lazy { FirebaseAuth.getInstance() }
    private val functions by lazy { FirebaseFunctions.getInstance(FUNCTIONS_REGION) }

    suspend fun ask(message: String): String = callGemini("suggest", message)

    suspend fun explainZekr(zekr: String): String = callGemini("explain", zekr)

    suspend fun suggestZekrForFeeling(feeling: String): String = callGemini("suggest", feeling)

    private suspend fun callGemini(mode: String, text: String): String {
        val clean = text.trim().take(MAX_INPUT_CHARS)
        if (clean.length < 2) return "اكتب سؤالك أو شعورك أولاً."

        return try {
            ensureAuthenticated()

            val response = functions
                .getHttpsCallable(FUNCTION_NAME)
                .call(mapOf("mode" to mode, "text" to clean))
                .await()

            val data = response.data as? Map<*, *>
            val answer = data?.get("text") as? String
            if (answer.isNullOrBlank()) {
                Log.w(TAG, "Callable returned an empty or unexpected response")
                "عذراً، لم أتمكن من استخراج الإجابة. حاول مرة أخرى."
            } else {
                answer.trim()
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: FirebaseFunctionsException) {
            Log.e(TAG, "Callable $FUNCTION_NAME failed with code=${error.code}", error)
            when (error.code) {
                FirebaseFunctionsException.Code.UNAUTHENTICATED ->
                    "لم تكتمل جلسة المساعد. سجّل الدخول بحساب Google أو فعّل تسجيل الدخول المجهول في Firebase."
                FirebaseFunctionsException.Code.PERMISSION_DENIED ->
                    "رفض Firebase الطلب. تحقق من إعداد App Check وصلاحيات المشروع."
                FirebaseFunctionsException.Code.RESOURCE_EXHAUSTED ->
                    "وصلت إلى حد استخدام المساعد مؤقتاً. حاول بعد قليل."
                FirebaseFunctionsException.Code.INVALID_ARGUMENT ->
                    "لم يُقبل نص السؤال. اختصره ثم حاول مرة أخرى."
                FirebaseFunctionsException.Code.UNAVAILABLE,
                FirebaseFunctionsException.Code.DEADLINE_EXCEEDED ->
                    "تعذر الاتصال بالمساعد الآن. تحقق من الإنترنت وحاول مرة أخرى."
                FirebaseFunctionsException.Code.FAILED_PRECONDITION ->
                    "المساعد غير مهيأ على الخادم. يلزم إعداد سر GEMINI_API_KEY في Firebase Functions."
                else -> "تعذر الحصول على إجابة من المساعد. حاول مرة أخرى."
            }
        } catch (error: Exception) {
            Log.e(TAG, "Failed to prepare or call Firebase AI function", error)
            if (auth.currentUser == null) {
                "لتشغيل المساعد دون تسجيل Google، فعّل مزوّد Anonymous في Firebase Authentication. أو سجّل الدخول بحساب Google."
            } else {
                "حدث خطأ أثناء الاتصال بالمساعد. تحقق من إعداد Firebase والإنترنت."
            }
        }
    }

    /**
     * The callable function rate-limits by UID and requires Firebase Auth.
     * Use anonymous auth only when the user has not chosen to sign in with Google.
     */
    private suspend fun ensureAuthenticated() {
        if (auth.currentUser != null) return
        try {
            auth.signInAnonymously().await()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            Log.e(TAG, "Anonymous Firebase auth is unavailable", error)
            throw IllegalStateException("AI authentication is not configured", error)
        }
    }

    private companion object {
        const val TAG = "AiRepository"
        const val FUNCTION_NAME = "generateGemini"
        const val FUNCTIONS_REGION = "europe-west1"
        const val MAX_INPUT_CHARS = 500
    }
}
