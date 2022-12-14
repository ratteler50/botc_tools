import com.google.common.collect.ImmutableTable
import com.google.common.truth.Truth.assertThat
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import org.junit.After
import org.junit.Before
import org.junit.Test


class ScriptPrinterTest {
  private val standardOut = System.out
  private val outputStreamCaptor = ByteArrayOutputStream()

  @Before
  fun setUp() {
    System.setOut(PrintStream(outputStreamCaptor))
  }

  @After
  fun tearDown() {
    System.setOut(standardOut)
  }

  @Test
  fun printScript_empty() {
    val printer = ScriptPrinter(listOf(), ImmutableTable.of(), mapOf())
    printer.printScript()
    assertThat(outputStreamCaptor.toString()).isEqualTo("""
        **__INSERT SCRIPT TITLE HERE__**

        __Townsfolk__

        __Outsiders__

        __Minions__

        __Demons__



        **__WAKE ORDER__**

        __First Night__


        __Other Nights__
        
        
      """.trimIndent())
  }

  @Test
  fun printScript_happyCase() {
    val roleMap = Role.toMap(Role.setFromJson(gson, getRoleJson()))
    val printer = ScriptPrinter(getScriptRoles(roleMap), getJinxTable(), roleMap)
    printer.printScript()
    assertThat(outputStreamCaptor.toString()).isEqualTo("""
    **__INSERT SCRIPT TITLE HERE__**
    
    __Townsfolk__
    > **Snake Charmer** -- Each night, choose an alive player: a chosen Demon swaps characters & alignments with you & is then poisoned.
    > **Amnesiac** -- You do not know what your ability is. Each day, privately guess what it is: you learn how accurate you are.
    > **Magician** -- The Demon thinks you are a Minion. Minions think you are a Demon.
    
    __Outsiders__
    > **Damsel** -- All Minions know you are in play. If a Minion publicly guesses you (once), your team loses.
    > **Barber** -- If you died today or tonight, the Demon may choose 2 players (not another Demon) to swap characters.
    
    __Minions__
    > **Witch** -- Each night, choose a player: if they nominate tomorrow, they die. If just 3 players live, you lose this ability.
    > **Spy** -- Each night, you see the Grimoire. You might register as good & as a Townsfolk or Outsider, even if dead.
    > **Scarlet Woman** -- If there are 5 or more players alive & the Demon dies, you become the Demon. (Travellers don???t count)
    > **Boomdandy** -- If you are executed, all but 3 players die. 1 minute later, the player with the most players pointing at them dies.
    
    __Demons__
    > **Fang Gu** -- Each night\*, choose a player: they die. The 1st Outsider this kills becomes an evil Fang Gu & you die instead. [+1 Outsider]
    > **Vortox** -- Each night\*, choose a player: they die. Townsfolk abilities yield false info. Each day, if no-one is executed, evil wins.
    
    
    **__Jinxes and Clarifications__**
    
    > **Magician** - Some special clarification for Magician specific to playing text game.
    > **Fang Gu / Scarlet Woman** - If the Fang Gu chooses an Outsider and dies, the Scarlet Woman does not become the Fang Gu.
    > **Spy / Damsel** - Only 1 jinxed character can be in play.
    > **Spy / Magician** - When the Spy sees the Grimoire, the Demon and Magician's character tokens are removed.
    
    **__WAKE ORDER__**
    
    __First Night__
    
    > Magician
    > **MINION INFO**
    > **DEMON INFO**
    > Snake Charmer
    > Witch
    > Damsel
    > Amnesiac
    > Spy
    > **DAWN**
    
    __Other Nights__
    
    > **DUSK**
    > Snake Charmer
    > Witch
    > Scarlet Woman
    > Fang Gu
    > Vortox
    > Barber
    > Damsel
    > Amnesiac
    > Spy
    > **DAWN**

    """.trimIndent())
  }

