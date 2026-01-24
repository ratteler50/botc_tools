package discord.character_info

import getRolesFromJson
import models.Role
import models.Role.Type.*
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.OptionType.STRING
import net.dv8tion.jda.api.interactions.commands.build.Commands
import normalize
import wikiReader
import java.awt.Color

class RoleHandler {
  companion object {
    val roleCommand = Commands.slash(
      "role", "Search for an official Blood on the Clocktower role by name"
    ).addOption(STRING, "name", "The name of the role to look for", true)

    /**
     * Handles role search queries, finding and displaying details about a Blood on the Clocktower role.
     */
    fun handleRole(event: SlashCommandInteractionEvent) {
      event.deferReply().queue()
      val roleName = event.options[0].asString

      // Load roles from JSON and normalize their names for comparison
      val roles = getRolesFromJson().associateBy { it.id.normalize() }

      // Find the role and send an embed with its details
      roles[roleName.normalize()]?.let { role ->
        event.hook.sendMessageEmbeds(buildEmbed(role)).queue()
      } ?: run {
        event.hook.sendMessage("Role not found: $roleName").queue()
      }
    }

    /**
     * Builds an embedded message containing detailed role information.
     */
    private fun buildEmbed(role: Role) = EmbedBuilder().run {
      // Fetch URLs from wiki
      val wikiData = runCatching {
        wikiReader.getRole(role.name ?: "")
      }.getOrNull()

      setTitle(role.name, wikiData?.wikiUrl)
      setDescription(role.ability)
      setFooter(role.flavour)
      setThumbnail(wikiData?.imageUrl)

      // Set the embed color based on role type
      setColor(
        when (role.type) {
          TOWNSFOLK -> Color(0x183EFF)
          OUTSIDER -> Color(0x2096FF)
          MINION -> Color(0x9F0400)
          DEMON -> Color(0xEC0804)
          TRAVELLER -> Color(0xc519ff)
          FABLED -> Color(0xECCB21)
          LORIC -> Color(0x3f9651)
          else -> Color(0x000000)
        }
      )
      build()
    }
  }

}
