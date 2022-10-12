import com.google.common.truth.Truth.assertThat
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
}