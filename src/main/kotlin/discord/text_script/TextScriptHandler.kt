package discord.text_script

import generateTextScript
import getScriptMetadata
import io.github.oshai.kotlinlogging.KotlinLogging
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.OptionType.STRING
import net.dv8tion.jda.api.interactions.commands.build.Commands
import net.dv8tion.jda.api.utils.FileUpload

class TextScriptHandler {
  companion object {
    val textScriptCommand = Commands.slash(
      "textscript", "Generate a 5oS style text script from a JSON file"
    ).addOption(STRING, "input", "The input script in JSON format", true)
    private val logger = KotlinLogging.logger {}


    /**
     * Handles script generation from JSON input, sending the output as a file attachment.
     */
    fun handleTextScript(event: SlashCommandInteractionEvent) {
      event.deferReply().queue()
      val channel: MessageChannel = event.channel
      val scriptJson = event.options[0].asString

      try {
        // Parse script metadata and generate the script text
        val scriptMetadata = getScriptMetadata(scriptJson)
        val output = generateTextScript(scriptMetadata, scriptJson)

        // Send the generated script as a file attachment
        channel.sendFiles(
          FileUpload.fromData(
            output.toByteArray(), "${scriptMetadata?.name ?: "output"}.md"
          )
        ).queue()
        event.hook.sendMessage("`$scriptJson`").queue()
      } catch (e: Exception) {
        logger.warn(e) { "Error generating script from $scriptJson" }
        event.hook.sendMessage("Invalid JSON: $scriptJson").queue()
      }
    }
  }
}