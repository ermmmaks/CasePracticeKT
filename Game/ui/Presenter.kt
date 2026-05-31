package game.ui

import androidx.compose.runtime.*
import game.engine.*
import game.entities.*

data class PlayerUiState(
    val name: String,
    val health: Int,
    val isCuffed: Boolean,
    val isBuffed: Boolean,
    val inventory: List<Any>,
    val isAlive: Boolean
)

data class UiState(
    val players: List<PlayerUiState> = emptyList(),
    val activePlayerName: String = "",
    val targetPlayerName: String? = null,
    val logs: MutableList<String> = mutableListOf(),
    val isShotgunSawedOff: Boolean = false,
    val infoMessage: String = "",
    val pendingItemUser: String? = null,
    val pendingItem: TargetItem? = null,
    val isGameOver: Boolean = false
)

class ViewModel(
    session: GameSession
) {
    var uiState by mutableStateOf(UiState(
        players = session.getPlayersCopy().map { it.toUiState() },
        activePlayerName = session.getCurrentPlayer()?.name ?: ""
    ))
        private set

    private var currentSession = session

    fun startGame() {
        val result = currentSession.startRound()
        addLog("${result.liveCount} LIVE, ${result.blankCount} BLANK. SOMEONE WILL BE HURT")
        updateStateFromSession()
        uiState = uiState.copy(
            activePlayerName = result.firstPlayerName,
            infoMessage = ""
        )
    }

    fun shoot(targetPlayerUi: PlayerUiState) {
        if (uiState.isGameOver) return

        val realTarget = currentSession.players.find { it.name == targetPlayerUi.name }
        if (realTarget == null || currentSession.getCurrentPlayer()?.name != uiState.activePlayerName) {
            uiState = uiState.copy(infoMessage = "Not your turn!")
            return
        }

        when (val result = currentSession.shot(realTarget)) {
            is ShotResult.TurnEnded -> {
                addLog("Shot at ${result.targetName} with ${result.ammo} for ${result.damage} damage")
                updateStateFromSession()
                uiState = uiState.copy(
                    activePlayerName = result.nextPlayer,
                    targetPlayerName = null,
                    isShotgunSawedOff = false
                )
            }
            is ShotResult.SelfBlank -> {
                addLog("${result.targetName} shot themselves with BLANK - lucky bastard!")
                updateStateFromSession()
                uiState = uiState.copy(
                    activePlayerName = uiState.activePlayerName,
                    targetPlayerName = null
                )
            }
            is ShotResult.RoundEnded -> {
                addLog("${result.targetName} took ${result.damage} damage from ${result.ammo}")
                addLog("Get ready for another round >:)")
                updateStateFromSession()
                val newRoundResult = currentSession.startRound()
                addLog("${newRoundResult.liveCount} LIVE, ${newRoundResult.blankCount} BLANK. NEXT ROUND")
                updateStateFromSession()
                uiState = uiState.copy(
                    activePlayerName = newRoundResult.firstPlayerName,
                    targetPlayerName = null,
                    isShotgunSawedOff = false
                )
            }
            is ShotResult.GameOver -> {
                addLog("${result.targetName} took ${result.damage} damage from ${result.ammo}")
                addLog("GAME OVER! ${currentSession.getWinner()?.name} WINS!")
                updateStateFromSession()
                uiState = uiState.copy(
                    isGameOver = true,
                    targetPlayerName = null
                )
            }
            is ShotResult.Empty -> {
            }
        }
    }

    fun useItem(playerUi: PlayerUiState, item: Item) {
        if (uiState.isGameOver) return

        if (playerUi.name != uiState.activePlayerName || !playerUi.isAlive) {
            uiState = uiState.copy(infoMessage = "It's not yours, hands off!")
            return
        }

        val realPlayer = currentSession.players.find { it.name == playerUi.name }
        if (realPlayer != null) {
            val result = currentSession.useItem(realPlayer, item)
            handleItemResult(result, item.name)
        }
    }

    fun useItem(playerUi: PlayerUiState, item: TargetItem) {
        if (uiState.isGameOver) return

        if (playerUi.name != uiState.activePlayerName || !playerUi.isAlive) {
            uiState = uiState.copy(infoMessage = "It's not yours, hands off!")
            return
        }

        uiState = uiState.copy(
            pendingItemUser = playerUi.name,
            pendingItem = item,
            infoMessage = "SELECT TARGET FOR ${item.name.uppercase()}"
        )
    }

    fun applyTargetItem(targetPlayerUi: PlayerUiState) {
        val pendingItem = uiState.pendingItem
        val pendingUser = uiState.pendingItemUser

        if (pendingItem == null || pendingUser == null) return

        if (targetPlayerUi.name == pendingUser) {
            uiState = uiState.copy(infoMessage = "You cannot target yourself!", pendingItem = null, pendingItemUser = null)
            return
        }

        val realUser = currentSession.players.find { it.name == pendingUser }
        val realTarget = currentSession.players.find { it.name == targetPlayerUi.name }

        if (realUser != null && realTarget != null) {
            val result = currentSession.useItem(realUser, pendingItem, realTarget)
            handleItemResult(result, pendingItem.name)
        }

        uiState = uiState.copy(pendingItem = null, pendingItemUser = null)
    }

    fun selectTarget(targetPlayerUi: PlayerUiState) {
        if (uiState.isGameOver) return

        if (uiState.pendingItem != null) {
            applyTargetItem(targetPlayerUi)
        } else if (uiState.targetPlayerName == targetPlayerUi.name) {
            shoot(targetPlayerUi)
        } else {
            uiState = uiState.copy(targetPlayerName = targetPlayerUi.name)
        }
    }

    private fun handleItemResult(result: UseItemResult, itemName: String) {
        when (result) {
            is UseItemResult.Success -> {
                addLog("Used $itemName")
                if (itemName == "Handsaw") {
                    uiState = uiState.copy(isShotgunSawedOff = true)
                }
                updateStateFromSession()
            }
            is UseItemResult.RoundEnded -> {
                addLog("Used $itemName - round ended!")
                updateStateFromSession()
                val newRoundResult = currentSession.startRound()
                addLog("${newRoundResult.liveCount} LIVE, ${newRoundResult.blankCount} BLANK. NEXT ROUND")
                updateStateFromSession()
                uiState = uiState.copy(
                    activePlayerName = newRoundResult.firstPlayerName,
                    isShotgunSawedOff = false
                )
            }
            is UseItemResult.GameOver -> {
                addLog("Used $itemName - GAME OVER! ${currentSession.getWinner()?.name} WINS!")
                updateStateFromSession()
                uiState = uiState.copy(isGameOver = true)
            }
            is UseItemResult.InvalidUser -> {
                uiState = uiState.copy(infoMessage = "Invalid user or not your turn!")
            }
            is UseItemResult.ItemNotFound -> {
                uiState = uiState.copy(infoMessage = "Item not found in inventory!")
            }
        }
    }

    private fun updateStateFromSession() {
        uiState = uiState.copy(
            players = currentSession.getPlayersCopy().map { it.toUiState() }
        )
    }

    private fun addLog(message: String) {
        val newLogs = uiState.logs.toMutableList()
        newLogs.add(message)
        if (newLogs.size > 5) newLogs.removeAt(0)
        uiState = uiState.copy(logs = newLogs)
    }

    private fun Player.toUiState(): PlayerUiState = PlayerUiState(
        name = this.name,
        health = this.health,
        isCuffed = this.isCuffed,
        isBuffed = this.isBuffed,
        inventory = this.inventory.toList(),
        isAlive = this.health > 0
    )
}