import java.util.Scanner

class AppLauncher
{
    private var currentSession: GameSession? = null

    private var players: List<Player> = emptyList()
    private var activePlayer: Player? = null
    private val scanner = Scanner(System.`in`)

    fun startNewGame()
    {
        println("Count of players is (2-4): ")

        if (!scanner.hasNextInt()) {
            println("Write a number!!!")
            return
        }

        val playerCount = scanner.nextInt()

        if (playerCount !in 2..4) {
            println("Maximum is 4 players. Kill unnecessary bug of bones")
            return
        }

        players = List(playerCount) { i ->
            Player(name = "Player ${i + 1}", initialHealth = 4)
        }

        val session = GameSession(players)
        currentSession = session

        session.onEvent = { event ->
            handleGameEvent(event)
        }

        println("=== GAME STARTED ===")

        session.startRound(live = null, blank = null)

        // CCYYCCLLEE
        while (session.status != SessionStatus.GAME_OVER) {
            val currentPlayer = activePlayer ?: players[0]

            println("\n Your turn, ${currentPlayer.name}")
            println("HP: ${currentPlayer.health} | Inventory: ${currentPlayer.inventory.map { it.name }}")
            println("Commands: 's' -- self shot, 'o' -- shot enemy, 'i [number]' -- use item, 'q' -- quit")

            val input = scanner.next()

            when (input) {
                "s" -> session.shot(currentPlayer)
                "o" -> {
                    println("Choose a target")
                    val targetIdx = scanner.nextInt() - 1
                    if (targetIdx in players.indices) {
                        session.shot(players[targetIdx])
                    } else {
                        println("Wrong number!!!")
                    }
                }
                "i" -> {
                    if (currentPlayer.inventory.isEmpty()) {
                        println("NO ITEMS? :(")
                    } else {
                        println("Peek item (0-${currentPlayer.inventory.size - 1}):")
                        val itemIdx = scanner.nextInt()
                        val item = currentPlayer.inventory.getOrNull(itemIdx)
                        if (item != null) {
                            val target = players.firstOrNull { it != currentPlayer && it.health > 0 }
                            session.useItem(currentPlayer, item, target)
                        }
                    }
                }

                "q" -> return
                else -> println("ANDEFAIND BIHAVIOR STAK OVERFLOU ANRESOLVET REFERENS")
            }
        }
    }

    private fun handleGameEvent(event: GameEvent)
    {
        when (event) {
            is GameEvent.TurnChanged -> {
                activePlayer = players.find { it.name == event.newActivePlayerName }
                println("Now is ${activePlayer?.name}'s turn")
            }

            is GameEvent.ShotFired -> {
                val typeStr = if (event.type == AmmoType.LIVE) "LIVE MUAHAHAHA" else "BLANK MHE"
                println("Target: ${event.targetName} | Type: $typeStr")
            }

            is GameEvent.ItemUsed -> {
                println("${event.playerName} used ${event.itemName}")
            }

            is GameEvent.InfoMessage -> {
                println("Yo! ${event.text}")
            }

            is GameEvent.ActionLog -> {
                println("Log: ${event.text}")
            }

            is GameEvent.GameOver -> {
                println("GAME OVER HEHEHEHA")
            }
        }
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>)
        {
            println("Buckshot roulette Admin Tool Started")

            val launcher = AppLauncher()
            launcher.startNewGame()
        }
    }
}
