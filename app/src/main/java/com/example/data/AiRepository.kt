package com.example.data

import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import kotlinx.coroutines.CancellationException

/**
 * Uses Firebase AI Logic with the Gemini Developer API.
 * This avoids requiring a separately deployed Cloud Function for ordinary AI chat.
 * Firebase App Check must be configured/enforced for Firebase AI Logic in the console.
 */
class AiRepository {
    private val model by lazy {
        Firebase.ai(backend = GenerativeBackend.googleAI())
            .generativeModel(MODEL_NAME)
    }

    suspend fun ask(message: String): String = generate("suggest", message)

    suspend fun explainZekr(zekr: String): String = generate("explain", zekr)

    suspend fun suggestZekrForFeeling(feeling: String): String = generate("suggest", feeling)

    private suspend fun generate(mode: String, input: String): String {
        val clean = input.trim().take(MAX_INPUT_CHARS)
        if (clean.length < 2) return "اكتب سؤالك أو شعورك أولاً."

        val prompt = buildPrompt(mode, clean)
        return try {
            val response = model.generateContent(prompt)
            response.text
                ?.trim()
                ?.takeIf { it.isNotEmpty() }
                ?: "وصل رد فارغ من Gemini. أعد صياغة السؤال وحاول مرة أخرى."
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            Log.e(TAG, "Firebase AI Logic request failed", error)
            "تعذر الاتصال بمساعد Gemini. تحقق من تفعيل Firebase AI Logic واختيار Gemini Developer API في مشروع Firebase، ثم أعد المحاولة. إذا استمر الخطأ فتحقق من إعداد App Check واتصال الإنترنت."
        }
    }

    private fun buildPrompt(mode: String, text: String): String {
        val task = if (mode == "explain") {
            "اشرح هذا الذكر بأسلوب إيماني ميسر ومختصر جداً، وبيّن معناه دون اختلاق فضل أو نسبة حديث بغير علم:\n\n«$text»"
        } else {
            "أشعر بـ ($text) أو أحتاج إلى دعاء بهذا الخصوص. اقترح ذكراً أو دعاءً مناسباً من الأحاديث الصحيحة وكتاب حصن المسلم. لا تخترع نصاً أو مصدراً. إذا لم تستطع التأكد من المصدر، فصرّح بذلك.\n\nاكتب الإجابة بهذا التنسيق:\nالذكر: [النص]\nفضله: [شرح مبسط]\nالمصدر: [المرجع]"
        }
        return "$SYSTEM_INSTRUCTION\n\n$task\n\nفي نهاية الإجابة أضف: (إجابة آلية، ليست فتوى)."
    }

    private companion object {
        const val TAG = "AiRepository"
        const val MODEL_NAME = "gemini-3.5-flash-lite"
        const val MAX_INPUT_CHARS = 500
        const val SYSTEM_INSTRUCTION =
            "أنت مساعد إسلامي متخصص في الأذكار والدعاء. اعتمد على الأحاديث الصحيحة وكتاب حصن المسلم فقط. " +
            "كن موجزاً وهادئاً، ولا تصدر فتاوى أو أحكاماً شرعية. لا تختلق نصوصاً أو مراجع أو فضائل."
    }
}
