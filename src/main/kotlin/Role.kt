import com.google.common.collect.ImmutableMap
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken

data class Role(
  val id: String = "",
  val name: String? = null,
  val edition: Edition? = null,
  @SerializedName("team", alternate = ["type"]) val type: Type? = null,
  val standardAmyOrder: Int? = null,
  val firstNight: Int? = null,
  val firstNightReminder: String? = null,
  val otherNight: Int? = null,
  val otherNightReminder: String? = null,
  @SerializedName("reminders", alternate = ["reminderTokens"]) val reminders: List<String>? = null,
  @SerializedName("remindersGlobal", alternate = ["globalReminderTokens"])
  val remindersGlobal: List<String>? = null,
  val setup: Boolean? = null,
  val ability: String? = null,
  val textGameClarification: String? = null,
) {
  enum class Type {
    @SerializedName("townsfolk")
    TOWNSFOLK,

    @SerializedName("outsider")
    OUTSIDER,

    @SerializedName("minion")
    MINION,

    @SerializedName("demon")
    DEMON,

    @SerializedName("traveler", alternate = ["traveller"])
    TRAVELER,

    @SerializedName("fabled")
    FABLED,

    @SerializedName("placeholder")
    PLACEHOLDER
  }

  enum class Edition {
    @SerializedName("tb")
    TROUBLE_BREWING,

    @SerializedName("bmr")
    BAD_MOON_RISING,

    @SerializedName("snv")
    SECTS_AND_VIOLETS
  }

  companion object {
    fun setFromJson(gson: Gson, json: String): Set<Role> =
      gson.fromJson<Set<Role>?>(json, object : TypeToken<List<Role>>() {}.type).toSet()

    fun toMap(roles: Set<Role>): ImmutableMap<String, Role> {
      return roles.stream().collect(ImmutableMap.toImmutableMap({ it.id }, { it }))
    }
  }

  fun asTextScriptEntry(): String {
    checkNotNull(name) { "Name must be non-null" }
    checkNotNull(ability) { "Ability must be non-null" }
    return "**$name** -- ${ability.replace("*", "\\*")}"
  }

  fun asTextScriptClarificationEntry(): String? {
    if (textGameClarification == null) return null
    checkNotNull(name) { "Name must be non-null" }
    return "**$name** - $textGameClarification"
  }
}





