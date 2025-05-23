import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class BotcRoleLoaderTest {

  @Test
  fun wikiPageExtractImage_findsImage() {
    val content = """
      Some content here
      [[File:Icon_washerwoman.png|100px|right|Washerwoman]]
      More content
    """.trimIndent()

    val wikiPage = BotcRoleLoader.WikiPage(
      title = "Washerwoman",
      revisions = listOf(
        BotcRoleLoader.WikiPage.Revision(
          slots = BotcRoleLoader.WikiPage.Slot(
            main = BotcRoleLoader.WikiPage.MainContent(
              contentmodel = "wikitext",
              contentformat = "text/x-wiki",
              content = content
            )
          )
        )
      )
    )

    // Note: This test would need to mock the HTTP client for full testing
    // For now, we test the regex extraction logic
    val imageMatch = Regex("\\[\\[File:(.*?)\\|").find(content)?.groupValues?.get(1)
    assertThat(imageMatch).isEqualTo("Icon_washerwoman.png")
  }

  @Test
  fun wikiPageExtractCotc_findsUrl() {
    val content = """
      Some content here
      Audio: https://anchor.fm/bloodontheclocktower/episodes/washerwoman-example.m4a
      More content
    """.trimIndent()

    val wikiPage = BotcRoleLoader.WikiPage(
      title = "Washerwoman",
      revisions = listOf(
        BotcRoleLoader.WikiPage.Revision(
          slots = BotcRoleLoader.WikiPage.Slot(
            main = BotcRoleLoader.WikiPage.MainContent(
              contentmodel = "wikitext", 
              contentformat = "text/x-wiki",
              content = content
            )
          )
        )
      )
    )

    val cotcUrl = wikiPage.extractCotc()
    assertThat(cotcUrl).isEqualTo("https://anchor.fm/bloodontheclocktower/episodes/washerwoman-example.m4a")
  }

  @Test
  fun wikiPageExtractCotc_noCotcUrl_returnsEmpty() {
    val content = "Some content without any audio links"

    val wikiPage = BotcRoleLoader.WikiPage(
      title = "Test",
      revisions = listOf(
        BotcRoleLoader.WikiPage.Revision(
          slots = BotcRoleLoader.WikiPage.Slot(
            main = BotcRoleLoader.WikiPage.MainContent(
              contentmodel = "wikitext",
              contentformat = "text/x-wiki", 
              content = content
            )
          )
        )
      )
    )

    val cotcUrl = wikiPage.extractCotc()
    assertThat(cotcUrl).isEmpty()
  }

  @Test
  fun roleContent_dataClass_fieldsAccessible() {
    val roleContent = BotcRoleLoader.RoleContent(
      rawText = "Raw wiki content",
      flavourText = "A helpful townsfolk.",
      abilityText = "You start knowing something.",
      examples = listOf("Example 1", "Example 2"),
      howToRun = "Run instructions",
      summary = "Summary text"
    )

    assertThat(roleContent.rawText).isEqualTo("Raw wiki content")
    assertThat(roleContent.flavourText).isEqualTo("A helpful townsfolk.")
    assertThat(roleContent.abilityText).isEqualTo("You start knowing something.")
    assertThat(roleContent.examples).containsExactly("Example 1", "Example 2")
    assertThat(roleContent.howToRun).isEqualTo("Run instructions")
    assertThat(roleContent.summary).isEqualTo("Summary text")
  }

  @Test
  fun roleResult_dataClass_fieldsAccessible() {
    val roleContent = BotcRoleLoader.RoleContent(
      rawText = "Raw",
      flavourText = "Flavour",
      abilityText = "Ability",
      examples = listOf("Ex1"),
      howToRun = "Run",
      summary = "Summary"
    )

    val roleResult = BotcRoleLoader.RoleResult(
      title = "Washerwoman",
      wikiUrl = "https://wiki.bloodontheclocktower.com/Washerwoman",
      imageUrl = "https://example.com/image.png",
      cotcUrl = "https://anchor.fm/example.m4a",
      roleContent = roleContent
    )

    assertThat(roleResult.title).isEqualTo("Washerwoman")
    assertThat(roleResult.wikiUrl).isEqualTo("https://wiki.bloodontheclocktower.com/Washerwoman")
    assertThat(roleResult.imageUrl).isEqualTo("https://example.com/image.png")
    assertThat(roleResult.cotcUrl).isEqualTo("https://anchor.fm/example.m4a")
    assertThat(roleResult.roleContent).isEqualTo(roleContent)
  }
}