package discord

object Launcher {
    @JvmStatic
    fun main(args: Array<String>) {
        val bot = DiscordBot(Settings.BOT_TOKEN)
        bot.start()
    }
}