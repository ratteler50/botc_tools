package models

import com.google.common.truth.Truth.assertThat
import com.google.gson.Gson
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class ScriptToolRoleTest {

  @Test
  fun listFromJson_parsesCorrectly() {
    val json = """[{
      "id": "washerwoman",
      "name": "Washerwoman", 
      "roleType": "townsfolk",
      "print": "You start knowing that 1 of 2 players is a particular Townsfolk.",
      "icon": "icon_washerwoman.png",
      "version": "1 - Trouble Brewing",
      "isDisabled": false
    }, {
      "id": "scarletwoman",
      "name": "Scarlet Woman",
      "roleType": "minion", 
      "print": "If there are 5 or more players alive & the Demon dies, you become the Demon.",
      "icon": "icon_scarletwoman.png",
      "version": "1 - Trouble Brewing",
      "isDisabled": true
    }]"""
    
    val roles = ScriptToolRole.listFromJson(Gson(), json)
    
    assertThat(roles).hasSize(2)
    
    val washerwoman = roles[0]
    assertThat(washerwoman.id).isEqualTo("washerwoman")
    assertThat(washerwoman.name).isEqualTo("Washerwoman")
    assertThat(washerwoman.roleType).isEqualTo(ScriptToolRole.RoleType.TOWNSFOLK)
    assertThat(washerwoman.print).isEqualTo("You start knowing that 1 of 2 players is a particular Townsfolk.")
    assertThat(washerwoman.icon).isEqualTo("icon_washerwoman.png")
    assertThat(washerwoman.version).isEqualTo(ScriptToolRole.Version.TROUBLE_BREWING)
    assertThat(washerwoman.isDisabled).isFalse()
    
    val scarletWoman = roles[1]
    assertThat(scarletWoman.id).isEqualTo("scarletwoman")
    assertThat(scarletWoman.name).isEqualTo("Scarlet Woman")
    assertThat(scarletWoman.roleType).isEqualTo(ScriptToolRole.RoleType.MINION)
    assertThat(scarletWoman.isDisabled).isTrue()
  }

  @Test
  fun listFromJson_emptyArray() {
    val json = "[]"
    val roles = ScriptToolRole.listFromJson(Gson(), json)
    assertThat(roles).isEmpty()
  }

  @Test
  fun roleType_allValuesSupported() {
    val allTypes = ScriptToolRole.RoleType.values()
    assertThat(allTypes).hasLength(6)
    assertThat(allTypes).asList().containsExactly(
      ScriptToolRole.RoleType.TOWNSFOLK,
      ScriptToolRole.RoleType.OUTSIDER,
      ScriptToolRole.RoleType.MINION,
      ScriptToolRole.RoleType.DEMON,
      ScriptToolRole.RoleType.TRAVELLER,
      ScriptToolRole.RoleType.FABLED
    )
  }

  @Test
  fun version_allValuesSupported() {
    val allVersions = ScriptToolRole.Version.values()
    assertThat(allVersions).hasLength(6)
    assertThat(allVersions).asList().containsExactly(
      ScriptToolRole.Version.TROUBLE_BREWING,
      ScriptToolRole.Version.BAD_MOON_RISING,
      ScriptToolRole.Version.SECTS_AND_VIOLETS,
      ScriptToolRole.Version.KICKSTARTER_EXPERIMENTAL,
      ScriptToolRole.Version.UNRELEASED_EXPERIMENTAL,
      ScriptToolRole.Version.EXTRAS
    )
  }
}