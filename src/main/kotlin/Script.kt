
import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import com.google.gson.reflect.TypeToken

data class Script(
  val id: String,
  val logo: String? = null,
  val name: String? = null,
  val author: String? = null,
) {
  companion object {
    fun getRolesOnScript(gson: Gson, json: String): Set<Role> =
      gson.fromJson<List<JsonElement>>(json, object : TypeToken<List<JsonElement>>() {}.type)
        .mapNotNull { parseRole(it) }.toSet()
        .plus(listOf(Role("minioninfo"), Role("demoninfo"), Role("dusk"), Role("dawn")))

    private fun parseRole(entry: JsonElement): Role? =
      when (entry) {
        is JsonObject -> parseRole(gson.fromJson(entry, Role::class.java))
        is JsonPrimitive -> parseRole(gson.fromJson(entry, String::class.java))
        else -> null

      }

    private fun parseRole(entry: Role): Role? =
      if (entry.id == "_meta") null else entry.copy(id = entry.id.normalize())

    private fun parseRole(entry: String): Role =
      Role(entry.normalize())

    fun getScriptMetadata(gson: Gson, json: String): Script? =
      gson.fromJson<List<JsonElement>?>(json, object : TypeToken<List<JsonElement>>() {}.type)
        .filter { it.isJsonObject }.map { gson.fromJson(it, Script::class.java) }
        .firstOrNull { it.id == "_meta" }
  }
}