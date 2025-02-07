package discord

import generateTextScript
import getRolesFromJson
import getScriptMetadata
import io.github.oshai.kotlinlogging.KotlinLogging
import java.awt.Color
import java.time.OffsetDateTime
import kotlinx.coroutines.runBlocking
import models.Role
import models.Role.Type.DEMON
import models.Role.Type.FABLED
import models.Role.Type.MINION
import models.Role.Type.OUTSIDER
import models.Role.Type.TOWNSFOLK
import models.Role.Type.TRAVELLER
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.entities.Activity
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import net.dv8tion.jda.api.interactions.commands.OptionType.STRING
import net.dv8tion.jda.api.interactions.commands.build.Commands
import net.dv8tion.jda.api.utils.FileUpload
import normalize

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

    // Run message summarization in a blocking coroutine
    val summarizedMessages = runBlocking { summarizeMessages(jda) }
    logger.info { "Summarized messages: $summarizedMessages" }
  }

  /**
   * Configures the bot with event listeners, slash commands, and activity presence.
   */
  private fun setupBot(): JDA {
    val jda = JDABuilder.createDefault(token).addEventListeners(this).build()

    // Register slash commands for generating scripts and searching roles
    jda.upsertCommand(
      Commands.slash(
        "textscript", "Generate a 5oS style text script from a JSON file"
      ).addOption(STRING, "input", "The input script in JSON format", true)
    ).queue()

    jda.upsertCommand(
      Commands.slash(
        "role", "Search for an official Blood on the Clocktower role by name"
      ).addOption(STRING, "name", "The name of the role to look for", true)
    ).queue()

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
    val readStartTime = OffsetDateTime.parse("2025-02-05T07:00:00-08:00")
    val readEndTime = OffsetDateTime.now()

    // Retrieve messages using MessageReader
    val messages = MessageReader(
      jda, readStartTime, readEndTime
    ).readMessages()

    messages.logFirstAndLastMessages()

    // Summarize messages into a structured format
    return MessageSummarizer().summarize(messages.contentWithHeaders())
  }

  /**
   * Handles slash command interactions from Discord users.
   */
  override fun onSlashCommandInteraction(event: SlashCommandInteractionEvent) {
    when (event.name) {
      "role" -> handleRole(event)
      "textscript" -> handleTextScript(event)
    }
  }

  /**
   * Handles role search queries, finding and displaying details about a Blood on the Clocktower role.
   */
  private fun handleRole(event: SlashCommandInteractionEvent) {
    event.deferReply().queue()
    val roleName = event.options[0].asString

    // Load roles from JSON and normalize their names for comparison
    val roles = getRolesFromJson().associateBy { it.id.normalize() }

    // Find the role and send an embed with its details
    roles[roleName.normalize()]?.let { role ->
      event.hook.sendMessageEmbeds(buildEmbed(role)).queue()
    } ?: run {
      event.hook.sendMessage("Role not found: $roleName").queue()
    }
  }

  /**
   * Builds an embedded message containing detailed role information.
   */
  private fun buildEmbed(role: Role) = EmbedBuilder().run {
    setTitle(role.name, role.urls?.wiki)
    setDescription(role.ability)
    setFooter(role.flavour)
    setThumbnail(role.urls?.icon)

    // Set the embed color based on role type
    setColor(
      when (role.type) {
        TOWNSFOLK -> Color(0x2096FF)
        OUTSIDER -> Color(0x183EFF)
        MINION -> Color(0x9F0400)
        DEMON -> Color(0xEC0804)
        TRAVELLER -> Color(0xc519ff)
        FABLED -> Color(0xECCB21)
        else -> Color(0x000000)
      }
    )
    build()
  }

  /**
   * Handles script generation from JSON input, sending the output as a file attachment.
   */
  private fun handleTextScript(event: SlashCommandInteractionEvent) {
    event.deferReply().queue()
    val channel: MessageChannel = event.channel
    val scriptJson = event.options[0].asString

    try {
      // Parse script metadata and generate the script text
      val scriptMetadata = getScriptMetadata(scriptJson)
      val output = generateTextScript(scriptMetadata, scriptJson)

      // Send the generated script as a file attachment
      channel.sendFiles(
        FileUpload.fromData(
          output.toByteArray(), "${scriptMetadata?.name ?: "output"}.md"
        )
      ).queue()
      event.hook.sendMessage("`$scriptJson`").queue()
    } catch (e: Exception) {
      logger.warn(e) { "Error generating script from $scriptJson" }
      event.hook.sendMessage("Invalid JSON: $scriptJson").queue()
    }
  }
}
