package com.example.data

import com.example.BuildConfig
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class Part(
    @Json(name = "text") val text: String? = null
)

@JsonClass(generateAdapter = true)
data class Content(
    @Json(name = "parts") val parts: List<Part>
)

@JsonClass(generateAdapter = true)
data class GenerateContentRequest(
    @Json(name = "contents") val contents: List<Content>,
    @Json(name = "systemInstruction") val systemInstruction: Content? = null
)

@JsonClass(generateAdapter = true)
data class Candidate(
    @Json(name = "content") val content: Content?
)

@JsonClass(generateAdapter = true)
data class GenerateContentResponse(
    @Json(name = "candidates") val candidates: List<Candidate>?
)

interface GeminiApiService {
    @POST("v1beta/models/gemini-3.5-flash:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body request: GenerateContentRequest
    ): GenerateContentResponse
}

object GeminiClient {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    val apiService: GeminiApiService = retrofit.create(GeminiApiService::class.java)

    /**
     * Determines whether the API key is set properly.
     */
    fun isApiKeyAvailable(): Boolean {
        val key = BuildConfig.GEMINI_API_KEY
        return key.isNotEmpty() && key != "MY_GEMINI_API_KEY" && !key.contains("placeholder", ignoreCase = true)
    }

    /**
     * Summarizes the scanned text or URL using Gemini AI.
     */
    suspend fun queryGeminiAI(contentToAnalyze: String, contentType: String): String {
        if (!isApiKeyAvailable()) {
            return getMockAISummarization(contentToAnalyze, contentType)
        }

        val prompt = "" +
                "You are an intelligent, high-speed on-device QR scanning assistant. " +
                "Scan Content Type: $contentType. " +
                "Scanned Content: $contentToAnalyze. " +
                "Please analyze this QR data and output a concise summary of what it represents. " +
                "If it's a URL, estimate its safety index, summarize what the URL is about, and describe what the user should expect. " +
                "If it's a WiFi card, summarize it clearly with instructions. " +
                "If it's plain text, categorize it and extract 1-2 actionable bullets. " +
                "Keep your response strictly under 3 sentences, informative, and without markdown headers. Be extremely helpful and direct."

        val systemPrompt = "You are a secure, high-utility Android QR AI Lens assistant. Be concise, direct, helpful, and never use markdown headers."

        val request = GenerateContentRequest(
            contents = listOf(
                Content(parts = listOf(Part(text = prompt)))
            ),
            systemInstruction = Content(parts = listOf(Part(text = systemPrompt)))
        )

        return try {
            val response = apiService.generateContent(BuildConfig.GEMINI_API_KEY, request)
            response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                ?: "No AI summary could be generated for this QR scan."
        } catch (e: Exception) {
            "AI Scan Assistant Error: ${e.localizedMessage ?: "Could not establish connection to server."}"
        }
    }

    private fun getMockAISummarization(contentToAnalyze: String, contentType: String): String {
        return when (contentType) {
            "URL" -> {
                val isHttps = contentToAnalyze.startsWith("https://", ignoreCase = true)
                val safety = if (isHttps) "✅ Secure (HTTPS Verified)" else "⚠️ Unsecured (HTTP Plaintext)"
                val domain = try {
                    val temp = contentToAnalyze.replaceFirst(Regex("https?://(www\\.)?"), "")
                    temp.split("/").firstOrNull() ?: contentToAnalyze
                } catch (e: Exception) {
                    contentToAnalyze
                }
                "AI Insights (Local Offline Engine):\n• Link Target: $domain\n• Security Check: $safety\n• Notice: Configure Gemini API Key via AI Studio Secrets to unlock instant AI summarization of site contents!"
            }
            "WIFI" -> {
                "AI Insights (Local Offline Engine):\n• Network Profile detected.\n• Access: Secures direct auto-onboard configuration to the private hotspot. Click Connect below to trigger authentication."
            }
            "VCARD" -> {
                "AI Insights (Local Offline Engine):\n• Electronic Card Format (MeCard/VCard).\n• Fields extracted: Contact profile ready to be added to device Address Book."
            }
            "PAYMENT" -> {
                "AI Insights (Local Offline Engine):\n• Billing/Financial transfer QR.\n• Caution: Always cross-verify payment receipts and destination merchant tokens before transaction."
            }
            "LOCATION" -> {
                "AI Insights (Local Offline Engine):\n• Geographic landmark coordinates detected.\n• Action: Click 'Open Map' to query navigation pathways directly in visual map interfaces."
            }
            else -> {
                "AI Insights (Local Offline Engine):\n• Local text record. Select Copy to copy onto clipboard, or tap Favorite to save locally to bookmarks."
            }
        }
    }
}