  private fun getRoleJson(): String {
    return """
      [
        {
          "id":"snakecharmer",
          "name":"Snake Charmer",
          "edition":"snv",
          "team":"townsfolk",
          "standardAmyOrder":20,
          "firstNight":20,
          "firstNightReminder":"The Snake Charmer points to a player. If that player is the Demon: swap the Demon and Snake Charmer character and alignments. Wake each player to inform them of their new role and alignment. The new Snake Charmer is poisoned.",
          "otherNight":12,
          "otherNightReminder":"The Snake Charmer points to a player. If that player is the Demon: swap the Demon and Snake Charmer character and alignments. Wake each player to inform them of their new role and alignment. The new Snake Charmer is poisoned.",
          "reminders":[
            "Poisoned"
          ],
          "setup":false,
          "ability":"Each night, choose an alive player: a chosen Demon swaps characters & alignments with you & is then poisoned."
        },
        {
          "id":"amnesiac",
          "name":"Amnesiac",
          "team":"townsfolk",
          "standardAmyOrder":33,
          "firstNight":32,
          "firstNightReminder":"Decide the Amnesiac's entire ability. If the Amnesiac's ability causes them to wake tonight: Wake the Amnesiac and run their ability.",
          "otherNight":48,
          "otherNightReminder":"If the Amnesiac's ability causes them to wake tonight: Wake the Amnesiac and run their ability.",
          "reminders":[
            "?"
          ],
          "setup":false,
          "ability":"You do not know what your ability is. Each day, privately guess what it is: you learn how accurate you are."
        },
        {
          "id":"magician",
          "name":"Magician",
          "team":"townsfolk",
          "standardAmyOrder":52,
          "firstNight":5,
          "setup":false,
          "ability":"The Demon thinks you are a Minion. Minions think you are a Demon.",
          "textGameClarification":"Some special clarification for Magician specific to playing text game."
        },
        {
          "id":"damsel",
          "name":"Damsel",
          "team":"outsider",
          "standardAmyOrder":72,
          "firstNight":31,
          "firstNightReminder":"Wake all the Minions, show them the 'This character selected you' card and the Damsel token.",
          "otherNight":47,
          "otherNightReminder":"If selected by the Huntsman, wake the Damsel, show 'You are' card and a not-in-play Townsfolk token.",
          "reminders":[
            "Guess used"
          ],
          "setup":false,
          "ability":"All Minions know you are in play. If a Minion publicly guesses you (once), your team loses."
        },
        {
          "id":"barber",
          "name":"Barber",
          "edition":"snv",
          "team":"outsider",
          "standardAmyOrder":77,
          "otherNight":41,
          "otherNightReminder":"If the Barber died today: Wake the Demon. Show the 'This character selected you' card, then Barber token. The Demon either shows a 'no' head signal, or points to 2 players. If they chose players: Swap the character tokens. Wake each player. Show 'You are', then their new character token.",
          "reminders":[
            "Haircuts tonight"
          ],
          "setup":false,
          "ability":"If you died today or tonight, the Demon may choose 2 players (not another Demon) to swap characters."
        },
        {
          "id":"witch",
          "name":"Witch",
          "edition":"snv",
          "team":"minion",
          "standardAmyOrder":84,
          "firstNight":24,
          "firstNightReminder":"The Witch points to a player. If that player nominates tomorrow they die immediately.",
          "otherNight":15,
          "otherNightReminder":"If there are 4 or more players alive: The Witch points to a player. If that player nominates tomorrow they die immediately.",
          "reminders":[
            "Cursed"
          ],
          "setup":false,
          "ability":"Each night, choose a player: if they nominate tomorrow, they die. If just 3 players live, you lose this ability."
        },
        {
          "id":"boomdandy",
          "name":"Boomdandy",
          "team":"minion",
          "standardAmyOrder":97,
          "setup":false,
          "ability":"If you are executed, all but 3 players die. 1 minute later, the player with the most players pointing at them dies."
        },
        {
          "id":"fanggu",
          "name":"Fang Gu",
          "edition":"snv",
          "team":"demon",
          "standardAmyOrder":109,
          "otherNight":30,
          "otherNightReminder":"The Fang Gu points to a player. That player dies. Or, if that player was an Outsider and there are no other Fang Gu in play: The Fang Gu dies instead of the chosen player. The chosen player is now an evil Fang Gu. Wake the new Fang Gu. Show the 'You are' card, then the Fang Gu token. Show the 'You are' card, then the thumb-down 'evil' hand sign.",
          "reminders":[
            "Dead",
            "Once"
          ],
          "setup":true,
          "ability":"Each night*, choose a player: they die. The 1st Outsider this kills becomes an evil Fang Gu & you die instead. [+1 Outsider]"
        },
        {
          "id":"vortox",
          "name":"Vortox",
          "edition":"snv",
          "team":"demon",
          "standardAmyOrder":110,
          "otherNight":32,
          "otherNightReminder":"The Vortox points to a player. That player dies.",
          "reminders":[
            "Dead"
          ],
          "setup":false,
          "ability":"Each night*, choose a player: they die. Townsfolk abilities yield false info. Each day, if no-one is executed, evil wins."
        },
        {
          "id": "spy",
          "name": "Spy",
          "edition": "tb",
          "team": "minion",
          "standardAmyOrder": 85,
          "firstNight": 49,
          "firstNightReminder": "Show the Grimoire to the Spy for as long as they need.",
          "otherNight": 69,
          "otherNightReminder": "Show the Grimoire to the Spy for as long as they need.",
          "setup": false,
          "ability": "Each night, you see the Grimoire. You might register as good & as a Townsfolk or Outsider, even if dead."
        },
        {
          "id": "scarletwoman",
          "name": "Scarlet Woman",
          "edition": "tb",
          "team": "minion",
          "standardAmyOrder": 94,
          "otherNight": 20,
          "otherNightReminder": "If the Scarlet Woman became the Demon today: Show the 'You are' card, then the demon token.",
          "reminders": [
            "Demon"
          ],
          "setup": false,
          "ability": "If there are 5 or more players alive & the Demon dies, you become the Demon. (Travellers don???t count)"
        },
        {
          "id": "minion",
          "name": "**MINION INFO**",
          "firstNight": 6
        },
        {
          "id": "demon",
          "name": "**DEMON INFO**",
          "firstNight": 9
        },
        {
          "id": "dusk",
          "name": "**DUSK**",
          "otherNight": 1
        },
        {
          "id": "dawn",
          "name": "**DAWN**",
          "firstNight": 53,
          "otherNight": 73
        }
      ]
    """.trimIndent()
  }

