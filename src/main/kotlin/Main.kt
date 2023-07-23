import com.google.common.collect.ImmutableTable
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import java.io.File

val gson: Gson = GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create()

private const val SCRIPT_INPUT = "./src/data/example_script.json"
private const val SCRIPT_OUTPUT = "./src/data/output.md"
private const val ROLES_JSON = "./src/data/roles.json"
private const val NIGHTSHEET_JSON = "./src/data/nightsheet.json"
private const val SAO_JSON = "./src/data/sao.json"

fun main() {
  val roleMap = Role.toMap(Role.setFromJson(gson, File(ROLES_JSON).readText()))
  updateNightOrder()
  updateSao()
  File(SCRIPT_OUTPUT).writeText(
    ScriptPrinter(
      getScriptMetadata(), getScriptRoles(roleMap), getJinxTable(), roleMap
    ).textScriptString()
  )
}

fun getScriptMetadata(): Script? = Script.getScriptMetadata(gson, File(SCRIPT_INPUT).readText())

fun getScriptRoles(roleMap: Map<String, Role>): List<Role> {
  val charList = Script.getRolesOnScript(gson, File(SCRIPT_INPUT).readText())
  return charList.map { char -> checkNotNull(roleMap[char]) { "Couldn't find $char in roleMap" } }
    .sortedBy { it.standardAmyOrder }
}

fun updateSaoFromRawSao() {
  File(SAO_JSON).writeText(
    gson.toJson(
      Sao.listFromRawJson(File("./src/data/raw_sao.json").readText()).sortedWith(
        compareBy({ it.type },
                  { it.category },
                  { it.pixels },
                  { it.characters })
      )
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
  }.sortedWith(compareBy(nullsLast()) { it.standardAmyOrder })
  File(ROLES_JSON).writeText(gson.toJson(updatedRoles))
}

fun updateNightOrder() {
  val roles = Role.setFromJson(gson, File(ROLES_JSON).readText())
  val nightSheet = NightSheet.fromJson(gson, File(NIGHTSHEET_JSON).readText())
  val updatedRoles = roles.map { role ->
    when (val index = nightSheet.firstNight.indexOfFirst { normalize(it) == normalize(role.id) }) {
      -1 -> role.copy(firstNight = null)
      else -> role.copy(firstNight = index + 2)
    }
  }.map { role ->
    when (val index = nightSheet.otherNight.indexOfFirst { normalize(it) == normalize(role.id) }) {
      -1 -> role.copy(otherNight = null)
      else -> role.copy(otherNight = index + 1)
    }
  }
  File(ROLES_JSON).writeText(gson.toJson(updatedRoles))
}


fun getJinxTable(): ImmutableTable<String, String, Jinx> {
  val jinxes = Jinx.listFromJson(gson, File("./src/data/jinxes.json").readText())
  val interactions = Jinx.listFromJson(gson, File("./src/data/interactions.json").readText())
  val jinxTable = Jinx.toTable(jinxes)
  val interactionTable = Jinx.toTable(interactions)
  return ImmutableTable.builder<String, String, Jinx>().putAll(jinxTable).putAll(interactionTable)
    .build()
}

fun normalize(str: String): String = str.lowercase().replace(Regex("[^a-z]"), "")