@file:Suppress("unused", "RedundantSuspendModifier")

import AppConfig.INPUT_SCRIPT_JSON
import AppConfig.INTERACTIONS_JSON
import AppConfig.JINXES_JSON
import AppConfig.ROLES_JSON
import com.google.common.collect.ImmutableTable
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import java.io.File
import kotlin.system.measureTimeMillis
import models.Jinx
import models.Role
import models.Script


object AppConfig {
  const val INPUT_SCRIPT_JSON = "./src/data/input_script.json"
  const val ROLES_JSON = "./src/data/roles.json"
  const val GRIM_TOOL_ROLES = "./src/data/grim_tool_roles.json"
  const val NIGHTSHEET_JSON = "./src/data/nightsheet.json"
  const val INTERACTIONS_JSON = "./src/data/interactions.json"
  const val JINXES_JSON = "./src/data/jinxes.json"
  const val SCRIPT_TOOL_ROLES = "./src/data/script_tool_roles.json"
}

val gson: Gson by lazy { GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create() }
val wikiReader by lazy { BotcRoleLoader() }


suspend fun main() {
  val inputScriptJson = File(INPUT_SCRIPT_JSON).readText()
  val scriptMetadata = getScriptMetadata(inputScriptJson)
  val outputFilename = "./src/data/${scriptMetadata?.name ?: "output"}.md"
  measureTimeMillis {
    File(outputFilename).writeText(
      generateTextScript(
        scriptMetadata,
        inputScriptJson
      )
    )
  }.also { println("Generated script in $it ms") }
}

fun generateTextScript(scriptMetadata: Script?, inputScriptJson: String): String {
  val roleMap = getRolesFromJson().associateBy(Role::id)
  return ScriptPrinter(
    scriptMetadata,
    getScriptRoles(roleMap, inputScriptJson),
    getJinxTable(JINXES_JSON),
    getJinxTable(INTERACTIONS_JSON),
    roleMap
  ).textScriptString()
}

fun getRolesFromJson() = Role.listFromJson(gson, File(ROLES_JSON).readText())

fun getScriptMetadata(json: String): Script? = Script.getScriptMetadata(gson, json)

fun getScriptRoles(roleMap: Map<String, Role>, scriptJson: String): List<Role> {
  val charList = Script.getRolesOnScript(gson, scriptJson)
  return charList.map { char -> roleMap[char.id.normalize()] ?: char }
    .sortedBy { it.standardAmyOrder }
}

fun getJinxTable(inputJson: String): ImmutableTable<String, String, Jinx> {
  return Jinx.toTable(Jinx.listFromJson(gson, File(inputJson).readText()))
}

fun String.normalize(): String = this.lowercase().replace(Regex("[^a-z]"), "")