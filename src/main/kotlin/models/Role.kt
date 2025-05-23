@file:Suppress("unused")

package models

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken

data class Role(
  val id: String = "",
  val name: String? = null,
  val edition: Edition? = null,
  @SerializedName("team", alternate = ["type"]) val type: Type? = null,
  val sao: Int? = null,
  val firstNight: Int? = null,
  val firstNightReminder: String? = null,
  val otherNight: Int? = null,
  val otherNightReminder: String? = null,
  @SerializedName("reminders", alternate = ["reminderTokens"]) val reminders: List<String>? = null,
  @SerializedName("remindersGlobal", alternate = ["globalReminderTokens", "globalReminders"])
  val remindersGlobal: List<String>? = null,
  val setup: Boolean? = null,
  val ability: String? = null,
  val flavour: String? = null,
  val jinxes: List<Jinx>? = null,
  val urls: Urls? = null,
  val textGameClarification: String? = null,
) {

  data class Urls(
    val wiki: String? = null,
    val icon: String? = null,
  )
  enum class Type {
    @SerializedName("townsfolk")
    TOWNSFOLK,

    @SerializedName("outsider")
    OUTSIDER,

    @SerializedName("minion")
    MINION,

    @SerializedName("demon")
    DEMON,

    @SerializedName("traveller", alternate = ["traveler"])
    TRAVELLER,

    @SerializedName("fabled")
    FABLED
  }

  enum class Edition {
    @SerializedName("tb")
    TROUBLE_BREWING,

    @SerializedName("bmr")
    BAD_MOON_RISING,

    @SerializedName("snv")
    SECTS_AND_VIOLETS,

    @SerializedName("fabled")
    FABLED,

    @SerializedName("special")
    SPECIAL
  }

  data class Jinx(val id: String, val reason: String)

  companion object {
    fun listFromJson(gson: Gson, json: String): List<Role> =
      gson.fromJson(json, object : TypeToken<List<Role>>() {}.type)
  }

  fun asTextScriptEntry(): String {
    checkNotNull(name) { "Name must be non-null" }
    checkNotNull(ability) { "Ability must be non-null for $name" }
    return "**$name** -- ${ability.replace("*", "\\*")}"
  }

  fun asTextScriptClarificationEntry(): String? {
    if (textGameClarification == null) return null
    checkNotNull(name) { "Name must be non-null" }
    return "**$name** - $textGameClarification"
  }
}





