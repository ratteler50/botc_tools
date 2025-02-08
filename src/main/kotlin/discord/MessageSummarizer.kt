package discord

import com.google.gson.GsonBuilder
import io.github.oshai.kotlinlogging.KotlinLogging
import java.util.concurrent.TimeUnit.MINUTES
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

/**
 * Summarizes message exchanges from a game of Blood on the Clocktower using Google's Gemini API.
 */
class MessageSummarizer {

  private val logger = KotlinLogging.logger {}
  private val client = OkHttpClient.Builder().readTimeout(1, MINUTES).build()
  private val gson = GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create()

  private companion object {
    private const val BASE_URL =
      "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent"

    private val BASE_PROMPT = """
            Summarize in detail the following Blood on the Clocktower exchange, explicitly separating public and private conversations and organizing by game day. Ensure the summary captures:
            • Theories & Worlds: The narratives and possibilities players are constructing about roles and alignments.
            • Claims: Any characters that players are claiming to others.
            • Key Accusations & Deductions: Notable logical leaps, suspicions, or alliances forming.
            
            Maintain clarity and conciseness while preserving all essential context. Prioritize accuracy in capturing the logical flow of discussions.
        """.trimIndent()

    private val MEDIA_TYPE_JSON = "application/json".toMediaType()
  }

  private data class ResponsePayload(val candidates: List<Candidate>) {
    data class Candidate(val content: Content)
  }

  private data class RequestPayload(val contents: List<Content>)
  private data class Content(val parts: List<Part>) {
    data class Part(val text: String)
  }

  /**
   * Sends the message content to the Gemini API for summarization.
   *
   * @param inputText The text input to be summarized.
   * @return The summarized output text or an empty string if the request fails.
   */
  private fun summarizeText(inputText: String): String {
    val httpUrl = BASE_URL.toHttpUrl().newBuilder()
      .addQueryParameter("key", "GEMINI_KEY")
      .build()

    val requestPayload =
      RequestPayload(listOf(Content(listOf(Content.Part("$BASE_PROMPT\n$inputText")))))
    val requestBody = gson.toJson(requestPayload).toRequestBody(MEDIA_TYPE_JSON)

    val request = Request.Builder()
      .url(httpUrl)
      .addHeader("Content-Type", MEDIA_TYPE_JSON.toString())
      .post(requestBody)
      .build()

    return client.newCall(request).execute().use { response ->
      if (!response.isSuccessful) {
        logger.error { "Request failed: ${response.code} - ${response.message}" }
        return ""
      }

      gson.fromJson(response.body?.string().orEmpty(), ResponsePayload::class.java)
        .candidates.firstOrNull()?.content?.parts?.firstOrNull()?.text.orEmpty()
    }
  }
}