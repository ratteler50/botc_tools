@file:Suppress("RedundantSuppression")

import Role.Type.FABLED
import Role.Type.TRAVELLER
import com.google.common.collect.ImmutableTable
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import java.io.File
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

val gson: Gson = GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create()

private const val INPUT_SCRIPT_JSON = "./src/data/input_script.json"
private const val ROLES_JSON = "./src/data/roles.json"
private const val RAW_ROLES_JSON = "./src/data/raw_roles.json"
private const val NIGHTSHEET_JSON = "./src/data/nightsheet.json"
private const val RAW_INTERACTIONS_JSON = "./src/data/raw_interactions.json"
private const val INTERACTIONS_JSON = "./src/data/interactions.json"
private const val RAW_JINXES_JSON = "./src/data/raw_jinxes.json"
private const val JINXES_JSON = "./src/data/jinxes.json"
private const val RAW_SAO_JSON = "./src/data/raw_sao.json"
private const val SAO_JSON = "./src/data/sao.json"


suspend fun main() {
  val roleMap = Role.toMap(Role.setFromJson(gson, File(ROLES_JSON).readText()))
  val scriptMetadata = getScriptMetadata()
  val outputFilename = "./src/data/${scriptMetadata?.name ?: "output"}.md"
  updateSourceJsons()
  File(outputFilename).writeText(
    ScriptPrinter(
      scriptMetadata,
      getScriptRoles(roleMap),
      getJinxTable(JINXES_JSON),
      getJinxTable(INTERACTIONS_JSON),
      roleMap
    ).textScriptString()
  )
}

@Suppress("RedundantSuspendModifier")
private suspend fun updateSourceJsons() {
  // updateRolesFromRawRoles()
  updateAbilitiesAndFlavorFromWiki()
  updateJinxesFromRawJinxes()
  updateInteractionsFromRawInteractions()
  updateRawInteractionsFromInteractions()
  updateNightOrder()
  updateSao()
}

@Suppress("unused")
fun updateRolesFromRawRoles() {
  val rawRoles = Role.setFromJson(gson, File(RAW_ROLES_JSON).readText()).associateBy { it.id }
  val roles = Role.setFromJson(gson, File(ROLES_JSON).readText())
  roles.map { role ->
    rawRoles[role.id]?.let { rawRole ->
      role.copy(
        id = rawRole.id,
        name = rawRole.name,
        ability = rawRole.ability,
        firstNightReminder = rawRole.firstNightReminder?.takeIf { it.isNotBlank() },
        otherNightReminder = rawRole.otherNightReminder?.takeIf { it.isNotBlank() },
        reminders = rawRole.reminders?.takeIf { it.isNotEmpty() },
      )
    } ?: role
  }
    .run {
      File(ROLES_JSON).writeText(gson.toJson(this))
    }
}

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
  val roles = Role.setFromJson(gson, File(ROLES_JSON).readText())
  val sao = Sao.listFromJson(gson, File(SAO_JSON).readText())
    .sortedWith(compareBy({ it.type }, { it.category }, { it.pixels }, { it.characters }))
  val updatedRoles = roles.map { role ->
    when (val index = sao.indexOfFirst { normalize(it.id) == normalize(role.id) }) {
      -1 -> role
      else -> role.copy(standardAmyOrder = index + 1)
    }
  }
    .sortedWith(compareBy<Role, Int?>(nullsLast()) { it.standardAmyOrder }.thenBy(nullsLast()) { it.type }
                  .thenBy { it.edition }.thenBy { it.name })
  File(ROLES_JSON).writeText(gson.toJson(updatedRoles))
}

@Suppress("unused")
suspend fun updateAbilitiesAndFlavorFromWiki() = coroutineScope {
  val roles = Role.setFromJson(gson, File(ROLES_JSON).readText())
  val deferredUpdates = roles.map { role ->
    async {
      try {
        println("Updating ${role.name}")
        val wikiRole = BotcRoleLoader().getRole(role.name ?: "")
        role.copy(
          name = wikiRole.title,
          ability = wikiRole.roleContent.abilityText,
          flavour = wikiRole.roleContent.flavourText.takeIf { it.isNotBlank() },
          urls = role.urls?.copy(wiki = wikiRole.wikiUrl, icon = wikiRole.imageUrl)
        )
      } catch (e: Exception) {
        println("Couldn't update ${role.name}")
        role
      }
    }
  }
  val updatedRoles = deferredUpdates.awaitAll()
  File(ROLES_JSON).writeText(gson.toJson(updatedRoles))
}

fun updateNightOrder() {
  val roles = Role.setFromJson(gson, File(ROLES_JSON).readText())
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
  val index = nightList.indexOfFirst { normalize(it) == normalize(role.id) }
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

fun normalize(str: String): String = str.lowercase().replace(Regex("[^a-z]"), "")