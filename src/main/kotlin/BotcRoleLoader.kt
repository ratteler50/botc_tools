import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import java.io.IOException
import java.net.URLEncoder
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import org.jsoup.nodes.Document

class BotcRoleLoader {
  private val wikiUrl = "https://wiki.bloodontheclocktower.com/"
  private val wikiApi =
    "${wikiUrl}api.php?action=query&prop=categories|revisions&rvprop=content&format=json&rvslots=*&titles="
  private val wikiImage = "${wikiUrl}Special:FilePath/"
  private val wikiSearch =
    "${wikiUrl}api.php?action=query&list=search&srwhat=title&format=json&srsearch="

  private val gson = Gson()

  private val client = OkHttpClient()

  private fun getUrlContents(url: String): String {
    client.newCall(Request.Builder().url(url).build()).execute().use { response ->
      if (!response.isSuccessful) throw IOException("Unexpected code $response")
      return response.body?.string() ?: throw IOException("Response body is null")
    }
  }

  private fun getFinalUrl(url: String): String {
    val request = Request.Builder().url(url).build()
    client.newCall(request).execute().use { response ->
      return response.request.url.toString()
    }
  }


  suspend fun getRole(role: String): RoleResult = suspendCoroutine { cont ->
    val encodedRole = URLEncoder.encode("*$role*", "UTF-8")
    val searchUrl = "$wikiSearch$encodedRole"

    try {
      val searchResponse = getUrlContents(searchUrl)
      val searchResult = gson.fromJson(searchResponse, WikiSearchResult::class.java)

      val bestMatch = searchResult.query.search.firstOrNull {
        it.wordcount > 100 && !it.snippet.contains(
          "#redirect", ignoreCase = true
        )
      } ?: throw Exception("Role not found")

      val roleUrl = "$wikiApi${URLEncoder.encode(bestMatch.title, "UTF-8")}"
      val urlContents = getUrlContents(roleUrl)
      val roleResult = gson.fromJson(urlContents, WikiPageResult::class.java)

      val page =
        roleResult.query.pages.values.firstOrNull() ?: throw Exception("Role details not found")

      val imageUrl = page.extractImage(wikiImage, this)
      val roleContent = page.extractRoleContent()

      cont.resume(
        RoleResult(
          page.title,
          "$wikiUrl${URLEncoder.encode(bestMatch.title, "UTF-8").replace("+", "_")}",
          imageUrl,
          roleContent,
        )
      )
    } catch (e: Exception) {
      cont.resumeWith(Result.failure(e))
    }
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

    fun extractImage(wikiImageUrl: String, loader: BotcRoleLoader): String {
      return this.revisions.firstOrNull()?.slots?.main?.content?.let { content ->
        Regex("\\[\\[File:(.*?)\\|").find(content)?.groupValues?.get(1)?.let { imageName ->
          val encodedImageUrl = "$wikiImageUrl${URLEncoder.encode(imageName, "UTF-8")}"
          loader.getFinalUrl(encodedImageUrl)
        }
      } ?: ""
    }


    fun extractRoleContent(): RoleContent {
      val content = this.revisions.firstOrNull()?.slots?.main?.content ?: ""
      return RoleContent(
        rawText = content,
        flavourText = extractFlavourText(content),
        abilityText = extractAbilityText(content),
        examples = extractExamplesText(content),
      )
    }


    private fun extractFlavourText(htmlContent: String): String {
      val doc: Document = Jsoup.parse(htmlContent)
      return doc.select("p.flavour").text().trim('"')
    }

    private fun extractAbilityText(htmlContent: String): String {
      val doc: Document = Jsoup.parse(htmlContent)
      val pattern = Regex("==\\s*Summary\\s*==\n(.+)")
      val summaryElement = doc.select("div:containsOwn(== Summary ==)").first()
      val matchResult = pattern.find(summaryElement?.wholeText() ?: "")
      return matchResult?.groups?.get(1)?.value?.trim('"') ?: ""
    }

    private fun extractExamplesText(htmlContent: String): String {
      val doc: Document = Jsoup.parse(htmlContent)
      val examplesElement = doc.select("div:containsOwn(== Examples ==)").first()
      return examplesElement?.wholeText() ?: ""
    }

  }

  data class RoleContent(
    val rawText: String,
    val flavourText: String,
    val abilityText: String,
    val examples: Any,
  )

  data class RoleResult(
    val title: String,
    val wikiUrl: String,
    val imageUrl: String,
    val roleContent: RoleContent,
  )

}

// Usage example
suspend fun main() {
  val loader = BotcRoleLoader()
  val role = loader.getRole("goon")

  println("Title: ${role.title}")
  println("Image URL: ${role.imageUrl}")
  println("wikiUrl: ${role.wikiUrl}")
  println("FlavourText: ${role.roleContent.flavourText}")
  println("AbilityText: ${role.roleContent.abilityText}")
  println("rawText: ${role.roleContent.rawText}")
}
