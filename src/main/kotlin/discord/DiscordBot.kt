package discord

import discord.character_info.RoleHandler
import discord.character_info.RoleHandler.Companion.roleCommand
import discord.text_script.TextScriptHandler
import discord.text_script.TextScriptHandler.Companion.textScriptCommand
import discord.transcript.TranscriptHandler
import discord.transcript.TranscriptHandler.Companion.transcriptCommand
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.runBlocking
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.entities.Activity
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import net.dv8tion.jda.api.requests.GatewayIntent.MESSAGE_CONTENT

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
    val jda = JDABuilder.createDefault(token).enableIntents(MESSAGE_CONTENT).addEventListeners(this).build()

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
