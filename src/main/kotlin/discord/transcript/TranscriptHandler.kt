package discord.transcript

import io.github.oshai.kotlinlogging.KotlinLogging
import java.time.OffsetDateTime
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
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
      addOption(
        STRING,
        "channel_ids_with_author",
        "Comma separated list of channels to read from and append author",
        false
      )
      addOption(
        STRING,
        "channel_ids_without_author",
        "Comma separated list of channels to read from and append author",
        false
      )
    }

    suspend fun handleTranscript(event: SlashCommandInteractionEvent) {
      event.deferReply(true).queue() // Defer as ephemeral (visible only to the caller)

      val startTime = parseOffsetDateTime(event, event.getOption("start_time"))
      val endTime = parseOffsetDateTime(event, event.getOption("end_time"))
      val channelsWithAuthor = parseChannels(event, "channel_ids_with_author")
      val channelsWithoutAuthor = parseChannels(event, "channel_ids_without_author")

      val messageReader = ChannelMessages.MessageReader(event.jda, startTime, endTime)

      // In parallel process each channelWithAuthor and each channelWithoutAuthor
      val channelPayloads = coroutineScope {
        channelsWithAuthor.map { channel ->
          async { processSingleChannel(event, messageReader, channel, true) }
        } + channelsWithoutAuthor.map { channel ->
          async { processSingleChannel(event, messageReader, channel, false) }
        }
      }.mapNotNull { it.await() }

      val dmChannel = try {
        event.user.openPrivateChannel().submit().await()
      } catch (e: Exception) {
        logger.warn(e) { "Failed to open DM with ${event.user.id}" }
        null
      }


      if (dmChannel != null) {
        dmChannel.sendFiles(channelPayloads.map { it.toFilePayload() })
          .queue({ event.hook.sendMessage("Transcripts sent via DM! ✅").queue() }, {
            event.hook.sendMessage("Failed to send DM. Please check your settings. ❌").queue()
          })
      } else {
        event.hook.sendMessage("Could not open a DM with you. Make sure your DMs are enabled. ❌")
          .queue()
      }
    }


    private fun parseChannels(
      event: SlashCommandInteractionEvent,
      optionName: String,
    ): List<TextChannel> {
      val channels: List<TextChannel> = event.getOption(optionName)?.asString?.split(",")
        ?.mapNotNull { event.jda.getTextChannelById(it) } ?: emptyList()
      return channels
    }

    private data class ChannelPayload(val channel: TextChannel, val payload: String) {
      fun toFilePayload(): FileUpload =
        FileUpload.fromData(payload.toByteArray(), "${channel.name}.md")
    }

    private suspend fun processSingleChannel(
      event: SlashCommandInteractionEvent,
      messageReader: ChannelMessages.MessageReader,
      channel: TextChannel?,
      includeAuthor: Boolean,
    ): ChannelPayload? {
      if (!validateInput(event, channel)) return null
      requireNotNull(channel) { "Channel ID not found" }

      val messages = messageReader.readMessages(channel.id.toLong())

      if (messages.allMessages.isEmpty()) {
        event.hook.sendMessage("No messages found in channel $channel between ${messages.startTime} and ${messages.endTime}.")
          .queue()
        return null
      }

      val payload = messages.allMessages.joinToString("\n") { it.asLogString(includeAuthor) }
      return ChannelPayload(channel, payload)

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