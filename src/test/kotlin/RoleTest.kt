import com.google.common.truth.Truth.assertThat
import kotlin.test.assertFailsWith
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class RoleTest {

  @Test
  fun asTextScriptEntry() {
    val exampleRole = Role(id = "ignoredid",
                           name = "Role Name",
                           ability = "When this role does something, something happens.")
    val expectedText = "**Role Name** -- When this role does something, something happens."
    assertThat(exampleRole.asTextScriptEntry()).isEqualTo(expectedText)
  }

  @Test
  fun asTextScriptEntry_escapesAsterisk() {
    val exampleRole = Role(id = "ignoredid",
                           name = "Role Name",
                           ability = "Each night* something happens.")
    val expectedText = "**Role Name** -- Each night\\* something happens."
    assertThat(exampleRole.asTextScriptEntry()).isEqualTo(expectedText)
  }

  @Test
  fun asTextScriptEntry_missingName_throwsException() {
    val exampleRole = Role(id = "ignoredid",
                           ability = "When this role does something, something happens.")
    assertFailsWith(IllegalStateException::class) { exampleRole.asTextScriptEntry() }
  }

  @Test
  fun asTextScriptEntry_missingAbility_throwsException() {
    val exampleRole = Role(id = "ignoredid",
                           name = "Role Name")
    assertFailsWith(IllegalStateException::class) { exampleRole.asTextScriptEntry() }
  }
}