package discord

import io.github.oshai.kotlinlogging.KotlinLogging
import java.time.OffsetDateTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.Message

// Logger for debugging and informational logs
private val logger = KotlinLogging.logger {}

// Discord channel IDs for different types of messages
private const val TOWN_SQUARE_CHANNEL_ID = 1164312583088640030
private const val WHISPER_CHANNEL_ID = 1251899834361712721

/**
 * Data class representing messages retrieved from specific Discord channels.
 * @property townSquareMsgs List of messages from the town square channel.
 * @property whisperMsgs List of messages from the whisper channel.
 */
data class ChannelMessages(
  val townSquareMsgs: List<Message>,
  val whisperMsgs: List<Message>,
) {

  /**
   * Logs the timestamps and content of the first and last messages from both channels.
   */
  fun logFirstAndLastMessages() {
    val firstTownSquareMessage = townSquareMsgs.firstOrNull()
    val lastTownSquareMessage = townSquareMsgs.lastOrNull()
    val firstWhisperMessage = whisperMsgs.firstOrNull()
    val lastWhisperMessage = whisperMsgs.lastOrNull()

    // Check if messages exist before logging
    if (firstTownSquareMessage == null || lastTownSquareMessage == null ||
      firstWhisperMessage == null || lastWhisperMessage == null
    ) {
      logger.warn { "Could not find first and last messages in one or both channels." }
      return
    }

    logger.info { "First Town Square message: ${firstTownSquareMessage.timeCreated} - ${firstTownSquareMessage.author.effectiveName}${firstTownSquareMessage.contentDisplay}" }
    logger.info { "Last Town Square message: ${lastTownSquareMessage.timeCreated} - ${firstTownSquareMessage.author.effectiveName} ${lastTownSquareMessage.contentDisplay}" }
    logger.info { "First whisper message: ${firstWhisperMessage.timeCreated} - ${firstWhisperMessage.contentDisplay}" }
    logger.info { "Last whisper message: ${lastWhisperMessage.timeCreated} - ${lastWhisperMessage.contentDisplay}" }
  }

  /**
   * Generates a formatted list of messages with headers indicating public (town square)
   * and private (whisper) discussions.
   * @return A list of formatted strings representing the messages.
   */
  fun contentWithHeaders(): List<String> {
    return buildList {
      add("\nPUBLIC DISCUSSION IN TOWN SQUARE:")
      addAll(townSquareMsgs.map { "${it.timeCreated} Message from ${it.author.effectiveName}: ${it.contentDisplay}" })
      add("\nPRIVATE DISCUSSION IN WHISPERS:")
      addAll(whisperMsgs.map { "${it.timeCreated} ${it.contentDisplay}" })
    }
  }

  /**
   * Converts the collected messages into a formatted string.
   * @return A single string representation of all messages with headers.
   */
  override fun toString(): String = contentWithHeaders().joinToString("\n")
}

/**
 * Handles retrieving messages from Discord channels within a specified timeframe.
 *
 * @property jda The JDA (Java Discord API) instance used to interact with Discord.
 * @property readStartTime The start time for filtering messages.
 * @property readEndTime The end time for filtering messages.
 */
class MessageReader(
  private val jda: JDA,
  private val readStartTime: OffsetDateTime,
  private val readEndTime: OffsetDateTime,
) {

  /**
   * Reads messages from both the town square and whisper channels concurrently.
   * @return A [ChannelMessages] instance containing retrieved messages.
   */
  suspend fun readMessages(): ChannelMessages = coroutineScope {
    logger.debug { "Reading messages from $readStartTime to $readEndTime" }

    // Launch concurrent message retrieval for both channels
    val townSquareDeferred = async { fetchMessages(TOWN_SQUARE_CHANNEL_ID) }
    val whisperDeferred = async { fetchMessages(WHISPER_CHANNEL_ID) }

    // Await results and construct the ChannelMessages object
    ChannelMessages(townSquareDeferred.await(), whisperDeferred.await())
  }

  /**
   * Retrieves messages from a specified channel that fall within the defined time range.
   *
   * @param channelId The ID of the channel to read messages from.
   * @return A list of messages from the specified channel.
   */
  private suspend fun fetchMessages(channelId: Long): List<Message> = withContext(Dispatchers.IO) {
    logger.debug { "Fetching messages from channel $channelId." }

    // Retrieve the Discord text channel by ID
    val channel = jda.getTextChannelById(channelId) ?: run {
      logger.warn { "Channel with ID $channelId not found." }
      return@withContext emptyList()
    }

    // Fetch messages, filtering within the time range
    val messages = channel.iterableHistory
      .takeWhile { it.timeCreated.isAfter(readStartTime) }
      .reversed()
      .takeWhile { it.timeCreated.isBefore(readEndTime) }

    logger.debug { "Fetched ${messages.size} messages from channel $channelId." }

    messages
  }
}