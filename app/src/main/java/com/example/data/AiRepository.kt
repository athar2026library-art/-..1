package com.example.data

import android.util.Log
import com.google.firebase.ai.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.ThinkingLevel
import com.google.firebase.ai.type.generationConfig
import com.google.firebase.ai.type.thinkingConfig
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.FirebaseFunctionsException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlin.coroutines.cancellation.CancellationException

class AiRepository {

    companion object {
        private const val MAX_INPUT_CHARS = 800

        private const val DIRECT_SYSTEM_INSTRUCTION =
            "أنت مساعد إسلامي متخصص في الأذكار والدعاء. " +
                "اعتمد فقط على الأحاديث الصحيحة وكتاب حصن المسلم. " +
                "قدم إجاباتك بالعربية بأسلوب ميسر ومختصر وهادئ. " +
                "لا تفتِ ولا تصدر أحكاماً شرعية من عندك. " +
                "لا تخترع أحاديث أو أذكاراً من عندك."

        private const val DISCLAIMER =
            "\n\n— هذا رد آلي للمساعدة العامة وليس فتوى شرعية."
    }

    private val functions: FirebaseFunctions by lazy {
        FirebaseFunctions.getInstance()
    }

    private val directModel by lazy {
        val config = generationConfig {
            thinkingConfig = thinkingConfig {
                thinkingLevel = ThinkingLevel.LOW
            }
        }

        Firebase.ai(
            backend = GenerativeBackend.googleAI()
        ).generativeModel(
            modelName = "gemini-2.5-flash",
            generationConfig = config,
            systemInstruction = DIRECT_SYSTEM_INSTRUCTION,
        )
    }

    suspend fun explainZekr(zekr: String): String =
        callGemini(mode = "explain", text = zekr)

    suspend fun suggestZekrForFeeling(feeling: String): String =
        callGemini(mode = "suggest", text = feeling)

    private fun buildDirectPrompt(mode: String, text: String): String {
        return when (mode) {
            "explain" ->
                "قم بشرح وتدبر هذا الذكر بأسلوب إيماني، ميسر ومختصر جداً:\n\n\"$text\""
            else ->
                "أشعر بـ ($text) أو أحتاج إلى دعاء بهذا الخصوص. " +
                    "اقترح لي ذكراً أو دعاءً من الأحاديث الصحيحة وحصن المسلم يناسب حالتي.\n" +
                    "نرجو الرد بالتنسيق التالي حصراً:\n" +
                    "الذكر: [النص]\nفضله: [شرح مبسط ومختصر لفضله]\nالمصدر والتخريج: [المرجع]"
        }
    }

    private fun ensureDisclaimer(text: String): String {
        val trimmed = text.trim()
        return if (trimmed.contains("ليس فتوى") || trimmed.contains("ليس حكماً شرعياً")) {
            trimmed
        } else {
            trimmed + DISCLAIMER
        }
    }

    private suspend fun callGemini(
        mode: String,
        text: String
    ): String = withContext(Dispatchers.IO) {

        val clean = text
            .trim()
            .take(MAX_INPUT_CHARS)

        if (clean.length < 2) {
            return@withContext "اكتب سؤالك أو شعورك أولاً."
        }

        var directFailure: Throwable? = null

        try {
            val prompt = buildDirectPrompt(mode, clean)
            val response = directModel.generateContent(prompt)
            val out = response.text?.trim()

            if (!out.isNullOrEmpty()) {
                return@withContext ensureDisclaimer(out)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            directFailure = e
            Log.w(
                "AiRepository",
                "Direct Firebase AI Logic failed; trying Cloud Function fallback",
                e
            )
        }

        try {
            val result = functions
                .getHttpsCallable("generateGemini")
                .call(
                    mapOf(
                        "mode" to mode,
                        "text" to clean
                    )
                )
                .await()

            @Suppress("UNCHECKED_CAST")
            val data = result.data as? Map<String, Any?>

            val out = data?.get("text") as? String

            out?.trim()
                .takeUnless { it.isNullOrEmpty() }
                ?.let {
                    return@withContext ensureDisclaimer(it)
                }
                ?: "عذراً، لم أتمكن من استخراج الإجابة."

        } catch (e: FirebaseFunctionsException) {
            when (e.code) {
                FirebaseFunctionsException.Code.UNAUTHENTICATED ->
                    "سجّل الدخول أولاً لاستخدام المساعد الذكي."

                FirebaseFunctionsException.Code.PERMISSION_DENIED ->
                    "المساعد محمي بـ App Check ولم يتم اعتماد هذا الجهاز بعد."

                FirebaseFunctionsException.Code.RESOURCE_EXHAUSTED ->
                    "وصلت لحد الاستخدام. حاول بعد قليل."

                FirebaseFunctionsException.Code.INVALID_ARGUMENT ->
                    "النص غير مقبول. اختصر أو أعد الصياغة."

                FirebaseFunctionsException.Code.UNAVAILABLE ->
                    "لا يوجد اتصال بالإنترنت. تحقق من الشبكة 🌐"

                FirebaseFunctionsException.Code.NOT_FOUND ->
                    "خدمة المساعد غير منشورة بعد."

                else ->
                    "تعذر الحصول على إجابة. تأكد من إعداد Firebase AI."
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e("AiRepository", "Cloud Function fallback failed", e)
            if (directFailure != null) {
                "تعذر الاتصال بالمساعد الذكي. تحقق من الشبكة وإعدادات Firebase."
            } else {
                "حدث خطأ غير متوقع. حاول مرة أخرى لاحقاً."
            }
        }
    }
}
