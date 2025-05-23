package models

import com.google.gson.Gson

data class GrimToolData(
  val editions: List<Edition>? = null,
  val roles: List<GrimToolRole>? = null,
  val fabled: List<GrimToolRole>? = null,
  val servers: List<Server>? = null,
) {

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

  data class GrimToolRole(
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
        special = special?.map { it.toAppIntegrationFeature() }
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
        else -> null
      }
    }

    private fun SpecialFeature.toAppIntegrationFeature(): Role.AppIntegrationFeature {
      return Role.AppIntegrationFeature(
        type = mapIntegrationType(type),
        name = mapFeatureName(name),
        value = value,
        time = mapTime(time),
        global = mapGlobalScope(global)
      )
    }

    private fun mapIntegrationType(type: String): Role.IntegrationType {
      return when (type) {
        "signal" -> Role.IntegrationType.SIGNAL
        "ability" -> Role.IntegrationType.ABILITY
        "selection" -> Role.IntegrationType.SELECTION
        "vote" -> Role.IntegrationType.VOTE
        else -> Role.IntegrationType.ABILITY
      }
    }

    private fun mapFeatureName(name: String): Role.FeatureName {
      return when (name) {
        "grimoire" -> Role.FeatureName.GRIMOIRE
        "pointing" -> Role.FeatureName.POINTING
        "ghost-votes" -> Role.FeatureName.GHOST_VOTES
        "distribute-roles" -> Role.FeatureName.DISTRIBUTE_ROLES
        "bag-disabled" -> Role.FeatureName.BAG_DISABLED
        "bag-duplicate" -> Role.FeatureName.BAG_DUPLICATE
        "multiplier" -> Role.FeatureName.MULTIPLIER
        else -> Role.FeatureName.GRIMOIRE
      }
    }

    private fun mapTime(time: String?): Role.Time? {
      return when (time) {
        "pregame" -> Role.Time.PREGAME
        "day" -> Role.Time.DAY
        "night" -> Role.Time.NIGHT
        "firstNight" -> Role.Time.FIRST_NIGHT
        "firstDay" -> Role.Time.FIRST_DAY
        "otherNight" -> Role.Time.OTHER_NIGHT
        "otherDay" -> Role.Time.OTHER_DAY
        else -> null
      }
    }

    private fun mapGlobalScope(global: String?): Role.GlobalScope? {
      return when (global) {
        "townsfolk" -> Role.GlobalScope.TOWNSFOLK
        "outsider" -> Role.GlobalScope.OUTSIDER
        "minion" -> Role.GlobalScope.MINION
        "demon" -> Role.GlobalScope.DEMON
        "traveler" -> Role.GlobalScope.TRAVELER
        else -> null
      }
    }
  }

  fun getAllRoles(): List<GrimToolRole> = (roles.orEmpty() + fabled.orEmpty()).sortedBy { it.id }

  companion object {
    fun fromJson(gson: Gson, json: String): GrimToolData =
      gson.fromJson(json, GrimToolData::class.java)
  }
}