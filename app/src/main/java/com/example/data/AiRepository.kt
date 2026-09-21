package com.example.data

import com.example.BuildConfig
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class GenerateContentRequest(
    val contents: List<Content>,
    val generationConfig: GenerationConfig? = null,
    val systemInstruction: Content? = null
)

@JsonClass(generateAdapter = true)
data class Content(
    val parts: List<Part>
)

@JsonClass(generateAdapter = true)
data class Part(
    val text: String? = null
)

@JsonClass(generateAdapter = true)
data class GenerationConfig(
    val temperature: Float? = null,
    val responseModalities: List<String>? = null
)

@JsonClass(generateAdapter = true)
data class GenerateContentResponse(
    val candidates: List<Candidate>? = null
)

@JsonClass(generateAdapter = true)
data class Candidate(
    val content: Content? = null
)

interface GeminiApiService {
    @POST("v1beta/models/{model}:generateContent")
    suspend fun generateContent(
        @Path("model") model: String,
        @Query("key") apiKey: String,
        @Body request: GenerateContentRequest
    ): GenerateContentResponse
}

object RetrofitClient {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .build()

    val service: GeminiApiService by lazy {
        val moshi = Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()
        
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
        retrofit.create(GeminiApiService::class.java)
    }
}

class AiRepository {

    private val apiKey: String
        get() = BuildConfig.GEMINI_API_KEY

    private val isKeyConfigured: Boolean
        get() = apiKey.isNotBlank() &&
                apiKey != "MY_GEMINI_API_KEY" &&
                !apiKey.startsWith("YOUR_") &&
                apiKey.length > 20

    suspend fun explainZekr(zekr: String): String = withContext(Dispatchers.IO) {
        if (!isKeyConfigured) {
            return@withContext "الميزة الذكية غير مفعّلة حالياً. يرجى إعداد مفتاح Gemini بشكل آمن (Cloud Function أو Firebase AI)."
        }
        val prompt = "قم بشرح وتدبر هذا الذكر بأسلوب إيماني، ميسر ومختصر جداً: \n\n\"$zekr\""
        callGemini(prompt)
    }

    suspend fun suggestZekrForFeeling(feeling: String): String = withContext(Dispatchers.IO) {
        if (!isKeyConfigured) {
            return@withContext "الميزة الذكية غير مفعّلة حالياً. يرجى إعداد مفتاح Gemini بشكل آمن (Cloud Function أو Firebase AI)."
        }
        val prompt = "أشعر بـ ($feeling) أو أحتاج إلى دعاء بهذا الخصوص. اقترح لي ذكراً أو دعاءً من الأحاديث الصحيحة وحصن المسلم يناسب حالتي.\nنرجو الرد بالتنسيق التالي حصراً:\nالذكر: [النص]\nفضله: [شرح مبسط ومختصر لفضله]\nالمصدر والتخريج: [الكتاب الراوي واسم المرجع كحصن المسلم]"
        callGemini(prompt)
    }

    private suspend fun callGemini(prompt: String): String {
        val request = GenerateContentRequest(
            contents = listOf(Content(parts = listOf(Part(text = prompt)))),
            systemInstruction = Content(parts = listOf(Part(text = "أنت مساعد إسلامي متخصص في الأذكار والدعاء. اعتمد فقط على الأحاديث الصحيحة وكتاب (حصن المسلم). قدم إجاباتك بأسلوب ميسر، مختصر جداً، وهادئ. لا تفتي ولا تصدر أحكاماً شرعية من عندك، واكتفِ بشرح وتدبر الأذكار، أو اقتراح أذكار تناسب حاجة المستخدم بناءً على المصادر الموثوقة المذكورة."))),
            generationConfig = GenerationConfig(temperature = 0.4f)
        )
        return try {
            val response = RetrofitClient.service.generateContent("gemini-2.5-flash", apiKey, request)
            response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: "عذراً، لم أتمكن من استخراج الإجابة."
        } catch (e: Exception) {
            if (e is java.net.UnknownHostException || e is java.net.SocketTimeoutException) {
                "عذراً، لا يوجد اتصال بالإنترنت. يرجى التحقق من الشبكة والمحاولة مرة أخرى 🌐"
            } else {
                "حدث خطأ غير متوقع أثناء الاتصال. حاول مرة أخرى لاحقاً. ⚠️"
            }
        }
    }
}
