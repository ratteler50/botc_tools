package discord.ai

import com.anthropic.client.AnthropicClientAsync
import com.anthropic.client.okhttp.AnthropicOkHttpClientAsync
import com.anthropic.models.ContentBlock
import com.anthropic.models.MessageCreateParams
import com.anthropic.models.Model
import discord.Settings
import kotlinx.coroutines.future.await
import kotlinx.coroutines.runBlocking

val client: AnthropicClientAsync =
  AnthropicOkHttpClientAsync.builder() // Configures using the `ANTHROPIC_API_KEY` and `ANTHROPIC_AUTH_TOKEN` environment variables
    .fromEnv()
    .apiKey(Settings.ANTHROPIC_AI_KEY)
    .build()


suspend fun fetchMessage(client: AnthropicClientAsync, params: MessageCreateParams): List<ContentBlock> {
  val message = client.messages().create(params).await()
  return message.content()
}

fun main() {
  val params = MessageCreateParams.builder()
    .maxTokens(1024L)
    .addUserMessage("Hello, Claude")
    .model(Model.CLAUDE_3_5_HAIKU_LATEST)
    .build()

  runBlocking {
    val messageContent = fetchMessage(client, params)
    println(messageContent)
  }
}