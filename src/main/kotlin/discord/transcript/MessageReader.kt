package discord.transcript

import io.github.oshai.kotlinlogging.KotlinLogging
import java.time.OffsetDateTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.Message

private val logger = KotlinLogging.logger {}

private const val TOWN_SQUARE_CHANNEL_ID = 1164312583088640030L
private const val WHISPER_CHANNEL_ID = 1251899834361712721L

/**
 * Holds messages retrieved from Discord channels.
 */
data class ChannelMessages(
  val townSquareMsgs: List<Message>,
  val whisperMsgs: List<Message>,
) {
  private fun Message.asLogString(includeAuthor: Boolean): String {
    return if (includeAuthor) "${author.effectiveName}: " else ""
  }

  /**
   * Logs the timestamps and content of the first and last messages from both channels.
   */
  fun logFirstAndLastMessages() {
    fun logMessages(type: String, first: Message?, last: Message?, includeAuthor: Boolean) {
      if (first == null || last == null) {
        logger.warn { "Could not find first and last messages in $type channel." }
        return
      }

      logger.info {
        "First $type message: ${first.asLogString(includeAuthor = includeAuthor)}\n"
        "Last $type message: ${last.asLogString(includeAuthor = includeAuthor)}"
      }
    }

    logMessages("Town Square", townSquareMsgs.firstOrNull(), townSquareMsgs.lastOrNull(), true)
    logMessages("Whisper", whisperMsgs.firstOrNull(), whisperMsgs.lastOrNull(), false)
  }

  /**
   * Returns messages formatted with headers indicating public and private discussions.
   */
  fun contentWithHeaders(): List<String> = buildList {
    add("\nPUBLIC DISCUSSION IN TOWN SQUARE:")
    addAll(townSquareMsgs.map { "${it.timeCreated} ${it.author.effectiveName}: ${it.contentDisplay}" })
    add("\nPRIVATE DISCUSSION IN WHISPERS:")
    addAll(whisperMsgs.map { "${it.timeCreated} ${it.contentDisplay}" })
  }

  override fun toString(): String = contentWithHeaders().joinToString("\n")
}

/**
 * Reads messages from Discord channels within a specified timeframe.
 */
class MessageReader(
  private val jda: JDA,
  private val readStartTime: OffsetDateTime,
  private val readEndTime: OffsetDateTime,
  private val includeAuthor: Boolean = false,
) {

  /**
   * Reads messages from both channels concurrently.
   */
  suspend fun readMessages(): ChannelMessages = coroutineScope {
    logger.debug { "Reading messages from $readStartTime to $readEndTime" }

    val townSquareMsgs = async { fetchMessages(TOWN_SQUARE_CHANNEL_ID) }
    val whisperMsgs = async { fetchMessages(WHISPER_CHANNEL_ID) }

    ChannelMessages(townSquareMsgs.await(), whisperMsgs.await())
  }

  /**
   * Fetches messages from a Discord channel within the specified time range.
   */
  private suspend fun fetchMessages(channelId: Long): List<Message> = withContext(Dispatchers.IO) {
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