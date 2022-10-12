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
    fun listFromJson(gson: Gson, json: String): List<Sao> =
      gson.fromJson(json, object : TypeToken<List<Sao>>() {}.type)
  }
}
