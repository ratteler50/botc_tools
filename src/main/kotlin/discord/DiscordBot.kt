package discord

import generateTextScript
import getScriptMetadata
import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import net.dv8tion.jda.api.interactions.commands.OptionType.STRING
import net.dv8tion.jda.api.interactions.commands.build.Commands
import net.dv8tion.jda.api.utils.FileUpload

class DiscordBot(private val token: String) : ListenerAdapter() {

  fun start() {
    val jda = JDABuilder.createDefault(token).addEventListeners(this).build()
    jda.upsertCommand(
      Commands.slash(
        "textscript", "Generate a 5oS style text script from a JSON file"
      ).addOption(STRING, "input", "The input script in JSON format", true)
    ).queue()
    jda.awaitReady()
    println("Bot is ready!")
  }

  override fun onSlashCommandInteraction(event: SlashCommandInteractionEvent) {
    if (event.name == "textscript") {
      handleTextScript(event)
    }
  }

  private fun handleTextScript(event: SlashCommandInteractionEvent) {
    event.deferReply().queue()
    val channel: MessageChannel = event.channel
    val scriptJson = event.options[0].asString
    try {
      val scriptMetadata = getScriptMetadata(scriptJson)
      val output = generateTextScript(scriptMetadata, scriptJson)

      // Send the InputStream as an attachment
      channel.sendFiles(
        FileUpload.fromData(
          output.toByteArray(), "${scriptMetadata?.name ?: "output"}.md"
        )
      ).queue()
      event.hook.sendMessage(scriptJson).queue()
    } catch (e: Exception) {
      event.hook.sendMessage("Invalid JSON: $scriptJson").queue()
    }
  }
}