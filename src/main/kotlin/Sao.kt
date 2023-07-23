import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

data class Sao(
  val id: String,
  val type: Int,
  val category: Int,
  val pixels: Int,
  val characters: Int,
) {
  companion object {
    private fun saoFromLine(saoLine: String): Sao? =
      ".*\"(.*)\".*\"(\\d*).(\\d*).(\\d*).(\\d*).*".toRegex()
        .matchEntire(saoLine)?.destructured?.let { (id, type, category, pixels, characters) ->
          Sao(id, type.toInt(), category.toInt(), pixels.toInt(), characters.toInt())
        }

    fun listFromRawJson(json: String): List<Sao> =
      json.lineSequence().map(::saoFromLine).filterNotNull().toList()

    fun listFromJson(gson: Gson, json: String): List<Sao> =
      gson.fromJson(json, object : TypeToken<List<Sao>>() {}.type)
  }
}
