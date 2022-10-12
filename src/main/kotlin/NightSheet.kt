import com.google.gson.Gson

data class NightSheet(val firstNight: List<String>, val otherNight: List<String>) {
  companion object {
    fun fromJson(gson: Gson, json: String): NightSheet =
      gson.fromJson(json, NightSheet::class.java)
  }
}
