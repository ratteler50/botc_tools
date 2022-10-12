import com.google.common.collect.ImmutableTable
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import java.io.File

val gson: Gson = GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create()

fun main() {
  val roleMap = Role.toMap(Role.setFromJson(gson, File("./src/data/roles.json").readText()))
  ScriptPrinter(getScriptRoles(roleMap), getJinxTable(), roleMap).printScript()
}


fun getScriptRoles(roleMap: Map<String, Role>): List<Role> {
  val charList =
    Script.getRolesOnScript(gson, File("./src/data/example_script.json").readText())
  return charList.map { checkNotNull(roleMap[it]) }.sortedBy { it.standardAmyOrder }
}

fun getJinxTable(): ImmutableTable<String, String, Jinx> {
  val jinxes = Jinx.listFromJson(gson, File("./src/data/jinxes.json").readText())
  val interactions = Jinx.listFromJson(gson, File("./src/data/interactions.json").readText())
  val jinxTable = Jinx.toTable(jinxes)
  val interactionTable = Jinx.toTable(interactions)
  return ImmutableTable.builder<String, String, Jinx>().putAll(jinxTable).putAll(interactionTable)
    .build()
}