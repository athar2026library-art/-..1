package com.example.data

import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/**
 * عميل Gemini مباشر للتطوير المحلي فقط.
 *
 * يقرأ المفتاح من local.properties عبر BuildConfig؛ لا تضع المفتاح في Git
 * ولا توزع نسخة release مبنية بهذا المسار على المستخدمين.
 */
class AiRepository {

    suspend fun ask(message: String): String = callGemini("suggest", message)

    suspend fun explainZekr(zekr: String): String = callGemini("explain", zekr)

    suspend fun suggestZekrForFeeling(feeling: String): String = callGemini("suggest", feeling)

    private suspend fun callGemini(mode: String, text: String): String = withContext(Dispatchers.IO) {
        val clean = text.trim().take(MAX_INPUT_CHARS)
        if (clean.length < 2) return@withContext "اكتب سؤالك أو شعورك أولاً."

        val apiKey = BuildConfig.GEMINI_API_KEY.trim()
        if (apiKey.isEmpty()) {
            return@withContext "المساعد غير مهيأ للتطوير المحلي. أضف GEMINI_API_KEY إلى local.properties."
        }

        val prompt = when (mode) {
            "explain" -> "اشرح هذا الذكر باختصار وبلغة عربية واضحة، مع بيان معناه وفائدته دون اختلاق مصادر:\n$clean"
            else -> "أجب بالعربية بإيجاز وبأسلوب داعم حول طلب المستخدم التالي، ولا تقدم فتوى أو مصدرًا غير متأكد منه:\n$clean"
        }

        var connection: HttpURLConnection? = null
        try {
            val endpoint = URL(
                "https://generativelanguage.googleapis.com/v1beta/models/" +
                    "$MODEL:generateContent?key=$apiKey"
            )
            connection = (endpoint.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = TIMEOUT_MS
                readTimeout = TIMEOUT_MS
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=UTF-8")
            }

            val body = JSONObject()
                .put("contents", org.json.JSONArray().put(
                    JSONObject().put("parts", org.json.JSONArray().put(JSONObject().put("text", prompt)))
                ))
                .put("generationConfig", JSONObject()
                    .put("temperature", 0.7)
                    .put("maxOutputTokens", 512))
                .toString()

            connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val responseText = stream?.bufferedReader()?.use { it.readText() }.orEmpty()

            if (status !in 200..299) {
                Log.e("AiRepository", "Gemini HTTP $status: ${responseText.take(300)}")
                return@withContext when (status) {
                    400 -> "طلب غير صالح إلى Gemini. تحقق من إعداد التطوير."
                    401, 403 -> "مفتاح Gemini غير صالح أو غير مصرح به."
                    429 -> "تم تجاوز حد الاستخدام. حاول لاحقاً."
                    else -> "تعذر الاتصال بالمساعد. حاول لاحقاً."
                }
            }

            val output = JSONObject(responseText)
                .optJSONArray("candidates")
                ?.optJSONObject(0)
                ?.optJSONObject("content")
                ?.optJSONArray("parts")
                ?.optJSONObject(0)
                ?.optString("text")
                ?.trim()
                .orEmpty()

            output.ifEmpty { "عذراً، لم أتمكن من استخراج الإجابة." }
        } catch (e: CancellationException) {
            throw e
        } catch (e: java.net.SocketTimeoutException) {
            Log.e("AiRepository", "Gemini timeout", e)
            "انتهت مهلة الاتصال. تحقق من الشبكة وحاول مرة أخرى."
        } catch (e: IOException) {
            Log.e("AiRepository", "Gemini network failure", e)
            "لا يوجد اتصال بالإنترنت. تحقق من الشبكة وحاول مرة أخرى."
        } catch (e: Exception) {
            Log.e("AiRepository", "Gemini request failed", e)
            "حدث خطأ غير متوقع أثناء الاتصال بالمساعد."
        } finally {
            connection?.disconnect()
        }
    }

    private companion object {
        const val MODEL = "gemini-1.5-flash"
        const val MAX_INPUT_CHARS = 500
        const val TIMEOUT_MS = 20_000
    }
}
