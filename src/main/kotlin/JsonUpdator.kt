import AppConfig.GRIM_TOOL_ROLES
import AppConfig.JINXES_JSON
import AppConfig.NIGHTSHEET_JSON
import AppConfig.ROLES_JSON
import AppConfig.SCRIPT_TOOL_ROLES
import java.io.File
import kotlin.system.measureTimeMillis
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import models.Jinx
import models.NightSheet
import models.Role
import models.ScriptToolRole

suspend fun main() {
  measureTimeMillis { updateRoleJinxes() }.also { println("Updated role jinxes in $it ms") }
  measureTimeMillis { updateRolesFromGrimToolRoles() }.also { println("Updated roles in $it ms") }
  measureTimeMillis { updateRolesFromWiki() }.also { println("Updated roles from wiki in $it ms") }
  measureTimeMillis { updateNightOrder() }.also { println("Updated night order in $it ms") }
  measureTimeMillis { updateSaoFromScriptToolRoles() }.also { println("Updated SAO in $it ms") }
}

private fun updateRoleJinxes() {
  val jinxes =
    Jinx.listFromJson(gson, File(JINXES_JSON).readText()).groupBy { it.role1.normalize() }
  val updatedRoles = getRolesFromJson().map { role ->
    jinxes[role.id.normalize()]?.map { Role.Jinx(it.role2.normalize(), it.reason) }
      ?.let { role.copy(jinxes = it) } ?: role
  }

  File(ROLES_JSON).writeText(gson.toJson(updatedRoles))
}


private fun updateRolesFromGrimToolRoles() {
  val rawRoles = Role.listFromJson(gson, File(GRIM_TOOL_ROLES).readText()).associateBy(Role::id)
  val roles = getRolesFromJson()
  roles.map { role -> rawRoles[role.id]?.let { rawRole -> role.copyFrom(rawRole) } ?: role }
    .run {
      File(ROLES_JSON).writeText(gson.toJson(this))
    }
}

private fun Role.copyFrom(otherRole: Role): Role = copy(
  id = otherRole.id,
  name = otherRole.name,
  ability = otherRole.ability,
  edition = otherRole.edition,
  type = otherRole.type,
  setup = otherRole.setup.takeIf { it != false },
  firstNightReminder = otherRole.firstNightReminder?.takeUnless { it.isBlank() },
  otherNightReminder = otherRole.otherNightReminder?.takeUnless { it.isBlank() },
  reminders = otherRole.reminders?.takeUnless { it.isEmpty() },
  jinxes = otherRole.jinxes?.takeUnless { it.isEmpty() },
)

private fun Role.copyFrom(wikiRole: BotcRoleLoader.RoleResult): Role = copy(
  name = wikiRole.title,
  ability = wikiRole.roleContent.abilityText.takeIf { it.isNotBlank() },
  flavour = wikiRole.roleContent.flavourText.takeIf { it.isNotBlank() },
  urls = urls?.copy(wiki = wikiRole.wikiUrl, icon = wikiRole.imageUrl)
    ?: Role.Urls(wiki = wikiRole.wikiUrl, icon = wikiRole.imageUrl),
)

private suspend fun updateRolesFromWiki() {
  val roles = getRolesFromJson()
  withContext(Dispatchers.IO) {
    val updatedRoles = roles.map { role ->
      async {
        var updatedRole: Role
        val elapsedTimeMillis = measureTimeMillis {
          println("Updating ${role.name}")
          updatedRole =
            runCatching { role.copyFrom(wikiReader.getRole(role.name ?: "")) }.getOrElse {
              println("Couldn't update ${role.name}: ${it.message}")
              role
            }
        }
        println("Update complete for ${role.name}: $elapsedTimeMillis ms")
        updatedRole
      }
    }.awaitAll()
    File(ROLES_JSON).writeText(gson.toJson(updatedRoles))
  }
}

private fun updateNightOrder() {
  val roles = getRolesFromJson()
  val nightSheet = NightSheet.fromJson(gson, File(NIGHTSHEET_JSON).readText())

  val updatedRoles = roles.map { role ->
    role.copy(
      firstNight = updatedNightOrder(role, nightSheet.firstNight, role.firstNightReminder != null),
      otherNight = updatedNightOrder(role, nightSheet.otherNight, role.otherNightReminder != null)
    )
  }

  File(ROLES_JSON).writeText(gson.toJson(updatedRoles))
}


private fun updatedNightOrder(
  role: Role,
  nightList: List<String>,
  hasNightReminder: Boolean,
): Int? {
  val index = nightList.indexOfFirst { it.normalize() == role.id.normalize() }
  return when {
    role.id == "demoninfo" -> {
      val demonIndex = nightList.indexOf("DEMON")
      if (demonIndex == -1) null else demonIndex + 2
    }

    role.id == "minioninfo" -> {
      val minionIndex = nightList.indexOf("MINION")
      if (minionIndex == -1) null else minionIndex + 2
    }

    index == -1 -> if ((role.type == Role.Type.TRAVELLER || role.type == Role.Type.FABLED) && hasNightReminder) 2 else null
    role.id == "dusk" -> index + 1
    else -> index + 2
  }
}


private fun updateSaoFromScriptToolRoles() {
  val rolesSortedBySao = ScriptToolRole.listFromJson(gson, File(SCRIPT_TOOL_ROLES).readText())
    .filter { it.version != ScriptToolRole.Version.EXTRAS }.map { it.id.normalize() }

  val updatedRoles = getRolesFromJson().map { role ->
    val index = rolesSortedBySao.indexOf(role.id.normalize())
    if (index == -1) role.copy(standardAmyOrder = null) else role.copy(standardAmyOrder = index + 1)
  }
    .sortedWith(compareBy<Role> { it.edition == Role.Edition.SPECIAL }.thenBy(nullsLast()) { it.standardAmyOrder }
                  .thenBy { it.type }.thenBy { it.edition }.thenBy { it.name })

  File(ROLES_JSON).writeText(gson.toJson(updatedRoles))
}
