package discord

import io.github.oshai.kotlinlogging.KotlinLogging
import java.time.OffsetDateTime
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.Message

private val logger = KotlinLogging.logger {}
private const val channelId = 1251899834361712721
private val readStartTime: OffsetDateTime = OffsetDateTime.parse("2025-02-06T19:00:00-08:00")
private val readEndTime: OffsetDateTime = OffsetDateTime.parse("2025-02-06T19:30:00-08:00")

class MessageReader(val jda: JDA) {
  fun readMessages() {
    readChannel().forEach {
      logger.info { "${it.timeCreated}: ${it.contentDisplay}" }
    }
  }


  private fun readChannel(): List<Message> {
    logger.info { "Reading channel $channelId at $readStartTime." }

    // throw if null
    val channel = jda.getTextChannelById(channelId)!!

    return channel.iterableHistory.takeWhile { it.timeCreated.isAfter(readStartTime) }
      .reversed().takeWhile { it.timeCreated.isBefore(readEndTime) }
  }
}