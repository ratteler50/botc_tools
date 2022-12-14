import com.google.common.collect.ImmutableTable
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import java.io.File

val gson: Gson = GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create()

fun main() {
  val roleMap = Role.toMap(Role.setFromJson(gson, File("./src/data/roles.json").readText()))
  File("./src/data/output.txt").writeText(ScriptPrinter(getScriptRoles(roleMap),
                                                        getJinxTable(),
                                                        roleMap).textScriptString())
}


fun getScriptRoles(roleMap: Map<String, Role>): List<Role> {
  val charList = Script.getRolesOnScript(gson, File("./src/data/example_script.json").readText())
  return charList.map { char -> checkNotNull(roleMap[char]) { "Couldn't find $char in roleMap" } }
    .sortedBy { it.standardAmyOrder }
}

fun getJinxTable(): ImmutableTable<String, String, Jinx> {
  val jinxes = Jinx.listFromJson(gson, File("./src/data/jinxes.json").readText())
  val interactions = Jinx.listFromJson(gson, File("./src/data/interactions.json").readText())
  val jinxTable = Jinx.toTable(jinxes)
  val interactionTable = Jinx.toTable(interactions)
  return ImmutableTable.builder<String, String, Jinx>().putAll(jinxTable).putAll(interactionTable)
    .build()
}