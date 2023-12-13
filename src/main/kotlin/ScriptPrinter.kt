import Role.Edition.SPECIAL
import Role.Type.DEMON
import Role.Type.FABLED
import Role.Type.MINION
import Role.Type.OUTSIDER
import Role.Type.TOWNSFOLK
import Role.Type.TRAVELLER
import com.google.common.collect.Table

class ScriptPrinter(
  private val scriptMetadata: Script?,
  private val script: List<Role>,
  private val jinxTable: Table<String, String, Jinx>,
  private val interactionsTable: Table<String, String, Jinx>,
  private val roleMap: Map<String, Role>,
) {
  companion object {
    const val DEFAULT_SCRIPT_TITLE = "INSERT SCRIPT TITLE HERE"
    const val FABLED_DIVIDER = "__Fabled__"
    const val TRAVELLER_DIVIDER = "__Travellers__"
    const val TOWNSFOLK_DIVIDER = "__Townsfolk__"
    const val OUTSIDER_DIVIDER = "__Outsiders__"
    const val MINIONS_DIVIDER = "__Minions__"
    const val DEMONS_DIVIDER = "__Demons__"
    const val JINXES_AND_CLARIFICATIONS_DIVIDER = "**__Jinxes and Clarifications__**"
    const val JINXES_DIVIDER = "__Jinxes__"
    const val CLARIFICATIONS_DIVIDER = "__Clarifications__"
    const val WAKE_ORDER_DIVIDER = "**__WAKE ORDER__**"
    const val FIRST_NIGHT_DIVIDER = "__First Night__"
    const val OTHER_NIGHTS_DIVIDER = "__Other Nights__"
  }

  fun textScriptString(): String {
    return buildString {
      append("**__${scriptMetadata?.name ?: DEFAULT_SCRIPT_TITLE}__**")
      appendLine(scriptMetadata?.author?.let { " by $it" } ?: "")
      appendLine()
      append(buildScriptRoles())
      append(buildTravellers())
      append(buildFabled())
      append(buildJinxesAndClarifications())
      append(buildWakeOrder())
    }
  }

  fun printScript() {
    print(textScriptString())
  }

  private fun buildScriptRoles(): String = buildString {
    appendLine(TOWNSFOLK_DIVIDER)
    getTownsfolkRoles().forEach { appendLine("> - ${it.asTextScriptEntry()}") }
    appendLine()
    appendLine(OUTSIDER_DIVIDER)
    getOutsiderRoles().forEach { appendLine("> - ${it.asTextScriptEntry()}") }
    appendLine()
    appendLine(MINIONS_DIVIDER)
    getMinionRoles().forEach { appendLine("> - ${it.asTextScriptEntry()}") }
    appendLine()
    appendLine(DEMONS_DIVIDER)
    getDemonRoles().forEach { appendLine("> - ${it.asTextScriptEntry()}") }
    appendLine()
  }

  private fun buildFabled(): String {
    val fabled = getFabledRoles()
    if (fabled.isEmpty()) return ""
    return buildString {
      appendLine(FABLED_DIVIDER)
      fabled.forEach { appendLine("> - ${it.asTextScriptEntry()}") }
      appendLine()
    }
  }

  private fun buildTravellers(): String {
    val travellers = getTravellerRoles()
    if (travellers.isEmpty()) return ""
    return buildString {
      appendLine(TRAVELLER_DIVIDER)
      travellers.forEach { appendLine("> - ${it.asTextScriptEntry()}") }
      appendLine()
    }
  }

  private fun buildJinxesAndClarifications(): String {
    val jinxes = buildJinxes()
    val clarifications = buildClarifications()
    if (jinxes.isEmpty() && clarifications.isEmpty()) return ""
    return buildString {
      appendLine(JINXES_AND_CLARIFICATIONS_DIVIDER)
      appendLine()
      appendLine(jinxes)
      appendLine(clarifications)
    }
  }


  private fun buildJinxes(): String {
    val jinxes = getInteractions(jinxTable).toSet()
    if (jinxes.isEmpty()) return ""
    return buildString {
      appendLine(JINXES_DIVIDER)
      jinxes.forEach { appendLine("> - ${it.asTextScriptEntry(roleMap)}") }
    }
  }

  private fun buildClarifications(): String {
    val interactions = getInteractions(interactionsTable).toSet()
    val textClarifications = script.mapNotNull { it.asTextScriptClarificationEntry() }.sorted()
    if (interactions.isEmpty() && textClarifications.isEmpty()) return ""
    return buildString {
      appendLine(CLARIFICATIONS_DIVIDER)
      textClarifications.forEach { appendLine("> - $it") }
      interactions.forEach { appendLine("> - ${it.asTextScriptEntry(roleMap)}") }
    }
  }

  private fun buildWakeOrder(): String = buildString {
    appendLine(WAKE_ORDER_DIVIDER)
    appendLine()
    appendLine(FIRST_NIGHT_DIVIDER)
    getFirstNightWakers().forEach { appendLine("> - $it") }
    appendLine()
    appendLine(OTHER_NIGHTS_DIVIDER)
    getOtherNightWakers().forEach { appendLine("> - $it") }
  }

  private fun getFabledRoles(): List<Role> {
    return script.filter { it.type == FABLED && it.edition != SPECIAL }
  }

  private fun getTravellerRoles(): List<Role> {
    return script.filter { it.type == TRAVELLER && it.edition != SPECIAL }
  }

  private fun getTownsfolkRoles(): List<Role> {
    return script.filter { it.type == TOWNSFOLK && it.edition != SPECIAL }
  }

  private fun getOutsiderRoles(): List<Role> {
    return script.filter { it.type == OUTSIDER && it.edition != SPECIAL }
  }

  private fun getMinionRoles(): List<Role> {
    return script.filter { it.type == MINION && it.edition != SPECIAL }
  }

  private fun getDemonRoles(): List<Role> {
    return script.filter { it.type == DEMON && it.edition != SPECIAL }
  }

  private fun getInteractions(interactionTable: Table<String, String, Jinx>): List<Jinx> {
    val scriptInteractions = arrayListOf<Jinx>()
    for (entry1 in script) {
      for (entry2 in script) {
        val interaction = interactionTable[entry1.id, entry2.id]
        if (interaction != null) scriptInteractions.add(interaction)
      }
    }
    return scriptInteractions.sortedWith(compareBy({ it.role1 }, { it.role2 }))
  }

  private fun getFirstNightWakers(): List<String> {
    return script.filter { (it.firstNight ?: 0) > 0 }.sortedBy { it.firstNight }.map {
      renamePlaceholders(it.name) ?: throw IllegalStateException("Name needed for role $it")
    }
  }


  private fun getOtherNightWakers(): List<String> {
    return script.filter { (it.otherNight ?: 0) > 0 }.sortedBy { it.otherNight }.map {
      renamePlaceholders(it.name) ?: throw IllegalStateException("Name needed for role $it")
    }
  }

  private fun renamePlaceholders(name: String?): String? {
    return when (name) {
      "Demon Info" -> "**DEMON INFO**"
      "Minion Info" -> "**MINION INFO**"
      "Dusk" -> "**DUSK**"
      "Dawn" -> "**DAWN**"
      else -> {
        name
      }
    }
  }
}