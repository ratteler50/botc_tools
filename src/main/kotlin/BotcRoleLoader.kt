import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import io.github.oshai.kotlinlogging.KotlinLogging
import java.io.IOException
import java.net.URLEncoder
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import org.jsoup.nodes.Document

private val logger = KotlinLogging.logger {}

private fun String.encodeUrl() = URLEncoder.encode(this, "UTF-8")

private fun OkHttpClient.getUrlContents(url: String): String =
  newCall(Request.Builder().url(url).build()).execute().use { response ->
    if (!response.isSuccessful) throw IOException("Unexpected code $response")
    response.body?.string() ?: throw IOException("Response body is null")
  }

private fun OkHttpClient.getFinalUrl(url: String): String =
  newCall(Request.Builder().url(url).build()).execute().use { response ->
    response.request.url.toString()
  }

class BotcRoleLoader {
  private val wikiUrl = "https://wiki.bloodontheclocktower.com/"
  private val wikiApi =
    "${wikiUrl}api.php?action=query&prop=categories|revisions&rvprop=content&format=json&rvslots=*&titles="
  private val wikiImage = "${wikiUrl}Special:FilePath/"
  private val wikiSearch =
    "${wikiUrl}api.php?action=query&list=search&srwhat=title&format=json&srsearch="

  private val gson = Gson()

  private val client = OkHttpClient()


  fun getRole(role: String): RoleResult {
    val searchUrl = "$wikiSearch${"*$role*".encodeUrl()}"
    val searchResponse = client.getUrlContents(searchUrl)
    val searchResult = gson.fromJson(searchResponse, WikiSearchResult::class.java)

    val bestMatch = searchResult.query.search.firstOrNull {
      it.wordcount > 100 && !it.snippet.contains("#redirect", ignoreCase = true)
    } ?: throw Exception("Models.Role not found")

    val roleUrl = "$wikiApi${bestMatch.title.encodeUrl()}"
    val roleContent = client.getUrlContents(roleUrl).let { it ->
      gson.fromJson(it, WikiPageResult::class.java).query.pages.values.firstOrNull()
        ?: throw Exception("Models.Role details not found")
    }

    return RoleResult(
      title = roleContent.title,
      wikiUrl = "$wikiUrl${bestMatch.title.encodeUrl().replace("+", "_")}",
      imageUrl = roleContent.extractImage(wikiImage, client),
      cotcUrl = roleContent.extractCotc(),
      roleContent = roleContent.extractRoleContent()
    )
  }


  data class WikiSearchResult(
    val batchcomplete: String,
    val query: Query,
  ) {
    data class Query(
      val searchinfo: SearchInfo,
      val search: List<SearchItem>,
    )

    data class SearchInfo(
      val totalhits: Int,
    )

    data class SearchItem(
      val ns: Int,
      val title: String,
      val pageid: Int,
      val size: Int,
      val wordcount: Int,
      val snippet: String,
      val timestamp: String,
    )
  }


  data class WikiPageResult(val query: PageQuery) {
    data class PageQuery(val pages: Map<String, WikiPage>)
  }

  data class WikiPage(
    val title: String,
    val revisions: List<Revision>,
  ) {
    data class Revision(
      val slots: Slot,
    )

    data class Slot(
      @SerializedName("main") val main: MainContent,
    )

    data class MainContent(
      val contentmodel: String,
      val contentformat: String,
      @SerializedName("*") val content: String,  // This represents the actual content
    )

    fun extractImage(wikiImageUrl: String, httpClient: OkHttpClient): String {
      val content = revisions.firstOrNull()?.slots?.main?.content ?: ""
      val imageName = Regex("\\[\\[File:(.*?)\\|").find(content)?.groupValues?.get(1) ?: ""
      return httpClient.getFinalUrl("$wikiImageUrl${imageName.encodeUrl()}")
    }

    fun extractCotc(): String {
      return revisions.firstOrNull()?.slots?.main?.content?.let {
        Regex("(https://anchor.fm.*\\.m4a)").find(it)?.groupValues?.get(1)
      } ?: ""
    }


    fun extractRoleContent(): RoleContent {
      val content = revisions.firstOrNull()?.slots?.main?.content ?: ""
      val doc = Jsoup.parse(content)
      val summarySectionText = extractSectionText(doc, "Summary").lines()
      val quoteRegex =
        "^[\u201C\u201D\u201E\u201F\u2033\u2036\"']+|[\u201C\u201D\u201E\u201F\u2033\u2036\"']+$".toRegex()

      return RoleContent(
        rawText = content,
        flavourText = doc.select("p.flavour").text().trim().replace(quoteRegex, "").trim(),
        abilityText = summarySectionText.firstOrNull()?.trim()?.replace(quoteRegex, "")?.trim()
          ?: "",
        summary = summarySectionText.drop(1).joinToString("\n"),
        examples = extractSectionText(doc, "Examples").lines(),
        howToRun = extractSectionText(doc, "How to Run"),
      )
    }

    private fun extractSectionText(doc: Document, sectionName: String): String {
      val sectionElement = doc.select("div:containsOwn(== $sectionName ==)").first()
      return sectionElement?.wholeText()?.replace("== $sectionName ==", "")
        ?.replace(Regex("[\r\n\t]+"), "\n")
        ?.trim() ?: ""
    }
  }

  data class RoleContent(
    val rawText: String,
    val flavourText: String,
    val abilityText: String,
    val examples: List<String>,
    val howToRun: String,
    val summary: String,
  )

  data class RoleResult(
    val title: String,
    val wikiUrl: String,
    val imageUrl: String,
    val cotcUrl: String,
    val roleContent: RoleContent,
  )

}

// Usage example
fun main() {
  val loader = BotcRoleLoader()
  val role = loader.getRole("kazali")

  logger.info { "Title: ${role.title}" }
  logger.info { "wiki URL: ${role.wikiUrl}" }
  logger.info { "Image URL: ${role.imageUrl}" }
  logger.info { "CotC URL: ${role.cotcUrl}" }
  logger.info { "Flavour Text: ${role.roleContent.flavourText}" }
  logger.info { "Ability Text: ${role.roleContent.abilityText}" }
  logger.info { "Summary: ${role.roleContent.summary}" }
  logger.info { "Examples: ${role.roleContent.examples}" }
  logger.info { "How To Run: ${role.roleContent.howToRun}" }
  logger.info { "Raw Text: ${role.roleContent.rawText}" }
}
