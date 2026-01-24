package models

import com.google.gson.Gson

data class DataJson(
  val editions: List<Edition>? = null,
  val roles: List<DataRole>? = null,
  val fabled: List<DataRole>? = null,
  val lorics: List<DataRole>? = null,
  val servers: List<Server>? = null,
  val nightOrder: NightOrder? = null,
) {

  data class NightOrder(
    val firstNight: List<String>? = null,
    val otherNight: List<String>? = null,
  )

  data class Edition(
    val id: String,
    val name: String,
    val color: String? = null,
    val themes: Int? = null,
    val description: String? = null,
    val level: String? = null,
    val isOfficial: Boolean? = null,
    val firstNight: List<String>? = null,
    val otherNight: List<String>? = null,
  )

  data class Server(
    val host: String,
    val name: String,
    val type: String? = null,
  )

  data class DataRole(
    val id: String = "",
    val name: String? = null,
    val edition: String? = null,
    val team: String? = null,
    val firstNight: Int? = null,
    val firstNightReminder: String? = null,
    val otherNight: Int? = null,
    val otherNightReminder: String? = null,
    val reminders: List<String>? = null,
    val setup: Boolean? = null,
    val ability: String? = null,
    val flavor: String? = null,
    val special: List<SpecialFeature>? = null,
  ) {

    data class SpecialFeature(
      val name: String,
      val type: String,
      val time: String? = null,
      val value: Any? = null,
      val global: String? = null,
    )

    fun toRole(): Role {
      return Role(
        id = id,
        name = name,
        edition = mapEdition(edition),
        type = mapTeam(team),
        firstNight = firstNight,
        firstNightReminder = firstNightReminder?.takeUnless { it.isBlank() },
        otherNight = otherNight,
        otherNightReminder = otherNightReminder?.takeUnless { it.isBlank() },
        reminders = reminders?.takeUnless { it.isEmpty() },
        setup = setup?.takeIf { it },
        ability = ability,
        flavour = flavor,
      )
    }

    private fun mapEdition(edition: String?): Role.Edition? {
      return when (edition) {
        "tb" -> Role.Edition.TROUBLE_BREWING
        "bmr" -> Role.Edition.BAD_MOON_RISING
        "snv" -> Role.Edition.SECTS_AND_VIOLETS
        "fabled" -> Role.Edition.FABLED
        "special" -> Role.Edition.SPECIAL
        else -> null
      }
    }

    private fun mapTeam(team: String?): Role.Type? {
      return when (team) {
        "townsfolk" -> Role.Type.TOWNSFOLK
        "outsider" -> Role.Type.OUTSIDER
        "minion" -> Role.Type.MINION
        "demon" -> Role.Type.DEMON
        "traveller", "traveler" -> Role.Type.TRAVELLER
        "fabled" -> Role.Type.FABLED
        "loric" -> Role.Type.LORIC
        else -> null
      }
    }
  }

  fun getAllRoles(): List<DataRole> = roles.orEmpty() + fabled.orEmpty() + lorics.orEmpty()

  companion object {
    fun fromJson(gson: Gson, json: String): DataJson =
      gson.fromJson(json, DataJson::class.java)
  }
}