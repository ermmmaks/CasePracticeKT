package game

import game.engine.GameSession
import game.engine.ShotResult
import game.engine.UseItemResult
import game.engine.RoundStartResult
import game.entities.Player
import game.entities.Item
import game.entities.TargetItem
import game.entities.ItemEffectResult
import game.models.MAX_PLAYERS_COUNT
import game.models.MIN_PLAYERS_COUNT
import game.models.PLAYER_HEALTH
import game.models.SessionStatus
import java.util.Scanner

class AppLauncher
{
    private var currentSession: GameSession? = null
    private var players: List<Player> = emptyList()
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

        println("=== GAME STARTED ===")

        // Запускаем первый раунд и обрабатываем результат
        val roundStartResult = session.startRound(live = null, blank = null)
        handleRoundStart(roundStartResult)

        while (session.status != SessionStatus.GAME_OVER) {
            val currentPlayer = session.getCurrentPlayer() ?: break

            println("\n Your turn, ${currentPlayer.name}")

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
                "s" -> {
                    val result = session.shot(currentPlayer)
                    handleShotResult(result)
                }
                "o" -> {
                    println("Choose a target (1-${players.size}):")
                    if (scanner.hasNextInt()) {
                        val targetIdx = scanner.nextInt() - 1
                        if (targetIdx in players.indices) {
                            val result = session.shot(players[targetIdx])
                            handleShotResult(result)
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

                            if (item != null) {
                                when (item) {
                                    is TargetItem -> {
                                        val target = players.firstOrNull { it != currentPlayer && it.health > 0 }
                                        if (target != null) {
                                            val result = session.useItem(currentPlayer, item, target)
                                            handleItemResult(result)
                                        } else {
                                            println("No valid targets available!")
                                        }
                                    }
                                    is Item -> {
                                        val result = session.useItem(currentPlayer, item)
                                        handleItemResult(result)
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
                else -> println("UNDEFINED BEHAVIOR STACK OVERFLOW UNRESOLVED REFERENCE")
            }
        }

        // Игра закончена, показываем победителя
        val winner = currentSession?.getWinner()
        println("\n=== GAME OVER ===")
        println("WINNER: ${winner?.name?.uppercase() ?: "UNKNOWN"}")
    }

    private fun handleRoundStart(result: RoundStartResult) {
        println("Round started: ${result.liveCount} LIVE, ${result.blankCount} BLANK bullets")
        println("First turn: ${result.firstPlayerName}")
    }

    private fun handleShotResult(result: ShotResult) {
        when (result) {
            is ShotResult.TurnEnded -> {
                println("Shot at ${result.targetName} with ${result.ammo} for ${result.damage} damage")
                println("Next turn: ${result.nextPlayer}")
            }
            is ShotResult.SelfBlank -> {
                println("${result.targetName} shot themselves with BLANK - lucky bastard!")
                println("${result.targetName} keeps the turn")
            }
            is ShotResult.RoundEnded -> {
                println("${result.targetName} took ${result.damage} damage from ${result.ammo}")
                println("Round ended! Starting new round...")
                val newRoundResult = currentSession?.startRound()
                if (newRoundResult != null) {
                    handleRoundStart(newRoundResult)
                }
            }
            is ShotResult.GameOver -> {
                println("${result.targetName} took ${result.damage} damage from ${result.ammo}")
                println("GAME OVER!")
            }
            is ShotResult.Empty -> {
                println("Cannot shoot - shotgun is empty or not your turn!")
            }
        }
    }

    private fun handleItemResult(result: UseItemResult) {
        when (result) {
            is UseItemResult.Success -> {
                println("Used ${result.itemName}")
                handleItemEffect(result.effectResult)
            }
            is UseItemResult.RoundEnded -> {
                println("Used ${result.itemName} - round ended!")
                handleItemEffect(result.effectResult)
                val newRoundResult = currentSession?.startRound()
                if (newRoundResult != null) {
                    handleRoundStart(newRoundResult)
                }
            }
            is UseItemResult.GameOver -> {
                println("Used ${result.itemName} - GAME OVER!")
                handleItemEffect(result.effectResult)
            }
            is UseItemResult.InvalidUser -> {
                println("Invalid user or not your turn!")
            }
            is UseItemResult.ItemNotFound -> {
                println("Item not found in inventory!")
            }
        }
    }

    private fun handleItemEffect(effect: ItemEffectResult) {
        when (effect) {
            is ItemEffectResult.ActionLog -> println("Log: ${effect.message}")
            is ItemEffectResult.InfoMessage -> println("Info: ${effect.message}")
            is ItemEffectResult.Success -> println("Success: ${effect.message}")
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