  private fun getScriptRoles(roleMap: Map<String, Role>): List<Role> {
    val json =
      """[{"id":"vortox"},{"id":"amnesiac"},{"id":"snake_charmer"},{"id":"spy"},{"id":"damsel"},{"id":"barber"},{"id":"boomdandy"},{"id":"witch"},{"id":"magician"},{"id":"scarlet_woman"},{"id":"fang_gu"}]"""
    val charList = Script.getRolesOnScript(gson, json)
    return charList.map { checkNotNull(roleMap[it]) {"Couldn't find $it in $roleMap"} }.sortedBy { it.standardAmyOrder }
  }

  private fun getJinxTable(): ImmutableTable<String, String, Jinx> {
    val json = """[
      |{"role1": "fanggu", "role2": "scarletwoman", "reason": "If the Fang Gu chooses an Outsider and dies, the Scarlet Woman does not become the Fang Gu."},
      |{"role1": "spy", "role2": "damsel", "reason": "Only 1 jinxed character can be in play."},
      |{"role1": "spy", "role2": "magician", "reason": "When the Spy sees the Grimoire, the Demon and Magician's character tokens are removed."}
      |]""".trimMargin()
    val jinxes = Jinx.listFromJson(gson, json)
    return Jinx.toTable(jinxes)
  }
}