import com.google.common.collect.Table

class ScriptPrinter(
  val script: List<Role>,
  val jinxTable: Table<String, String, Jinx>,
  val roleMap: Map<String, Role>,
) {
  companion object {
    const val SCRIPT_TITLE = "**__INSERT SCRIPT TITLE HERE__**"
    const val TOWNSFOLK_DIVIDER = "__Townsfolk__"
    const val OUTSIDER_DIVIDER = "__Outsiders__"
    const val MINIONS_DIVIDER = "__Minions__"
    const val DEMONS_DIVIDER = "__Demons__"
    const val JINXES_DIVIDER = "**__Jinxes and Clarifications__**"
    const val WAKE_ORDER_DIVIDER = "**__WAKE ORDER__**"
    const val FIRST_NIGHT_DIVIDER = "__First Night__"
    const val OTHER_NIGHTS_DIVIDER = "__Other Nights__"
  }

  fun printScript() {
    println(SCRIPT_TITLE)
    println()
    printScriptRoles()
    println()
    printJinxes()
    println()
    printWakeOrder()

  }

  private fun printScriptRoles() {
    println(TOWNSFOLK_DIVIDER)
    getTownsfolkRoles().forEach { println("> ${it.asTextScriptEntry()}") }
    println()
    println(OUTSIDER_DIVIDER)
    getOutsiderRoles().forEach { println("> ${it.asTextScriptEntry()}") }
    println()
    println(MINIONS_DIVIDER)
    getMinionRoles().forEach { println("> ${it.asTextScriptEntry()}") }
    println()
    println(DEMONS_DIVIDER)
    getDemonRoles().forEach { println("> ${it.asTextScriptEntry()}") }
    println()
  }


  private fun printJinxes() {
    val jinxes = getJinxes().toSet()
    if (jinxes.isEmpty()) return
    println(JINXES_DIVIDER)
    println()
    jinxes.forEach { println("> ${it.asTextScriptEntry(roleMap)}") }
  }

  private fun printWakeOrder() {
    println(WAKE_ORDER_DIVIDER)
    println()
    println(FIRST_NIGHT_DIVIDER)
    println()
    getFirstNightWakers().forEach { println("> $it") }
    println()
    println(OTHER_NIGHTS_DIVIDER)
    println()
    getOtherNightWakers().forEach { println("> $it") }
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