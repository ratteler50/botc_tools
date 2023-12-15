package discord

import generateTextScript
import getRolesFromJson
import getScriptMetadata
import java.awt.Color
import models.Role
import models.Role.Type.DEMON
import models.Role.Type.FABLED
import models.Role.Type.MINION
import models.Role.Type.OUTSIDER
import models.Role.Type.TOWNSFOLK
import models.Role.Type.TRAVELLER
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import net.dv8tion.jda.api.interactions.commands.OptionType.STRING
import net.dv8tion.jda.api.interactions.commands.build.Commands
import net.dv8tion.jda.api.utils.FileUpload
import normalize

class DiscordBot(private val token: String) : ListenerAdapter() {

  private val roles by lazy { getRolesFromJson().associateBy { it.id.normalize() } }

  fun start() {
    val jda = JDABuilder.createDefault(token).addEventListeners(this).build()
    jda.upsertCommand(
      Commands.slash(
        "textscript", "Generate a 5oS style text script from a JSON file"
      ).addOption(STRING, "input", "The input script in JSON format", true)
    ).queue()
    jda.upsertCommand(
      Commands.slash(
        "role", "Search for an official Blood on the Clocktower role by name"
      ).addOption(STRING, "name", "The name of the role to look for", true)
    ).queue()
    jda.retrieveCommands().queue { commands ->
      commands.forEach { println("name: ${it.name}; id: ${it.id}") }
    }
    jda.awaitReady()
    println("Bot is ready!")
  }

  override fun onSlashCommandInteraction(event: SlashCommandInteractionEvent) {
    when (event.name) {
      "role" -> handleRole(event)
      "textscript" -> handleTextScript(event)
    }
  }


  private fun handleRole(event: SlashCommandInteractionEvent) {
    event.deferReply().queue()
    val roleName = event.options[0].asString
    roles[roleName.normalize()]?.let { role ->
      event.hook.sendMessageEmbeds(buildEmbed(role)).queue()
    } ?: run {
      event.hook.sendMessage("Role not found: $roleName").queue()
    }
  }

  private fun buildEmbed(role: Role) = EmbedBuilder().run {
    setTitle(role.name, role.urls?.wiki)
    setDescription(role.ability)
    setFooter(role.flavour)
    setThumbnail(role.urls?.icon)
    setColor(
      when (role.type) {
        TOWNSFOLK -> Color(0x2096FF)
        OUTSIDER -> Color(0x183EFF)
        MINION -> Color(0x9F0400)
        DEMON -> Color(0xEC0804)
        TRAVELLER -> Color(0xc519ff)
        FABLED -> Color(0xECCB21)
        else -> Color(0x000000)
      }
    )
    build()
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
      event.hook.sendMessage("`$scriptJson`").queue()
    } catch (e: Exception) {
      event.hook.sendMessage("Invalid JSON: $scriptJson").queue()
    }
  }
}