package discord.transcript

import io.github.oshai.kotlinlogging.KotlinLogging
import java.time.OffsetDateTime
import kotlinx.coroutines.future.await
import kotlinx.coroutines.runBlocking
import net.dv8tion.jda.api.Permission.VIEW_CHANNEL
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.OptionMapping
import net.dv8tion.jda.api.interactions.commands.OptionType.BOOLEAN
import net.dv8tion.jda.api.interactions.commands.OptionType.STRING
import net.dv8tion.jda.api.interactions.commands.build.Commands

class TranscriptHandler {
  companion object {
    private val logger = KotlinLogging.logger {}
    private val allowlistedUsers = setOf(
      "258476947779092480" // Dae
    )

    val transcriptCommand = Commands.slash(
      "transcript", "Returns the contents of the input channel between start and end timestamp"
    ).addOption(STRING, "channel_id", "The channel to read from", true).addOption(
      STRING,
      "start_time",
      "The start time to read from in the form of an OffsetDatetime like`2024-12-31T15:00:00-08:00`",
      true
    ).addOption(
      STRING,
      "end_time",
      "The end time to read till in the form of an OffsetDatetime like`2024-12-31T15:00:00-08:00`",
      false
    ).addOption(BOOLEAN, "include_author", "Include message author name in logs", false)

    fun handleTranscript(event: SlashCommandInteractionEvent) {
      event.deferReply().queue()
      val channelId = event.getOption("channel_id")?.asString
      val channel = channelId?.let { event.jda.getTextChannelById(it) }
      val startTime = parseOffsetDateTimeOrNow(event, event.getOption("start_time"))
      val endTime = parseOffsetDateTimeOrNow(event, event.getOption("end_time"))
      val includeAuthor = event.getOption("include_author")?.asBoolean ?: false

      validateInput(event, channel, startTime, endTime, includeAuthor)

    }

    private fun validateInput(
      event: SlashCommandInteractionEvent,
      channel: TextChannel?,
      startTime: OffsetDateTime,
      endTime: OffsetDateTime,
      includeAuthor: Boolean,
    ) {
      event.hook.sendMessage("Input received: $channel, $startTime, $endTime, $includeAuthor")
        .queue()

      if (channel == null || startTime.isAfter(endTime)) {
        event.hook.sendMessage("Invalid input. Please check your parameters.").queue()
        return
      }

      event.hook.sendMessage("You are ${event.user}").queue()
      if (!allowlistedUsers.contains(event.user.id)) {
        event.hook.sendMessage("User ${event.user} does not have permission to use this command")
          .queue()
        return
      }
      if (!runBlocking { hasReadAccess(channel, event.user) }) {
        event.hook.sendMessage(
          "User ${event.user} does not have permission to view contents of channel ${channel.id}"
        ).queue()
        return
      }
    }

    private suspend fun hasReadAccess(channel: TextChannel, user: User): Boolean {
      val member = channel.guild.retrieveMemberById(user.id).submit().await()
      logger.info { "Checking read access for $member in $channel" }
      return member?.hasPermission(channel, VIEW_CHANNEL) ?: false
    }

    private fun parseOffsetDateTimeOrNow(
      event: SlashCommandInteractionEvent,
      input: OptionMapping?,
    ): OffsetDateTime {
      if (input == null) {
        logger.warn { "No timestamp provided, defaulting to now" }
        return OffsetDateTime.now()
      }
      return try {
        OffsetDateTime.parse(input.asString)
      } catch (e: Exception) {
        logger.warn(e) { "Error parsing timestamp: $input" }
        event.hook.sendMessage("Invalid timestamp: $input").queue()
        return OffsetDateTime.now()
      }
    }


    // fun handleTextScript(event: SlashCommandInteractionEvent) {
    //   event.deferReply().queue()
    //   val channel: MessageChannel = event.channel
    //   val scriptJson = event.options[0].asString
    //
    //   try {
    //     // Parse script metadata and generate the script text
    //     val scriptMetadata = getScriptMetadata(scriptJson)
    //     val output = generateTextScript(scriptMetadata, scriptJson)
    //
    //     // Send the generated script as a file attachment
    //     channel.sendFiles(
    //       FileUpload.fromData(
    //         output.toByteArray(), "${scriptMetadata?.name ?: "output"}.md"
    //       )
    //     ).queue()
    //     event.hook.sendMessage("`$scriptJson`").queue()
    //   } catch (e: Exception) {
    //     TextScriptHandler.logger.warn(e) { "Error generating script from $scriptJson" }
    //     event.hook.sendMessage("Invalid JSON: $scriptJson").queue()
    //   }
    // }
  }

}
