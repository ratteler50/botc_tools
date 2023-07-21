import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

data class Script(
  val id: String,
  val logo: String? = null,
  val name: String? = null,
  val author: String? = null,
) {
  companion object {
    fun getRolesOnScript(gson: Gson, json: String): Set<String> =
      gson.fromJson<List<Script>?>(json, object : TypeToken<List<Script>>() {}.type).asSequence()
        .map { it.id.lowercase() }.filterNot { it == "_meta" }
        .map { it.replace(Regex("[^a-z]"), "") }.plus(listOf("minion", "demon", "dusk", "dawn"))
        .toSet()

    fun getScriptMetadata(gson: Gson, json: String): Script? =
      gson.fromJson<List<Script>?>(json, object : TypeToken<List<Script>>() {}.type)
        .firstOrNull { it.id.lowercase() == "_meta" }
  }
}