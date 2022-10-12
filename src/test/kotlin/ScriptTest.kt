import com.google.common.truth.Truth.assertThat
import com.google.gson.Gson
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class ScriptTest {
  @Test
  fun scriptCharListFromJsonMakesLowercase() {
    val inputScript =
      """[{"id":"general"},{"id":"GOSSIP"},{"id":"savant"},{"id":"amnesiac"},{"id":"juggler"},{"id":"Barber"},{"id":"lunatic"},{"id":"WITCH"},{"id":"fearmonger"},{"id":"boomdandy"},{"id":"pukka"},{"id":"zombuul"}]"""
    assertThat(Script.getRolesOnScript(Gson(), inputScript)).containsExactly(
      "general",
      "gossip",
      "savant",
      "amnesiac",
      "juggler",
      "barber",
      "lunatic",
      "witch",
      "fearmonger",
      "boomdandy",
      "pukka",
      "zombuul",
      "minion",
      "demon",
      "dusk",
      "dawn",
    )
  }

  @Test
  fun scriptCharListRemovesSpecialCharacters() {
    val inputScript =
      """[{"id":"fortune teller"},{"id":"town_crier"},{"id":"night watchman"},{"id":"poppy_grower"},{"id":"lil' monsta"},{"id":"fang_gu"}]"""
    assertThat(Script.getRolesOnScript(Gson(), inputScript)).containsExactly(
      "fortuneteller",
      "towncrier",
      "nightwatchman",
      "poppygrower",
      "lilmonsta",
      "fanggu",
      "minion",
      "demon",
      "dusk",
      "dawn",
    )
  }

  @Test
  fun scriptCharListRemovesMetaEntry() {
    val inputScript =
      """[{"id": "_meta", "logo": "https://i.postimg.cc/CKxv6qTn/whoami.png", "name": "Who Am I", "author": "Dae"}, {"id": "pixie"}, {"id": "balloonist"}, {"id": "dreamer"}]"""
    assertThat(Script.getRolesOnScript(Gson(), inputScript)).containsExactly(
      "pixie",
      "balloonist",
      "dreamer",
      "minion",
      "demon",
      "dusk",
      "dawn",
    )
  }
}