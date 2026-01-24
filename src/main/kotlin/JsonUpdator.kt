
import AppConfig.DATA_JSON
import AppConfig.ROLES_JSON
import AppConfig.SAO_JSON
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import models.DataJson
import models.Role
import java.io.File
import kotlin.system.measureTimeMillis

private val logger = KotlinLogging.logger {}

suspend fun main() {
  measureTimeMillis { updateRolesFromData() }.also { logger.info { "Updated roles in $it ms" } }
  measureTimeMillis { updateRolesFromWiki() }.also { logger.info { "Updated roles from wiki in $it ms" } }
  measureTimeMillis { updateNightOrder() }.also { logger.info { "Updated night order in $it ms" } }
  measureTimeMillis { updateRoleSaoFromData() }.also { logger.info { "Updated SAO in $it ms" } }
  measureTimeMillis { writeUpdatedSaoFileFromData() }.also { logger.info { "Wrote updated SAO in $it ms" } }
}


private fun updateRolesFromData() {
  val grimToolData = DataJson.fromJson(gson, File(DATA_JSON).readText())
  val grimToolRoles = grimToolData.getAllRoles()
  val rawRoles = grimToolRoles.map { it.toRole() }.associateBy(Role::id)
  val roles = getRolesFromJson()
  roles.map { role -> rawRoles[role.id]?.let { rawRole -> role.copyFrom(rawRole) } ?: role }.run {
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
  jinxes = otherRole.jinxes?.takeUnless { it.isEmpty() } ?: jinxes,
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
          logger.info { "Updating ${role.name}" }
          updatedRole =
            runCatching { role.copyFrom(wikiReader.getRole(role.name ?: "")) }.getOrElse {
              logger.warn { "Couldn't update ${role.name}: ${it.message}" }
              role
            }
        }
        logger.info { "Update complete for ${role.name}: $elapsedTimeMillis ms" }
        updatedRole
      }
    }.awaitAll()
    File(ROLES_JSON).writeText(gson.toJson(updatedRoles))
  }
}

private fun updateNightOrder() {
  val roles = getRolesFromJson()
  val dataJson = DataJson.fromJson(gson, File(DATA_JSON).readText())
  val nightOrder = dataJson.nightOrder ?: return

  val updatedRoles = roles.map { role ->
    role.copy(
      firstNight = updatedNightOrder(role, nightOrder.firstNight.orEmpty(), role.firstNightReminder != null),
      otherNight = updatedNightOrder(role, nightOrder.otherNight.orEmpty(), role.otherNightReminder != null)
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

    index == -1 -> if ((role.type == Role.Type.TRAVELLER || role.type == Role.Type.FABLED || role.type == Role.Type.LORIC) && hasNightReminder) 2 else null
    role.id == "dusk" -> index + 1
    else -> index + 2
  }
}


private fun updateRoleSaoFromData() {
  val dataJson = DataJson.fromJson(gson, File(DATA_JSON).readText())
  val rolesSortedBySao = dataJson.getAllRoles().map { it.id.normalize() }

  val updatedRoles = getRolesFromJson().map { role ->
    val index = rolesSortedBySao.indexOf(role.id.normalize())
    if (index == -1) role.copy(sao = null) else role.copy(sao = index + 1)
  }
    .sortedWith(compareBy<Role> { it.edition == Role.Edition.SPECIAL }.thenBy(nullsLast()) { it.sao }
                  .thenBy { it.type }.thenBy { it.edition }.thenBy { it.name })

  File(ROLES_JSON).writeText(gson.toJson(updatedRoles))
}

private fun writeUpdatedSaoFileFromData() {
  val dataJson = DataJson.fromJson(gson, File(DATA_JSON).readText())
  val rolesSortedBySao = dataJson.getAllRoles().map { it.id.normalize() }

  println(rolesSortedBySao)
  File(SAO_JSON).writeText(gson.toJson(rolesSortedBySao))
}
