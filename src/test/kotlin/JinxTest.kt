import com.google.common.truth.Truth.assertThat
import com.google.gson.Gson
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class JinxTest {

  @Test
  fun asTextScriptEntry() {
    val exampleJinx = Jinx("fanggu",
                           "scarletwoman",
                           "If the Fang Gu Chooses an Outsider and dies, the Scarlet Woman does not become the Fang Gu.")
    val expectedText =
      "**Fang Gu / Scarlet Woman** - If the Fang Gu Chooses an Outsider and dies, the Scarlet Woman does not become the Fang Gu."
    val nameMap = mapOf("fanggu" to Role(id = "fanggu", name = "Fang Gu"),
                        "scarletwoman" to Role(id = "scarletwoman", name = "Scarlet Woman"))
    assertThat(exampleJinx.asTextScriptEntry(nameMap)).isEqualTo(expectedText)
  }

  @Test
  fun jinxList_parsesExpected() {
    val json = """[{
      "role1": "spy",
      "role2": "magician",
      "reason": "When the Spy sees the Grimoire, the Demon and Magician's character tokens are removed."
    },
    {
      "role1": "spy",
      "role2": "poppygrower",
      "reason": "If the Poppy Grower is in play, the Spy does not see the Grimoire until the Poppy Grower dies."
    },
    {
      "role1": "widow",
      "role2": "alchemist",
      "reason": "The Alchemist can not have the Widow ability."
    }]"""

    val actualValue = Jinx.listFromJson(Gson(), json)
    assertThat(actualValue).containsExactly(Jinx("spy",
                                                 "magician",
                                                 "When the Spy sees the Grimoire, the Demon and Magician's character tokens are removed."),
                                            Jinx("spy",
                                                 "poppygrower",
                                                 "If the Poppy Grower is in play, the Spy does not see the Grimoire until the Poppy Grower dies."),
                                            Jinx("widow",
                                                 "alchemist",
                                                 "The Alchemist can not have the Widow ability."))
  }

  @Test
  fun jinxList_rawFormat_parsesExpected() {
    val json = """[{
    "id": "Spy",
    "jinx": [
      {
        "id": "Magician",
        "reason": "When the Spy sees the Grimoire, the Demon and Magician's character tokens are removed."
      },
      {
        "id": "Poppy Grower",
        "reason": "If the Poppy Grower is in play, the Spy does not see the Grimoire until the Poppy Grower dies."
      }
    ]
  },
  {
    "id": "Widow",
    "jinx": [
      {
        "id": "Alchemist",
        "reason": "The Alchemist can not have the Widow ability."
      }
    ]
  }]"""

    val actualValue = Jinx.listFromRawJson(Gson(), json)
    assertThat(actualValue).containsExactly(Jinx("Spy",
                                                 "Magician",
                                                 "When the Spy sees the Grimoire, the Demon and Magician's character tokens are removed."),
                                            Jinx("Spy",
                                                 "Poppy Grower",
                                                 "If the Poppy Grower is in play, the Spy does not see the Grimoire until the Poppy Grower dies."),
                                            Jinx("Widow",
                                                 "Alchemist",
                                                 "The Alchemist can not have the Widow ability."))
  }

  @Test
  fun jinxTable_containsExpectedEntries() {
    val jinxList = listOf(Jinx("spy",
                               "magician",
                               "When the Spy sees the Grimoire, the Demon and Magician's character tokens are removed."),
                          Jinx("spy",
                               "poppygrower",
                               "If the Poppy Grower is in play, the Spy does not see the Grimoire until the Poppy Grower dies."),
                          Jinx("widow",
                               "alchemist",
                               "The Alchemist can not have the Widow ability."))
    val jinxTable = Jinx.toTable(jinxList)
    assertThat(jinxTable).contains("spy", "magician")
    assertThat(jinxTable).contains("magician", "spy")
    assertThat(jinxTable).contains("spy", "poppygrower")
    assertThat(jinxTable).contains("poppygrower", "spy")
    assertThat(jinxTable).contains("widow", "alchemist")
    assertThat(jinxTable).contains("alchemist", "widow")
    assertThat(jinxTable).doesNotContain("spy", "alchemist")
    assertThat(jinxTable).doesNotContain("alchemist", "spy")
    assertThat(jinxTable).doesNotContain("widow", "poppygrower")
  }
}