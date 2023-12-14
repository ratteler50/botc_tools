package models

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
                           name = "Models.Role Name",
                           ability = "When this role does something, something happens.")
    val expectedText = "**Models.Role Name** -- When this role does something, something happens."
    assertThat(exampleRole.asTextScriptEntry()).isEqualTo(expectedText)
  }

  @Test
  fun asTextScriptEntry_escapesAsterisk() {
    val exampleRole = Role(id = "ignoredid",
                           name = "Models.Role Name",
                           ability = "Each night* something happens.")
    val expectedText = "**Models.Role Name** -- Each night\\* something happens."
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
                           name = "Models.Role Name")
    assertFailsWith(IllegalStateException::class) { exampleRole.asTextScriptEntry() }
  }


  @Test
  fun asTextScriptClarificationEntry() {
    val exampleRole = Role(id = "ignoredid",
                           name = "Models.Role Name",
                           textGameClarification = "Some text game specific thing players should know.")
    val expectedText = "**Models.Role Name** - Some text game specific thing players should know."
    assertThat(exampleRole.asTextScriptClarificationEntry()).isEqualTo(expectedText)
  }

  @Test
  fun asTextScriptClarificationEntry_missingName_throwsException() {
    val exampleRole = Role(id = "ignoredid",
                           textGameClarification = "Some text game specific thing players should know.")
    assertFailsWith(IllegalStateException::class) { exampleRole.asTextScriptClarificationEntry() }
  }

  @Test
  fun asTextScriptClarificationEntry_noClarification_returnsNull() {
    val exampleRole = Role(id = "ignoredid")
    assertThat(exampleRole.asTextScriptClarificationEntry()).isNull()
  }
}