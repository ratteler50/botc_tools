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
    val printer = ScriptPrinter(null, listOf(), ImmutableTable.of(), ImmutableTable.of(), mapOf())
    printer.printScript()
    assertThat(outputStreamCaptor.toString()).isEqualTo(
      """
        **__INSERT SCRIPT TITLE HERE__**

        __Townsfolk__

        __Outsiders__

        __Minions__

        __Demons__

        **__WAKE ORDER__**

        __First Night__

        __Other Nights__
        
      """.trimIndent()
    )
  }

  @Test
  fun printScript_happyCase() {
    val roleMap = Role.listFromJson(gson, getRoleJson()).associateBy { it.id }
    val printer = ScriptPrinter(
      Script(id = "_meta", name = "EXAMPLE_SCRIPT_NAME", author = "somebody"),
      getScriptRoles(roleMap),
      getJinxTable(),
      getClarificationTable(),
      roleMap
    )
    printer.printScript()
    assertThat(outputStreamCaptor.toString()).isEqualTo(
      """
    **__EXAMPLE_SCRIPT_NAME__** by somebody
    
    __Townsfolk__
    > - **Snake Charmer** -- Each night, choose an alive player: a chosen Demon swaps characters & alignments with you & is then poisoned.
    > - **Monk** -- Each night\*, choose a player (not yourself): they are safe from the Demon tonight.
    > - **Amnesiac** -- You do not know what your ability is. Each day, privately guess what it is: you learn how accurate you are.
    > - **Magician** -- The Demon thinks you are a Minion. Minions think you are a Demon.
    
    __Outsiders__
    > - **Damsel** -- All Minions know you are in play. If a Minion publicly guesses you (once), your team loses.
    > - **Barber** -- If you died today or tonight, the Demon may choose 2 players (not another Demon) to swap characters.
    
    __Minions__
    > - **Witch** -- Each night, choose a player: if they nominate tomorrow, they die. If just 3 players live, you lose this ability.
    > - **Spy** -- Each night, you see the Grimoire. You might register as good & as a Townsfolk or Outsider, even if dead.
    > - **Scarlet Woman** -- If there are 5 or more players alive & the Demon dies, you become the Demon. (Travellers don’t count)
    > - **Boomdandy** -- If you are executed, all but 3 players die. 1 minute later, the player with the most players pointing at them dies.
    
    __Demons__
    > - **Fang Gu** -- Each night\*, choose a player: they die. The 1st Outsider this kills becomes an evil Fang Gu & you die instead. [+1 Outsider]
    > - **Vortox** -- Each night\*, choose a player: they die. Townsfolk abilities yield false info. Each day, if no-one is executed, evil wins.
    
    __Travellers__
    > - **Gangster** -- Once per day, you may choose to kill an alive neighbour, if your other alive neighbour agrees.
    
    __Fabled__
    > - **Spirit of Ivory** -- There can't be more than 1 extra evil player.
    
    **__Jinxes and Clarifications__**
    
    __Jinxes__
    > - **Fang Gu / Scarlet Woman** - If the Fang Gu chooses an Outsider and dies, the Scarlet Woman does not become the Fang Gu.
    > - **Spy / Damsel** - Only 1 jinxed character can be in play.
    > - **Spy / Magician** - When the Spy sees the Grimoire, the Demon and Magician's character tokens are removed.
    
    __Clarifications__
    > - **Snake Charmer** - On the first night, the snake charmer will pick before evil info.
    > - **Fang Gu / Monk** - An Outsider chosen by the Monk cannot be jumped to.
    > - **Vortox / Monk** - A player protected by the Monk would get correct information in a Vortox game.
    
    **__WAKE ORDER__**
    
    __First Night__
    > - Magician
    > - **MINION INFO**
    > - **DEMON INFO**
    > - Snake Charmer
    > - Witch
    > - Damsel
    > - Amnesiac
    > - Spy
    > - **DAWN**
    
    __Other Nights__
    > - **DUSK**
    > - Snake Charmer
    > - Monk
    > - Witch
    > - Scarlet Woman
    > - Fang Gu
    > - Vortox
    > - Barber
    > - Damsel
    > - Amnesiac
    > - Spy
    > - **DAWN**

    """.trimIndent()
    )
  }

  private fun getRoleJson(): String {
    return """
      [
        {
          "id": "spiritofivory",
          "name": "Spirit of Ivory",
          "team": "fabled",
          "reminders": [
            "No extra evil"
          ],
          "ability": "There can't be more than 1 extra evil player."
        },
       {
        "id": "gangster",
        "name": "Gangster",
        "team": "traveler",
        "setup": false,
        "ability": "Once per day, you may choose to kill an alive neighbour, if your other alive neighbour agrees.",
        "flavour": "I like your shoes. It would be such a shame if you had a little accident, and they got ruined. Now that you mention it, I like your cufflinks too.",
        "urls": {
          "token": "/token/gangster.png",
          "icon": "/icon/gangster.png",
          "wiki": "https://wiki.bloodontheclocktower.com/Gangster"
          }
       },
        {
          "id": "monk",
          "name": "Monk",
          "edition": "tb",
          "team": "townsfolk",
          "standardAmyOrder": 32,
          "otherNight": 12,
          "otherNightReminder": "The previously protected player is no longer protected. The Monk points to a player not themself. Mark that player 'Protected'.",
          "reminders": [
            "Protected"
          ],
          "setup": false,
          "ability": "Each night*, choose a player (not yourself): they are safe from the Demon tonight."
        },
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
          "ability":"Each night, choose an alive player: a chosen Demon swaps characters & alignments with you & is then poisoned.",
          "textGameClarification": "On the first night, the snake charmer will pick before evil info."
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
          "ability":"The Demon thinks you are a Minion. Minions think you are a Demon."
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
          "ability": "If there are 5 or more players alive & the Demon dies, you become the Demon. (Travellers don’t count)"
        },
        {
          "id": "minioninfo",
          "name": "Minion Info",
          "firstNight": 6
        },
        {
          "id": "demoninfo",
          "name": "Demon Info",
          "firstNight": 9
        },
        {
          "id": "dusk",
          "name": "Dusk",
          "otherNight": 1
        },
        {
          "id": "dawn",
          "name": "Dawn",
          "firstNight": 53,
          "otherNight": 73
        }
      ]
    """.trimIndent()
  }

  private fun getScriptRoles(roleMap: Map<String, Role>): List<Role> {
    val json =
      """[{"id": "spiritofivory"}, "gangster", {"id":"vortox"},{"id":"amnesiac"},{"id":"snake_charmer"},{"id":"spy"},{"id":"damsel"},{"id":"barber"},{"id":"boomdandy"},{"id":"witch"},{"id":"magician"},{"id":"scarlet_woman"},{"id":"fang_gu"},"monk"]"""
    val charList = Script.getRolesOnScript(gson, json)
    return charList.map { checkNotNull(roleMap[it]) { "Couldn't find $it in $roleMap" } }
      .sortedBy { it.standardAmyOrder }
  }

  private fun getJinxTable(): ImmutableTable<String, String, Jinx> {
    val json = """[
      |{"id": "fanggu", "jinxes": [{"id": "scarletwoman", "reason": "If the Fang Gu chooses an Outsider and dies, the Scarlet Woman does not become the Fang Gu."}]},
      |{"id": "spy", "jinxes": [{"id": "damsel", "reason": "Only 1 jinxed character can be in play."},
                               |{"id": "magician", "reason": "When the Spy sees the Grimoire, the Demon and Magician's character tokens are removed."}]}
      |]""".trimMargin()
    val jinxes = Jinx.listFromJson(gson, json)
    return Jinx.toTable(jinxes)
  }

  private fun getClarificationTable(): ImmutableTable<String, String, Jinx> {
    val json = """[
      |{"id": "Fang Gu", "jinxes": [{"id": "Monk", "reason": "An Outsider chosen by the Monk cannot be jumped to."}]},
      |{"id": "Vortox", "jinxes": [{"id": "Monk", "reason": "A player protected by the Monk would get correct information in a Vortox game."}]}
      |]""".trimMargin()
    val interactions = Jinx.listFromJson(gson, json)
    return Jinx.toTable(interactions)
  }
}