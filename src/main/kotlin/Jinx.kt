import com.google.common.collect.ImmutableTable
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.reflect.TypeToken

data class Jinx(val role1: String, val role2: String, val reason: String) {
  companion object {
    fun listFromJson(gson: Gson, json: String): List<Jinx> =
      gson.fromJson(json, object : TypeToken<List<Jinx>>() {}.type)

    fun listFromRawJson(gson: Gson, json: String): List<Jinx> =
      gson.fromJson<List<JsonObject>>(json, object : TypeToken<List<JsonObject>>() {}.type)
        .flatMap { outerRole ->
          outerRole.getAsJsonArray("jinx").map { it.asJsonObject }
            .map { Jinx(outerRole.get("id").asString, it.get("id").asString, it.get("reason").asString) }
        }


    fun toTable(jinxes: List<Jinx>): ImmutableTable<String, String, Jinx> {
      val tableBuilder = ImmutableTable.builder<String, String, Jinx>()
      jinxes.forEach {
        tableBuilder.put(normalize(it.role1), normalize(it.role2), it)
          .put(normalize(it.role2), normalize(it.role1), it)
      }
      return tableBuilder.build()
    }
  }

  fun asTextScriptEntry(roleMap: Map<String, Role>): String {
    val name1 = roleMap[normalize(role1)]?.name
    val name2 = roleMap[normalize(role2)]?.name
    checkNotNull(name1) { "Name1 must be non-null when trying to convert $role1" }
    checkNotNull(name2) { "Name2 must be non-null when trying to convert $role2" }
    return "**$name1 / $name2** - $reason"
  }
}