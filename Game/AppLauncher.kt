package game

import game.engine.GameSession
import game.entities.Player
import game.entities.Item
import game.entities.TargetItem
import game.models.AmmoType
import game.models.GameEvent
import game.models.MAX_PLAYERS_COUNT
import game.models.MIN_PLAYERS_COUNT
import game.models.PLAYER_HEALTH
import game.models.SessionStatus
import java.util.Scanner

class AppLauncher
{
    private var currentSession: GameSession? = null

    private var players: List<Player> = emptyList()
    private var activePlayer: Player? = null
    private val scanner = Scanner(System.`in`)

    fun startNewGame()
    {
        println("Count of players is (${MIN_PLAYERS_COUNT} - ${MAX_PLAYERS_COUNT}): ")

        if (!scanner.hasNextInt()) {
            println("Write a number!!!")
            return
        }

        val playerCount = scanner.nextInt()

        if (playerCount !in MIN_PLAYERS_COUNT..MAX_PLAYERS_COUNT) {
            println("Maximum is ${MAX_PLAYERS_COUNT}, minimum is $MIN_PLAYERS_COUNT players. Kill unnecessary bug of bones")
            return
        }

        players = List(playerCount) { i ->
            Player(name = "Player ${i + 1}", initialHealth = PLAYER_HEALTH)
        }

        val session = GameSession(players)
        currentSession = session

        session.onEvent = { event ->
            handleGameEvent(event)
        }

        println("=== GAME STARTED ===")

        session.startRound(live = null, blank = null)

        while (session.status != SessionStatus.GAME_OVER) {
            val currentPlayer = activePlayer ?: players[0]

            println("\n Your turn, ${currentPlayer.name}")

            // ИСПРАВЛЕНИЕ ОШИБКИ 56: Безопасно приводим Any к интерфейсам, чтобы прочитать .name
            val inventoryNames = currentPlayer.inventory.map {
                when (it) {
                    is Item -> it.name
                    is TargetItem -> it.name
                    else -> "Unknown"
                }
            }
            println("HP: ${currentPlayer.health} | Inventory: $inventoryNames")
            println("Commands: 's' -- self shot, 'o' -- shot enemy, 'i' -- use item, 'q' -- quit")

            val input = scanner.next()

            when (input) {
                "s" -> session.shot(currentPlayer)
                "o" -> {
                    println("Choose a target (1-${players.size}):")
                    if (scanner.hasNextInt()) {
                        val targetIdx = scanner.nextInt() - 1
                        if (targetIdx in players.indices) {
                            session.shot(players[targetIdx])
                        } else {
                            println("Wrong number!!!")
                        }
                    }
                }
                "i" -> {
                    if (currentPlayer.inventory.isEmpty()) {
                        println("NO ITEMS? :(")
                    } else {
                        println("Peek item (0-${currentPlayer.inventory.size - 1}):")
                        if (scanner.hasNextInt()) {
                            val itemIdx = scanner.nextInt()
                            val item = currentPlayer.inventory.getOrNull(itemIdx)

                            // ИСПРАВЛЕНИЕ ОШИБОК НА СТРОКЕ 81: Разделяем логику активации по типам
                            if (item != null) {
                                when (item) {
                                    is TargetItem -> {
                                        // Ищем первую подходящую живую цель для наручников
                                        val target = players.firstOrNull { it != currentPlayer && it.health > 0 }
                                        if (target != null) {
                                            session.useItem(currentPlayer, item, target) // Вызов перегрузки TargetItem
                                        } else {
                                            println("No valid targets available!")
                                        }
                                    }
                                    is Item -> {
                                        session.useItem(currentPlayer, item) // Вызов перегрузки Item без цели
                                    }
                                    else -> println("Unknown item type!")
                                }
                            } else {
                                println("Invalid item index!")
                            }
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

            is GameEvent.RoundEnded -> {
                currentSession?.startRound()
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
