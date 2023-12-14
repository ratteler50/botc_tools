package discord

import generateTextScript
import getScriptMetadata
import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import net.dv8tion.jda.api.utils.FileUpload

class DiscordBot(private val token: String) : ListenerAdapter() {

  fun start() {
    val jda = JDABuilder.createDefault(token)
      .addEventListeners(this)
      .build()
    jda.awaitReady()
    println("Bot is ready!")
  }

  override fun onMessageReceived(event: MessageReceivedEvent) {
    val message: Message = event.message
    val content: String = message.contentDisplay
    val channel: MessageChannel = event.channel
    val user: User = event.author

    if (user.isBot) return

    if (content.startsWith("!generateScript")) {
      val json = content.substringAfter(" ")
      val scriptMetadata = getScriptMetadata(json)
      val output = generateTextScript(scriptMetadata, json)

      // Send the InputStream as an attachment
      channel.sendFiles(
        FileUpload.fromData(
          output.toByteArray(),
          "${scriptMetadata?.name ?: "output"}.md"
        )
      ).queue()
    }
  }
}