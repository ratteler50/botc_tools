package models

import com.google.common.truth.Truth.assertThat
import com.google.gson.Gson
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class GrimToolDataTest {

  @Test
  fun fromJson_parsesCompleteStructure() {
    val json = """{
      "editions": [{
        "id": "tb",
        "name": "Trouble Brewing",
        "color": "#AB0D27",
        "themes": 6,
        "description": "A description",
        "level": "Beginner",
        "isOfficial": true,
        "firstNight": ["dusk", "washerwoman", "dawn"],
        "otherNight": ["dusk", "monk", "dawn"]
      }],
      "roles": [{
        "id": "washerwoman",
        "name": "Washerwoman",
        "edition": "tb",
        "team": "townsfolk",
        "ability": "You start knowing that 1 of 2 players is a particular Townsfolk.",
        "flavor": "Bloodstains on a dinner jacket?",
        "firstNight": 46,
        "otherNight": 0,
        "setup": false,
        "reminders": ["Townsfolk", "Wrong"]
      }],
      "fabled": [{
        "id": "storyteller",
        "name": "Storyteller",
        "edition": "fabled",
        "team": "fabled",
        "ability": "You may break any rule, as long as it is more fun."
      }],
      "servers": [{
        "host": "eu",
        "name": "EU Central",
        "type": "mediasoup"
      }]
    }"""
    
    val grimToolData = GrimToolData.fromJson(Gson(), json)
    
    // Test editions
    assertThat(grimToolData.editions).hasSize(1)
    val edition = grimToolData.editions!![0]
    assertThat(edition.id).isEqualTo("tb")
    assertThat(edition.name).isEqualTo("Trouble Brewing")
    assertThat(edition.color).isEqualTo("#AB0D27")
    assertThat(edition.themes).isEqualTo(6)
    assertThat(edition.description).isEqualTo("A description")
    assertThat(edition.level).isEqualTo("Beginner")
    assertThat(edition.isOfficial).isTrue()
    assertThat(edition.firstNight).containsExactly("dusk", "washerwoman", "dawn")
    assertThat(edition.otherNight).containsExactly("dusk", "monk", "dawn")
    
    // Test roles
    assertThat(grimToolData.roles).hasSize(1)
    val role = grimToolData.roles!![0]
    assertThat(role.id).isEqualTo("washerwoman")
    assertThat(role.name).isEqualTo("Washerwoman")
    assertThat(role.edition).isEqualTo("tb")
    assertThat(role.team).isEqualTo("townsfolk")
    assertThat(role.ability).isEqualTo("You start knowing that 1 of 2 players is a particular Townsfolk.")
    assertThat(role.flavor).isEqualTo("Bloodstains on a dinner jacket?")
    
    // Test fabled
    assertThat(grimToolData.fabled).hasSize(1)
    val fabled = grimToolData.fabled!![0]
    assertThat(fabled.id).isEqualTo("storyteller")
    assertThat(fabled.name).isEqualTo("Storyteller")
    
    // Test servers
    assertThat(grimToolData.servers).hasSize(1)
    val server = grimToolData.servers!![0]
    assertThat(server.host).isEqualTo("eu")
    assertThat(server.name).isEqualTo("EU Central")
    assertThat(server.type).isEqualTo("mediasoup")
  }

  @Test
  fun getAllRoles_combinesRolesAndFabled() {
    val grimToolData = GrimToolData(
      roles = listOf(
        GrimToolData.GrimToolRole(id = "washerwoman", name = "Washerwoman"),
        GrimToolData.GrimToolRole(id = "monk", name = "Monk")
      ),
      fabled = listOf(
        GrimToolData.GrimToolRole(id = "storyteller", name = "Storyteller"),
        GrimToolData.GrimToolRole(id = "djinn", name = "Djinn")
      )
    )
    
    val allRoles = grimToolData.getAllRoles()
    
    assertThat(allRoles).hasSize(4)
    assertThat(allRoles.map { it.id }).containsExactly(
      "washerwoman", "monk", "storyteller", "djinn"
    )
  }

  @Test
  fun grimToolRole_toRole_convertsCorrectly() {
    val grimToolRole = GrimToolData.GrimToolRole(
      id = "monk",
      name = "Monk",
      edition = "tb",
      team = "townsfolk",
      ability = "Each night*, choose a player (not yourself): they are safe from the Demon tonight.",
      flavor = "I will pray for you.",
      firstNight = null,
      otherNight = 12,
      setup = false,
      reminders = listOf("Protected")
    )
    
    val role = grimToolRole.toRole()
    
    assertThat(role.id).isEqualTo("monk")
    assertThat(role.name).isEqualTo("Monk")
    assertThat(role.edition).isEqualTo(Role.Edition.TROUBLE_BREWING)
    assertThat(role.type).isEqualTo(Role.Type.TOWNSFOLK)
    assertThat(role.ability).isEqualTo("Each night*, choose a player (not yourself): they are safe from the Demon tonight.")
    assertThat(role.flavour).isEqualTo("I will pray for you.")
    assertThat(role.firstNight).isNull()
    assertThat(role.otherNight).isEqualTo(12)
    assertThat(role.setup).isNull() // false setup becomes null
    assertThat(role.reminders).containsExactly("Protected")
  }

  @Test
  fun grimToolRole_toRole_mapsEditionsCorrectly() {
    val testCases = mapOf(
      "tb" to Role.Edition.TROUBLE_BREWING,
      "bmr" to Role.Edition.BAD_MOON_RISING,
      "snv" to Role.Edition.SECTS_AND_VIOLETS,
      "fabled" to Role.Edition.FABLED,
      "special" to Role.Edition.SPECIAL,
      "unknown" to null
    )
    
    testCases.forEach { (grimEdition, expectedEdition) ->
      val grimToolRole = GrimToolData.GrimToolRole(id = "test", edition = grimEdition)
      val role = grimToolRole.toRole()
      assertThat(role.edition).isEqualTo(expectedEdition)
    }
  }

  @Test
  fun grimToolRole_toRole_mapsTeamsCorrectly() {
    val testCases = mapOf(
      "townsfolk" to Role.Type.TOWNSFOLK,
      "outsider" to Role.Type.OUTSIDER,
      "minion" to Role.Type.MINION,
      "demon" to Role.Type.DEMON,
      "traveller" to Role.Type.TRAVELLER,
      "traveler" to Role.Type.TRAVELLER,
      "fabled" to Role.Type.FABLED,
      "unknown" to null
    )
    
    testCases.forEach { (grimTeam, expectedType) ->
      val grimToolRole = GrimToolData.GrimToolRole(id = "test", team = grimTeam)
      val role = grimToolRole.toRole()
      assertThat(role.type).isEqualTo(expectedType)
    }
  }

  @Test
  fun grimToolRole_toRole_handlesBlankStrings() {
    val grimToolRole = GrimToolData.GrimToolRole(
      id = "test",
      firstNightReminder = "",
      otherNightReminder = "   ",
      reminders = emptyList(),
      setup = false
    )
    
    val role = grimToolRole.toRole()
    
    assertThat(role.firstNightReminder).isNull()
    assertThat(role.otherNightReminder).isNull()
    assertThat(role.reminders).isNull()
    assertThat(role.setup).isNull()
  }

  @Test
  fun specialFeature_mapsCorrectly() {
    val specialFeature = GrimToolData.GrimToolRole.SpecialFeature(
      name = "distribute-roles",
      type = "ability",
      time = "pregame",
      value = "test",
      global = "townsfolk"
    )
    
    // Test that the special feature can be converted internally
    val grimToolRole = GrimToolData.GrimToolRole(
      id = "test",
      special = listOf(specialFeature)
    )
    
    // Verify the GrimToolRole has special features
    assertThat(grimToolRole.special).hasSize(1)
    assertThat(grimToolRole.special?.get(0)?.name).isEqualTo("distribute-roles")
    assertThat(grimToolRole.special?.get(0)?.type).isEqualTo("ability")
    
    // Role conversion should not include special features since they're not in roles.json
    val role = grimToolRole.toRole()
    assertThat(role.id).isEqualTo("test")
  }
}