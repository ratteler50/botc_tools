package discord.transcript

import io.github.oshai.kotlinlogging.KotlinLogging
import java.time.OffsetDateTime
import kotlinx.coroutines.future.await
import net.dv8tion.jda.api.Permission.VIEW_CHANNEL
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.OptionMapping
import net.dv8tion.jda.api.interactions.commands.OptionType.STRING
import net.dv8tion.jda.api.interactions.commands.build.Commands
import net.dv8tion.jda.api.utils.FileUpload

/**
 * Handles the /transcript command, which retrieves messages from a channel within a specified time range.
 */
class TranscriptHandler {
  companion object {
    private val logger = KotlinLogging.logger {}
    private val allowlistedUsers = setOf("258476947779092480") // Dae

    val transcriptCommand = Commands.slash(
      "transcript", "Returns the contents of the input channel between start and end timestamps"
    ).apply {
      addOption(STRING, "channel_ids", "Comma separated list of channels to read from", true)
      addOption(
        STRING,
        "include_authors",
        "Include message author in logs.  Comma separated list of t/f that must be the same length as channel_ids",
        true
      )
      addOption(
        STRING,
        "start_time",
        "Start time (OffsetDateTime format: `2024-12-31T15:00:00-08:00`)",
        true
      )
      addOption(
        STRING,
        "end_time",
        "End time (Optional, OffsetDateTime format: `2024-12-31T15:00:00-08:00`)",
        false
      )
    }

    suspend fun handleTranscript(event: SlashCommandInteractionEvent) {
      event.deferReply().queue()


      val channels= parseChannels(event)
      val channel = channels.firstOrNull()
      val includeAuthor = parseAuthorList(event)
      val startTime = parseOffsetDateTime(event, event.getOption("start_time"))
      val endTime = parseOffsetDateTime(event, event.getOption("end_time"))


      val messageReader = ChannelMessages.MessageReader(event.jda, startTime, endTime)
      val channelPayload = processSingleChannel(event, messageReader, channel, includeAuthor)
      channelPayload.let { payload ->
        if (payload != null) {
          requireNotNull(channel) { "Channel ID not found" }
          event.channel.sendFiles(
            FileUpload.fromData(payload.toByteArray(), "${channel.name}.md")
          ).queue()
          event.hook.sendMessage("done").queue()
        }
      }
    }

    private fun parseAuthorList(event: SlashCommandInteractionEvent) =
      event.getOption("include_author")?.asBoolean ?: false

    private fun parseChannels(event: SlashCommandInteractionEvent): List<TextChannel> {
      val channel: List<TextChannel> =
        event.getOption("channel_ids")?.asString?.split(",")
          ?.mapNotNull { event.jda.getTextChannelById(it) } ?: emptyList()
      return channel
    }

    private suspend fun processSingleChannel(
      event: SlashCommandInteractionEvent,
      messageReader: ChannelMessages.MessageReader,
      channel: TextChannel?,
      includeAuthor: Boolean,
    ): String? {
      if (!validateInput(event, channel)) return null
      requireNotNull(channel) { "Channel ID not found" }

      val messages = messageReader.readMessages(channel.id.toLong())

      if (messages.allMessages.isEmpty()) {
        event.hook.sendMessage("No messages found in channel $channel between ${messages.startTime} and ${messages.endTime}.")
          .queue()
        return null
      }

      val payload = messages.allMessages.joinToString("\n") { it.asLogString(includeAuthor) }
      logger.warn { "Payload: $payload" }
      return payload

    }


    private suspend fun validateInput(
      event: SlashCommandInteractionEvent,
      channel: TextChannel?,
    ): Boolean {
      if (channel == null) {
        event.hook.sendMessage("Invalid channel. Check channel ID.").queue()
        return false
      }

      if (event.user.id !in allowlistedUsers) {
        event.hook.sendMessage("You lack permission to use this command.").queue()
        return false
      }

      if (!hasReadAccess(channel, event.user)) {
        event.hook.sendMessage("No permission to view channel ${channel.id}.").queue()
        return false
      }
      return true
    }

    private suspend fun hasReadAccess(channel: TextChannel, user: User): Boolean {
      val member = channel.guild.retrieveMemberById(user.id).submit().await()
      return member?.hasPermission(channel, VIEW_CHANNEL) ?: false
    }

    private fun parseOffsetDateTime(
      event: SlashCommandInteractionEvent,
      input: OptionMapping?,
    ): OffsetDateTime {
      return input?.asString?.let {
        try {
          OffsetDateTime.parse(it)
        } catch (e: Exception) {
          logger.warn(e) { "Invalid timestamp format: $it" }
          event.hook.sendMessage("Invalid timestamp format: $it").queue()
          OffsetDateTime.now()
        }
      } ?: OffsetDateTime.now()
    }
  }
}
