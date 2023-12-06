
import com.google.common.collect.ImmutableTable
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.reflect.TypeToken

data class Jinx(val role1: String, val role2: String, val reason: String) {
  companion object {

    fun listFromJson(gson: Gson, json: String): List<Jinx> =
      gson.fromJson<List<JsonObject>>(json, object : TypeToken<List<JsonObject>>() {}.type)
        .filter { it.has("jinxes") || it.has("jinx") }
        .flatMap { outerRole ->
          val jinxes = outerRole.getAsJsonArray(if (outerRole.has("jinxes")) "jinxes" else "jinx")
          jinxes.map { it.asJsonObject }
            .map {
              Jinx(
                outerRole.get("id").asString,
                it.get("id").asString,
                it.get("reason").asString
              )
            }
        }

    fun toTable(jinxes: List<Jinx>): ImmutableTable<String, String, Jinx> {
      val tableBuilder = ImmutableTable.builder<String, String, Jinx>()
      jinxes.forEach {
        tableBuilder.put(it.role1.normalize(), it.role2.normalize(), it)
          .put(it.role2.normalize(), it.role1.normalize(), it)
      }
      return tableBuilder.build()
    }
  }

  fun asTextScriptEntry(roleMap: Map<String, Role>): String {
    val name1 = roleMap[role1.normalize()]?.name
    val name2 = roleMap[role2.normalize()]?.name
    checkNotNull(name1) { "Name1 must be non-null when trying to convert $role1" }
    checkNotNull(name2) { "Name2 must be non-null when trying to convert $role2" }
    return "**$name1 / $name2** - $reason"
  }
}