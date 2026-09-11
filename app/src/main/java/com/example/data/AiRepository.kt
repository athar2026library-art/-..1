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
import com.squareup.moshi.Json

@JsonClass(generateAdapter = true)
data class GenerateContentRequest(
    val contents: List<Content>,
    val generationConfig: GenerationConfig? = null,
    val tools: List<Tool>? = null,
    val systemInstruction: Content? = null
)

@JsonClass(generateAdapter = true)
data class Tool(
    val googleSearch: GoogleSearchTool? = null
)

@JsonClass(generateAdapter = true)
class GoogleSearchTool()

@JsonClass(generateAdapter = true)
data class Content(
    val parts: List<Part>
)

@JsonClass(generateAdapter = true)
data class Part(
    val text: String? = null,
    val inlineData: InlineData? = null
)

@JsonClass(generateAdapter = true)
data class InlineData(
    val mimeType: String,
    val data: String
)

@JsonClass(generateAdapter = true)
data class GenerationConfig(
    val temperature: Float? = null,
    val imageConfig: ImageConfig? = null,
    val responseModalities: List<String>? = null
)

@JsonClass(generateAdapter = true)
data class ImageConfig(
    val aspectRatio: String,
    val imageSize: String
)

@JsonClass(generateAdapter = true)
data class GenerateContentResponse(
    val candidates: List<Candidate>? = null
)

@JsonClass(generateAdapter = true)
data class Candidate(
    val content: Content? = null
)

@JsonClass(generateAdapter = true)
data class GenerateVideosRequest(
    val prompt: String,
    val config: VeoConfig? = null
)

@JsonClass(generateAdapter = true)
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
    ): Any
}

object RetrofitClient {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
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
    
    suspend fun askScholar(question: String): String = withContext(Dispatchers.IO) {
        val request = GenerateContentRequest(
            contents = listOf(Content(parts = listOf(Part(text = question)))),
            systemInstruction = Content(parts = listOf(Part(text = "أنت مساعد إسلامي مفيد. أجب عن أسئلة المستخدم بدقة بناءً على البحث."))),
            tools = listOf(Tool(googleSearch = GoogleSearchTool()))
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
            response.toString()
        } catch (e: Exception) {
            "Error: ${e.message}"
        }
    }
}
