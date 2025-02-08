package discord.transcript

import io.github.oshai.kotlinlogging.KotlinLogging
import java.time.OffsetDateTime
import java.time.ZoneOffset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.Message

private val logger = KotlinLogging.logger {}

/**
 * Logs the timestamps and content of the first and last messages from both channels.
 */
fun Message.asLogString(
  includeAuthor: Boolean,
  includeTimestamp: Boolean = true,
): String {
  return buildString {
    if (includeTimestamp) append("${timeCreated.withOffsetSameInstant(ZoneOffset.ofHours(-8))} ")
    if (includeAuthor) append("${author.effectiveName}: ")
    append(contentDisplay)
  }
}


/**
 * Holds messages retrieved from Discord channels.
 */
data class ChannelMessages(
  val startTime: OffsetDateTime,
  val endTime: OffsetDateTime,
  val allMessages: List<Message>,
) {

  fun logFirstAndLastMessages(includeAuthor: Boolean) {
    logMessages(
      allMessages.firstOrNull(), allMessages.lastOrNull(), includeAuthor
    )
  }

  fun logMessages(first: Message?, last: Message?, includeAuthor: Boolean) {
    if (first == null || last == null) {
      logger.warn { "Could not find first and last messages in channel." }
      return
    }
    logger.info { "First message: ${first.asLogString(includeAuthor = includeAuthor)}" }
    logger.info { "Last message: ${last.asLogString(includeAuthor = includeAuthor)}" }
  }

  /**
   * Reads messages from Discord channels within a specified timeframe.
   */
  class MessageReader(
    private val jda: JDA,
    private val readStartTime: OffsetDateTime,
    private val readEndTime: OffsetDateTime,
  ) {

    /**
     * Reads messages from the input channel
     */
    suspend fun readMessages(channelId: Long): ChannelMessages = coroutineScope {
      logger.debug { "Reading messages from $readStartTime to $readEndTime" }

      val townSquareMsgs = async { fetchMessages(channelId) }

      val messages = ChannelMessages(
        startTime = readStartTime,
        endTime = readEndTime,
        allMessages = townSquareMsgs.await()
      )

      messages
    }

    /**
     * Fetches messages from a Discord channel within the specified time range.
     */
    private suspend fun fetchMessages(channelId: Long): List<Message> =
      withContext(Dispatchers.IO) {
        logger.debug { "Fetching messages from channel $channelId." }

        val channel = jda.getTextChannelById(channelId) ?: return@withContext emptyList<Message>()
          .also { logger.warn { "Channel with ID $channelId not found." } }

        channel.iterableHistory
          .takeWhile { it.timeCreated.isAfter(readStartTime) }
          .reversed()
          .takeWhile { it.timeCreated.isBefore(readEndTime) }
          .also { logger.debug { "Fetched ${it.size} messages from channel $channelId." } }
      }
  }
}