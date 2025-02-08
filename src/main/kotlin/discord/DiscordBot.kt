package discord

import discord.character_info.RoleHandler
import discord.character_info.RoleHandler.Companion.roleCommand
import discord.text_script.TextScriptHandler
import discord.text_script.TextScriptHandler.Companion.textScriptCommand
import discord.transcript.ChannelMessages
import discord.transcript.TranscriptHandler
import discord.transcript.TranscriptHandler.Companion.transcriptCommand
import discord.transcript.asLogString
import io.github.oshai.kotlinlogging.KotlinLogging
import java.time.OffsetDateTime
import kotlinx.coroutines.runBlocking
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.entities.Activity
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter

private val logger = KotlinLogging.logger {}

/**
 * A Discord bot that listens for slash commands and interacts with Blood on the Clocktower scripts.
 */
class DiscordBot(private val token: String) : ListenerAdapter() {

  /**
   * Starts the bot, initializing its setup and summarizing messages asynchronously.
   */
  fun start() {
    val jda = setupBot()
    jda.awaitReady()
  }

  /**
   * Configures the bot with event listeners, slash commands, and activity presence.
   */
  private fun setupBot(): JDA {
    val jda = JDABuilder.createDefault(token).addEventListeners(this).build()

    jda.updateCommands().addCommands(textScriptCommand, roleCommand, transcriptCommand).queue()

    // Log registered commands
    jda.retrieveCommands().queue { commands ->
      commands.forEach {
        logger.info { "name: ${it.name}; id: ${it.id}" }
      }
    }

    // Wait for the bot to become ready and set its activity status
    jda.awaitReady()
    jda.presence.activity = Activity.playing("with her string!")
    logger.info { "Bot is ready!" }
    return jda
  }

  /**
   * Reads and summarizes messages within a given time range.
   */
  private suspend fun summarizeMessages(jda: JDA): String {
    val readStartTime = OffsetDateTime.parse("2025-02-07T00:00:00-08:00")
    val readEndTime = OffsetDateTime.now()


    val TOWN_SQUARE_CHANNEL_ID = 1164312583088640030L
    val WHISPER_CHANNEL_ID = 1251899834361712721L


    // Retrieve messages using MessageReader
    val townSquare = ChannelMessages.MessageReader(jda, readStartTime, readEndTime)
      .readMessages(TOWN_SQUARE_CHANNEL_ID)
    val whisper = ChannelMessages.MessageReader(jda, readStartTime, readEndTime)
      .readMessages(WHISPER_CHANNEL_ID)

    logger.info { "TOWN SQUARE" }
    townSquare.logFirstAndLastMessages(true)
    logger.info { "WHISPERS" }
    whisper.logFirstAndLastMessages(false)

    townSquare.allMessages.map { it.asLogString(true) }

    // Summarize messages into a structured format
    return MessageSummarizer().summarize(townSquare.allMessages.map { it.asLogString(true) })
  }

  /**
   * Handles slash command interactions from Discord users.
   */
  override fun onSlashCommandInteraction(event: SlashCommandInteractionEvent) {
    when (event.name) {
      "role" -> RoleHandler.handleRole(event)
      "textscript" -> TextScriptHandler.handleTextScript(event)
      "transcript" -> runBlocking { TranscriptHandler.handleTranscript(event) }
    }
  }
}
