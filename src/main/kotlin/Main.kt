@file:Suppress("unused", "RedundantSuspendModifier")

import AppConfig.INPUT_SCRIPT_JSON
import AppConfig.INTERACTIONS_JSON
import AppConfig.JINXES_JSON
import AppConfig.NIGHTSHEET_JSON
import AppConfig.RAW_INTERACTIONS_JSON
import AppConfig.RAW_JINXES_JSON
import AppConfig.RAW_ROLES_JSON
import AppConfig.RAW_SAO_JSON
import AppConfig.ROLES_JSON
import AppConfig.SAO_JSON
import Role.Edition.SPECIAL
import Role.Type.FABLED
import Role.Type.TRAVELLER
import com.google.common.collect.ImmutableTable
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext


object AppConfig {
  const val INPUT_SCRIPT_JSON = "./src/data/input_script.json"
  const val ROLES_JSON = "./src/data/roles.json"
  const val RAW_ROLES_JSON = "./src/data/raw_roles.json"
  const val NIGHTSHEET_JSON = "./src/data/nightsheet.json"
  const val RAW_INTERACTIONS_JSON = "./src/data/raw_interactions.json"
  const val INTERACTIONS_JSON = "./src/data/interactions.json"
  const val RAW_JINXES_JSON = "./src/data/raw_jinxes.json"
  const val JINXES_JSON = "./src/data/jinxes.json"
  const val RAW_SAO_JSON = "./src/data/raw_sao.json"
  const val SAO_JSON = "./src/data/sao.json"
}

val gson: Gson by lazy { GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create() }
val wikiReader by  lazy {BotcRoleLoader()}


suspend fun main() {
  val scriptMetadata = getScriptMetadata()
  val outputFilename = "./src/data/${scriptMetadata?.name ?: "output"}.md"
  updateSourceJsons()
  File(outputFilename).writeText(generateTextScript(scriptMetadata))
}

private suspend fun updateSourceJsons() {
  updateRolesFromRawRoles()
  updateRolesFromWiki()
  updateJinxesFromRawJinxes()
  updateInteractionsFromRawInteractions()
  updateRawInteractionsFromInteractions()
  updateNightOrder()
  updateSao()
}

private fun generateTextScript(scriptMetadata: Script?): String {
  val roleMap = getRolesFromJson().associateBy(Role::id)
  return ScriptPrinter(
    scriptMetadata,
    getScriptRoles(roleMap),
    getJinxTable(JINXES_JSON),
    getJinxTable(INTERACTIONS_JSON),
    roleMap
  ).textScriptString()
}
private fun getRolesFromJson() = Role.listFromJson(gson, File(ROLES_JSON).readText())


private fun updateRolesFromRawRoles() {
  val rawRoles = Role.listFromJson(gson, File(RAW_ROLES_JSON).readText()).associateBy(Role::id)
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
)

private fun Role.copyFrom(wikiRole: BotcRoleLoader.RoleResult): Role = copy(
  name = wikiRole.title,
  ability = wikiRole.roleContent.abilityText.takeIf { it.isNotBlank() },
  flavour = wikiRole.roleContent.flavourText.takeIf { it.isNotBlank() },
  urls = urls?.copy(wiki = wikiRole.wikiUrl, icon = wikiRole.imageUrl)
    ?: Role.Urls(wiki = wikiRole.wikiUrl, icon = wikiRole.imageUrl),
)


fun getScriptMetadata(): Script? =
  Script.getScriptMetadata(gson, File(INPUT_SCRIPT_JSON).readText())

fun getScriptRoles(roleMap: Map<String, Role>): List<Role> {
  val charList = Script.getRolesOnScript(gson, File(INPUT_SCRIPT_JSON).readText())
  return charList.map { char -> checkNotNull(roleMap[char]) { "Couldn't find $char in roleMap" } }
    .sortedBy { it.standardAmyOrder }
}

fun updateSaoFromRawSao() {
  File(SAO_JSON).writeText(
    gson.toJson(
      Sao.listFromRawJson(File(RAW_SAO_JSON).readText())
        .sortedWith(
          compareBy({ it.type }, { it.category }, { it.pixels }, { it.characters })
        )
    )
  )
}

fun updateJinxesFromRawJinxes() {
  File(JINXES_JSON).writeText(
    gson.toJson(
      Jinx.listFromRawJson(gson, File(RAW_JINXES_JSON).readText())
        .sortedWith(compareBy({ it.role1 }, { it.role2 }))
    )
  )
}

fun updateInteractionsFromRawInteractions() {
  File(INTERACTIONS_JSON).writeText(
    gson.toJson(
      Jinx.listFromRawJson(gson, File(RAW_INTERACTIONS_JSON).readText())
        .sortedWith(compareBy({ it.role1 }, { it.role2 }))
    )
  )
}

fun updateRawInteractionsFromInteractions() {
  File(RAW_INTERACTIONS_JSON).writeText(
    gson.toJson(Jinx.listFromJson(gson, File(INTERACTIONS_JSON).readText())
                  .sortedWith(compareBy({ it.role1 }, { it.role2 })).groupBy { it.role1 }
                  .map { Jinx.buildRawJsonForRole(it.value) })
  )
  File(INTERACTIONS_JSON).writeText(
    gson.toJson(
      Jinx.listFromRawJson(gson, File(RAW_INTERACTIONS_JSON).readText())
        .sortedWith(compareBy({ it.role1 }, { it.role2 }))
    )
  )
}

fun updateSao() {
  updateSaoFromRawSao()
  val roles = getRolesFromJson()
  val sao = Sao.listFromJson(gson, File(SAO_JSON).readText())
    .sortedWith(compareBy({ it.type }, { it.category }, { it.pixels }, { it.characters }))
  val updatedRoles = roles.map { role ->
    when (val index = sao.indexOfFirst { it.id.normalize() == role.id.normalize() }) {
      -1 -> role
      else -> role.copy(standardAmyOrder = index + 1)
    }
  }
    .sortedWith(compareBy<Role> { it.edition == SPECIAL }
                  .thenBy(nullsLast()) { it.standardAmyOrder }
                  .thenBy { it.type }
                  .thenBy { it.edition }
                  .thenBy { it.name })
  File(ROLES_JSON).writeText(gson.toJson(updatedRoles))
}

suspend fun updateRolesFromWiki() {
  val roles = getRolesFromJson()
  withContext(Dispatchers.IO)  {
    val updatedRoles = roles.map { role ->
      async {
        println("Updating ${role.name}")
        val updatedRole = runCatching { role.copyFrom(wikiReader.getRole(role.name ?: "")) }
          .getOrElse {
            println("Couldn't update ${role.name}: ${it.message}")
            role  // This ensures the original role is returned in case of failure
          }
        println("Update complete for ${role.name}")
        updatedRole
      }
    }.awaitAll()
    File(ROLES_JSON).writeText(gson.toJson(updatedRoles))
  }
}

fun updateNightOrder() {
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

    index == -1 -> if ((role.type == TRAVELLER || role.type == FABLED) && hasNightReminder) 2 else null
    role.id == "dusk" -> index + 1
    else -> index + 2
  }
}


fun getJinxTable(inputJson: String): ImmutableTable<String, String, Jinx> {
  return Jinx.toTable(Jinx.listFromJson(gson, File(inputJson).readText()))
}

fun String.normalize(): String = this.lowercase().replace(Regex("[^a-z]"), "")