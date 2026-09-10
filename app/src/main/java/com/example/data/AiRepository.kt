package com.example.data

import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

@Serializable
data class GenerateContentRequest(
    val contents: List<Content>,
    val generationConfig: GenerationConfig? = null,
    val tools: List<JsonObject>? = null,
    val systemInstruction: Content? = null
)

@Serializable
data class Content(
    val parts: List<Part>
)

@Serializable
data class Part(
    val text: String? = null,
    val inlineData: InlineData? = null
)

@Serializable
data class InlineData(
    val mimeType: String,
    val data: String
)

@Serializable
data class GenerationConfig(
    val temperature: Float? = null,
    val imageConfig: ImageConfig? = null,
    val responseModalities: List<String>? = null
)

@Serializable
data class ImageConfig(
    val aspectRatio: String,
    val imageSize: String
)

@Serializable
data class GenerateContentResponse(
    val candidates: List<Candidate>? = null
)

@Serializable
data class Candidate(
    val content: Content? = null
)

@Serializable
data class GenerateVideosRequest(
    val prompt: String,
    val config: VeoConfig? = null
)

@Serializable
data class VeoConfig(
    val numberOfVideos: Int,
    val resolution: String,
    val aspectRatio: String
)

interface GeminiApiService {
    @POST("v1beta/models/{model}:generateContent")
    suspend fun generateContent(
        @Path("model") model: String,
        @Query("key") apiKey: String,
        @Body request: GenerateContentRequest
    ): GenerateContentResponse
    
    @POST("v1beta/models/{model}:generateVideos")
    suspend fun generateVideos(
        @Path("model") model: String,
        @Query("key") apiKey: String,
        @Body request: GenerateVideosRequest
    ): JsonObject
}

object RetrofitClient {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    val service: GeminiApiService by lazy {
        val json = Json { ignoreUnknownKeys = true }
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
        retrofit.create(GeminiApiService::class.java)
    }
}

class AiRepository {
    
    suspend fun askScholar(question: String): String = withContext(Dispatchers.IO) {
        val request = GenerateContentRequest(
            contents = listOf(Content(parts = listOf(Part(text = question)))),
            systemInstruction = Content(parts = listOf(Part(text = "أنت مساعد إسلامي مفيد. أجب عن أسئلة المستخدم بدقة بناءً على البحث."))),
            tools = listOf(
                buildJsonObject {
                    putJsonObject("googleSearch") {}
                }
            )
        )
        try {
            val response = RetrofitClient.service.generateContent("gemini-3.5-flash", BuildConfig.GEMINI_API_KEY, request)
            response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: "عذراً، لم أتمكن من الإجابة."
        } catch (e: Exception) {
            "حدث خطأ أثناء البحث: ${e.message}"
        }
    }
    
    suspend fun generateImage(prompt: String): String? = withContext(Dispatchers.IO) {
        val request = GenerateContentRequest(
            contents = listOf(Content(parts = listOf(Part(text = prompt)))),
            generationConfig = GenerationConfig(
                imageConfig = ImageConfig(aspectRatio = "1:1", imageSize = "1K"),
                responseModalities = listOf("TEXT", "IMAGE")
            )
        )
        try {
            val response = RetrofitClient.service.generateContent("gemini-3.1-flash-image-preview", BuildConfig.GEMINI_API_KEY, request)
            // The image is returned as base64 in inlineData
            val part = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull { it.inlineData != null }
            part?.inlineData?.data
        } catch (e: Exception) {
            null
        }
    }
    
    suspend fun generateVideo(prompt: String): String? = withContext(Dispatchers.IO) {
        val request = GenerateVideosRequest(
            prompt = prompt,
            config = VeoConfig(numberOfVideos = 1, resolution = "1080p", aspectRatio = "16:9")
        )
        try {
            val response = RetrofitClient.service.generateVideos("veo-3.1-fast-generate-preview", BuildConfig.GEMINI_API_KEY, request)
            // Normally returns an operation ID to poll, but for this preview it might be different or just a stub.
            // We'll return the raw json string for now to debug.
            response.toString()
        } catch (e: Exception) {
            "Error: ${e.message}"
        }
    }
}
