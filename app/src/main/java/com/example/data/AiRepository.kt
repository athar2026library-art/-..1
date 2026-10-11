package com.example.data

import com.example.BuildConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Direct Gemini REST client for local experiments.
 *
 * Set geminiApiKey in local.properties (which is git-ignored). The key is still
 * extractable from a built APK; use this only for private testing and rotate keys
 * that may have been shared publicly.
 */
class AiRepository {
    suspend fun ask(message: String): String = callGemini("suggest", message)

    suspend fun explainZekr(zekr: String): String = callGemini("explain", zekr)

    suspend fun suggestZekrForFeeling(feeling: String): String = callGemini("suggest", feeling)

    private suspend fun callGemini(mode: String, text: String): String {
        val clean = text.trim().take(MAX_INPUT_CHARS)
        if (clean.length < 2) return "اكتب سؤالك أو شعورك أولاً."

        val apiKey = BuildConfig.GEMINI_API_KEY.trim()
        if (apiKey.isEmpty()) {
            return "مفتاح Gemini غير مضبوط. أضف geminiApiKey إلى ملف local.properties ثم أعد بناء التطبيق."
        }

        val prompt = when (mode) {
            "explain" -> "اشرح هذا الذكر باللغة العربية شرحاً موجزاً ومتزناً، ولا تنسب فضائل أو أحاديث بلا مصدر موثوق:\n$clean"
            else -> "أجب باللغة العربية عن طلب المستخدم بما يناسب تطبيق أذكار إسلامي. كن واضحاً ومختصراً، ولا تختلق آيات أو أحاديث أو تنسب نصوصاً إلى مصادر دون تحقق. إن كان السؤال طبياً أو شرعياً تخصصياً فاذكر حدود الإجابة وشجع على الرجوع إلى مختص.\n$clean"
        }

        return try {
            withContext(Dispatchers.IO) {
                val body = JSONObject()
                    .put("contents", JSONArray().put(
                        JSONObject().put("parts", JSONArray().put(JSONObject().put("text", prompt)))
                    ))
                    .put("generationConfig", JSONObject().put("temperature", 0.4))
                    .toString()
                    .toRequestBody(JSON_MEDIA_TYPE)

                val request = Request.Builder()
                    .url(API_URL)
                    .post(body)
                    .header("Content-Type", "application/json")
                    .header("x-goog-api-key", apiKey)
                    .build()

                client.newCall(request).execute().use { response ->
                    val responseText = response.body?.string().orEmpty()
                    if (!response.isSuccessful) {
                        val message = runCatching {
                            JSONObject(responseText).optJSONObject("error")?.optString("message")
                        }.getOrNull().orEmpty()
                        when (response.code) {
                            400, 401, 403 -> "رفض Gemini الطلب. تحقق من المفتاح وإتاحته للخدمة."
                            429 -> "تم بلوغ حد استخدام Gemini المجاني. حاول لاحقاً."
                            else -> "تعذر الاتصال بـ Gemini (HTTP ${response.code}). ${message.take(120)}"
                        }
                    } else {
                        val json = JSONObject(responseText)
                        val candidates = json.optJSONArray("candidates")
                        val parts = candidates?.optJSONObject(0)?.optJSONObject("content")?.optJSONArray("parts")
                        val answer = parts?.let { array ->
                            (0 until array.length()).mapNotNull { array.optJSONObject(it)?.optString("text") }
                                .filter { it.isNotBlank() }.joinToString("\n")
                        }.orEmpty()
                        answer.ifBlank { "لم يُرجع Gemini إجابة نصية. حاول بصياغة أخرى." }
                    }
                }
            }
        } catch (error: CancellationException) {
            throw error
        } catch (error: IOException) {
            "تعذر الاتصال بالإنترنت. تحقق من الاتصال وحاول مرة أخرى."
        } catch (error: Exception) {
            "حدث خطأ أثناء معالجة رد Gemini. حاول مرة أخرى."
        }
    }

    private companion object {
        const val MAX_INPUT_CHARS = 500
        const val API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent"
        val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
        val client = OkHttpClient.Builder()
            .callTimeout(45, TimeUnit.SECONDS)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(45, TimeUnit.SECONDS)
            .build()
    }
}
