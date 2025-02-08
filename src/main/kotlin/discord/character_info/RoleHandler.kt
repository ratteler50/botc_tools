package discord.character_info

import getRolesFromJson
import java.awt.Color
import models.Role
import models.Role.Type.DEMON
import models.Role.Type.FABLED
import models.Role.Type.MINION
import models.Role.Type.OUTSIDER
import models.Role.Type.TOWNSFOLK
import models.Role.Type.TRAVELLER
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.OptionType.STRING
import net.dv8tion.jda.api.interactions.commands.build.Commands
import normalize

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
      setTitle(role.name, role.urls?.wiki)
      setDescription(role.ability)
      setFooter(role.flavour)
      setThumbnail(role.urls?.icon)

      // Set the embed color based on role type
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
  }

}
