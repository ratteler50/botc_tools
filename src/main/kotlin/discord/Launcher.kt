package discord

import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType
import kotlinx.cli.required

object Launcher {
  @JvmStatic
  fun main(args: Array<String>) {
    val parser = ArgParser("discordBot")
    val botTokens by parser.option(
      ArgType.String,
      shortName = "t",
      description = "The bot tokens, separated by comma"
    ).required()

    parser.parse(args)

    val tokens = botTokens.split(",")

    tokens.forEach { token ->
      val bot = DiscordBot(token.trim())
      bot.start()
    }
  }
}