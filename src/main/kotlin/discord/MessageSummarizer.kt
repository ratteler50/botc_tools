package discord

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import discord.Settings.GEMINI_KEY
import io.github.oshai.kotlinlogging.KotlinLogging
import java.io.File
import java.util.concurrent.TimeUnit.MINUTES
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

/**
 * A class responsible for summarizing message exchanges from a game of Blood on the Clocktower
 * using Google's Gemini API.
 */
class MessageSummarizer {
  // Logger for debugging and informational logs
  private val logger = KotlinLogging.logger {}

  // HTTP client for making API requests
  private val client = OkHttpClient().newBuilder().readTimeout(1, MINUTES).build()

  // Gson instance for JSON serialization and deserialization
  private val gson: Gson = GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create()

  private companion object {
    // Base URL for the Gemini API endpoint
    const val BASE_URL =
      "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent"

    // Predefined prompt for summarization
    val BASE_PROMPT = """
  Summarize in detail the following Blood on the Clocktower exchange, explicitly separating public and private conversations and separating by game day of conversation. Ensure the summary captures:
	•	Theories & Worlds: The narratives and possibilities players are constructing about roles and alignments.
	•	Claims: Any characters that players are claiming to others.
	•	Key Accusations & Deductions: Notable logical leaps, suspicions, or alliances forming.

  Maintain clarity and conciseness while preserving all essential context. Prioritize accuracy in capturing the logical flow of discussions.
    """.trimIndent()

    // JSON media type used in HTTP requests
    val MEDIA_TYPE_JSON = "application/json".toMediaType()
  }

  /**
   * Data class representing the response structure from the Gemini API.
   * The API returns a list of candidates, each containing a content field.
   */
  data class ResponsePayload(val candidates: List<Candidate>) {
    data class Candidate(val content: Content)
  }

  /**
   * Data class representing the request payload structure sent to the API.
   */
  data class RequestPayload(val contents: List<Content>)

  /**
   * Represents message content with nested parts.
   */
  data class Content(val parts: List<Part>) {
    data class Part(val text: String)
  }

  /**
   * Summarizes a list of messages by converting them into a single text input and sending
   * it to the Gemini API.
   *
   * @param messages The list of messages to summarize.
   * @return The generated summary or an empty string if the request fails.
   */
  fun summarize(messages: List<String>): String {
    // write the output to file
    val output = messages.joinToString("\n")
    File("./data/transcript.txt").writeText(output)
    logger.warn { "DONE WRITING OUTPUT!" }
    return summarizeText(messages.joinToString("\n"))
  }

  /**
   * Constructs the API request, sends the request, and processes the response.
   *
   * @param inputText The text input to be summarized.
   * @return The summarized output text or an empty string if the request fails.
   */
  private fun summarizeText(inputText: String): String {
    // Build the API request URL with the authentication key
    val httpUrl = BASE_URL.toHttpUrl().newBuilder().addQueryParameter("key", GEMINI_KEY).build()

    // Construct the full prompt with the base instruction and input text
    val prompt = "$BASE_PROMPT\n$inputText"

    // Create the request payload in the expected JSON format
    val requestPayload = RequestPayload(listOf(Content(listOf(Content.Part(prompt)))))
    val jsonPayload = gson.toJson(requestPayload)

    // Build the HTTP request
    val request =
      Request.Builder().url(httpUrl).addHeader("Content-Type", MEDIA_TYPE_JSON.toString())
        .post(jsonPayload.toRequestBody(MEDIA_TYPE_JSON)).build()

    // Execute the API request and process the response
    return client.newCall(request).execute().use { response ->
      if (!response.isSuccessful) {
        println("Request failed: $response")
        return ""
      }

      // Parse the response body into the expected data structure
      val responseBody = response.body?.string().orEmpty()
      val responsePayload = gson.fromJson(responseBody, ResponsePayload::class.java)

      // Extract and return the summarized text
      responsePayload.candidates.firstOrNull()?.content?.parts?.firstOrNull()?.text.orEmpty()
    }
  }
}