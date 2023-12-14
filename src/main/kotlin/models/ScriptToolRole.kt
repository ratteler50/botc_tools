@file:Suppress("unused")

package models

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken

data class ScriptToolRole(
  val id: String,
  val name: String,
  val roleType: RoleType,
  val print: String,
  val icon: String,
  val version: Version,
  val isDisabled: Boolean,
) {


  enum class RoleType {
    @SerializedName("townsfolk")
    TOWNSFOLK,

    @SerializedName("outsider")
    OUTSIDER,

    @SerializedName("minion")
    MINION,

    @SerializedName("demon")
    DEMON,

    @SerializedName("travellers")
    TRAVELLER,

    @SerializedName("fabled")
    FABLED
  }

  enum class Version {
    @SerializedName("1 - Trouble Brewing")
    TROUBLE_BREWING,

    @SerializedName("2 - Bad Moon Rising")
    BAD_MOON_RISING,

    @SerializedName("3 - Sects and Violets")
    SECTS_AND_VIOLETS,

    @SerializedName("4a - Kickstarter Experimental")
    KICKSTARTER_EXPERIMENTAL,

    @SerializedName("4b - Unreleased Experimental")
    UNRELEASED_EXPERIMENTAL,

    @SerializedName("Extras")
    EXTRAS
  }

  companion object {
    fun listFromJson(gson: Gson, json: String): List<ScriptToolRole> =
      gson.fromJson(json, object : TypeToken<List<ScriptToolRole>>() {}.type)

  }
}





