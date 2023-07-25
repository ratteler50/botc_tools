import com.google.common.collect.Table

class ScriptPrinter(
  private val scriptMetadata: Script?,
  private val script: List<Role>,
  private val jinxTable: Table<String, String, Jinx>,
  private val roleMap: Map<String, Role>,
) {
  companion object {
    const val DEFAULT_SCRIPT_TITLE = "INSERT SCRIPT TITLE HERE"
    const val FABLED_DIVIDER = "__Fabled__"
    const val TOWNSFOLK_DIVIDER = "__Townsfolk__"
    const val OUTSIDER_DIVIDER = "__Outsiders__"
    const val MINIONS_DIVIDER = "__Minions__"
    const val DEMONS_DIVIDER = "__Demons__"
    const val JINXES_DIVIDER = "**__Jinxes and Clarifications__**"
    const val WAKE_ORDER_DIVIDER = "**__WAKE ORDER__**"
    const val FIRST_NIGHT_DIVIDER = "__First Night__"
    const val OTHER_NIGHTS_DIVIDER = "__Other Nights__"
  }

  fun textScriptString(): String {
    return buildString {
      append("**__${scriptMetadata?.name ?: DEFAULT_SCRIPT_TITLE}__**")
      appendLine(scriptMetadata?.author?.let { " by $it" } ?: "")
      appendLine()
      append(buildFabled())
      append(buildScriptRoles())
      appendLine()
      append(buildJinxes())
      appendLine()
      append(buildWakeOrder())
    }
  }

  fun printScript() {
    print(textScriptString())
  }

  private fun buildScriptRoles(): String =
    buildString {
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

  private fun buildJinxes(): String {
    val jinxes = getJinxes().toSet()
    val textClarifications = script.mapNotNull { it.asTextScriptClarificationEntry() }
    if (jinxes.isEmpty() && textClarifications.isEmpty()) return ""
    return buildString {
      appendLine(JINXES_DIVIDER)
      appendLine()
      textClarifications.forEach { appendLine("> - $it") }
      jinxes.forEach { appendLine("> - ${it.asTextScriptEntry(roleMap)}") }
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
    return script.filter { it.type == Role.Type.FABLED }
  }

  private fun getTownsfolkRoles(): List<Role> {
    return script.filter { it.type == Role.Type.TOWNSFOLK }
  }

  private fun getOutsiderRoles(): List<Role> {
    return script.filter { it.type == Role.Type.OUTSIDER }
  }

  private fun getMinionRoles(): List<Role> {
    return script.filter { it.type == Role.Type.MINION }
  }

  private fun getDemonRoles(): List<Role> {
    return script.filter { it.type == Role.Type.DEMON }
  }

  private fun getJinxes(): List<Jinx> {
    val scriptJinxes = arrayListOf<Jinx>()
    for (entry1 in script) {
      for (entry2 in script) {
        val jinx = jinxTable[entry1.id, entry2.id]
        if (jinx != null) scriptJinxes.add(jinx)
      }
    }
    return scriptJinxes.sortedWith(compareBy({ it.role1 }, { it.role2 }))
  }

  private fun getFirstNightWakers(): List<String> {
    return script.filterNot { it.firstNight == null }.sortedBy { it.firstNight }
      .map { it.name ?: throw IllegalStateException("Name needed for role $it") }
  }


  private fun getOtherNightWakers(): List<String> {
    return script.filterNot { it.otherNight == null }.sortedBy { it.otherNight }
      .map { it.name ?: throw IllegalStateException("Name needed for role $it") }
  }
}