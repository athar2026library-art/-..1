package com.example.data

import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/**
 * Firebase AI Logic SDK — no Gemini API key in the APK.
 * Auth is the Firebase project + App Check (AzkarApplication).
 */
private const val SYSTEM_INSTRUCTION =
    "أنت مساعد إسلامي متخصص في الأذكار والدعاء. اعتمد فقط على الأحاديث الصحيحة وكتاب (حصن المسلم). " +
        "قدم إجاباتك بأسلوب ميسر، مختصر جداً، وهادئ. لا تفتِ ولا تصدر أحكاماً شرعية من عندك، " +
        "واكتفِ بشرح وتدبر الأذكار، أو اقتراح أذكار تناسب حاجة المستخدم بناءً على المصادر الموثوقة المذكورة."

class AiRepository {

    private val generativeModel by lazy {
        Firebase.ai(backend = GenerativeBackend.googleAI()).generativeModel(
            modelName = "gemini-2.5-flash",
            systemInstruction = content { text(SYSTEM_INSTRUCTION) },
            generationConfig = generationConfig { temperature = 0.4f }
        )
    }

    suspend fun explainZekr(zekr: String): String = withContext(Dispatchers.IO) {
        val prompt = "قم بشرح وتدبر هذا الذكر بأسلوب إيماني، ميسر ومختصر جداً: \n\n\"$zekr\""
        callGemini(prompt)
    }

    suspend fun suggestZekrForFeeling(feeling: String): String = withContext(Dispatchers.IO) {
        val prompt =
            "أشعر بـ ($feeling) أو أحتاج إلى دعاء بهذا الخصوص. اقترح لي ذكراً أو دعاءً من الأحاديث الصحيحة وحصن المسلم يناسب حالتي.\n" +
                "نرجو الرد بالتنسيق التالي حصراً:\n" +
                "الذكر: [النص]\n" +
                "فضله: [شرح مبسط ومختصر لفضله]\n" +
                "المصدر والتخريج: [الكتاب الراوي واسم المرجع كحصن المسلم]"
        callGemini(prompt)
    }

    private suspend fun callGemini(prompt: String): String {
        return try {
            val response = generativeModel.generateContent(prompt)
            response.text?.takeIf { it.isNotBlank() } ?: "عذراً، لم أتمكن من استخراج الإجابة."
        } catch (e: UnknownHostException) {
            "عذراً، لا يوجد اتصال بالإنترنت. يرجى التحقق من الشبكة والمحاولة مرة أخرى 🌐"
        } catch (e: SocketTimeoutException) {
            "عذراً، لا يوجد اتصال بالإنترنت. يرجى التحقق من الشبكة والمحاولة مرة أخرى 🌐"
        } catch (e: Exception) {
            if (e.message?.contains("429") == true || e.message?.contains("RESOURCE_EXHAUSTED") == true) {
                "عذراً، تم تجاوز حد الاستخدام المؤقت للمساعد الذكي. يرجى المحاولة بعد قليل. ⏳"
            } else {
                "حدث خطأ غير متوقع أثناء الاتصال. حاول مرة أخرى لاحقاً. ⚠️"
            }
        }
    }
}
