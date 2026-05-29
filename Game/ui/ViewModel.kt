package game.ui

import androidx.compose.runtime.*
import game.models.GameEvent
import game.engine.GameSession
import game.entities.Item
import game.entities.TargetItem
import game.entities.Player

data class PlayerUiState(
    val name: String,
    val health: Int,
    val isCuffed: Boolean,
    val isBuffed: Boolean,
    val inventory: List<Any>, // Тип изменен на Any, так как внутри лежат разные интерфейсы предметов
    val isAlive: Boolean
)

data class UiState(
    val players: List<PlayerUiState> = emptyList(),
    val activePlayerIdx: Int = 0,
    val targetPlayerIdx: Int? = null,
    val logs: List<String> = emptyList(),
    val isShotgunSawedOff: Boolean = false,
    val infoMessage: String = "",
    val pendingItemUser: PlayerUiState? = null,
    val pendingItem: TargetItem? = null // Строго храним предмет, ожидающий цель
)

class ViewModel(
    private val session: GameSession
) {
    var uiState by mutableStateOf(UiState(
        players = session.players.map { it.toUiState() }
    ))
        private set

    init {
        session.onEvent = { event -> handleEvent(event) }
    }

    fun handleEvent(event: GameEvent) {
        when (event) {
            is GameEvent.TurnChanged -> {
                val newIdx = uiState.players.indexOfFirst { it.name == event.newActivePlayerName }
                uiState = uiState.copy(activePlayerIdx = newIdx, isShotgunSawedOff = false, infoMessage = "")
                refreshPlayers()
            }
            is GameEvent.ShotFired -> {
                refreshPlayers()
            }
            is GameEvent.ItemUsed -> {
                if (event.itemName == "Handsaw") {
                    uiState = uiState.copy(isShotgunSawedOff = true)
                }
                refreshPlayers()
            }
            is GameEvent.ActionLog -> {
                uiState = uiState.copy(logs = uiState.logs.takeLast(5) + event.text)
            }
            is GameEvent.InfoMessage -> {
                uiState = uiState.copy(infoMessage = event.text)
            }
            is GameEvent.RoundEnded -> {
                uiState = uiState.copy(isShotgunSawedOff = false)
                refreshPlayers()
            }
            is GameEvent.GameOver -> {
                refreshPlayers()
            }
        }
    }

    private fun refreshPlayers() {
        uiState = uiState.copy(
            players = session.players.map { it.toUiState() }
        )
    }

    private fun Player.toUiState(): PlayerUiState = PlayerUiState(
        name = this.name,
        health = this.health,
        isCuffed = this.isCuffed,
        isBuffed = this.isBuffed,
        inventory = this.inventory.toList(),
        isAlive = this.health > 0
    )

    private fun canUseItem(playerUi: PlayerUiState): Boolean {
        val clickedPlayerIdx = uiState.players.indexOfFirst { it.name == playerUi.name }
        if (clickedPlayerIdx != uiState.activePlayerIdx || !playerUi.isAlive) {
            uiState = uiState.copy(infoMessage = "It's not yours, hands off!")
            return false
        }
        return true
    }

    // ПЕРЕГРУЗКА 1: Для обычных предметов без цели
    fun useItem(playerUi: PlayerUiState, item: Item) {
        if (!canUseItem(playerUi)) return

        val realPlayer = session.players.find { it.name == playerUi.name }
        if (realPlayer != null) {
            session.useItem(realPlayer, item)
            refreshPlayers()
        }
    }

    // ПЕРЕГРУЗКА 2: Для предметов, требующих выбора цели (например, Наручники)
    fun useItem(playerUi: PlayerUiState, item: TargetItem) {
        if (!canUseItem(playerUi)) return

        uiState = uiState.copy(
            pendingItemUser = playerUi,
            pendingItem = item,
            infoMessage = "SELECT TARGET FOR ${item.name.uppercase()}"
        )
    }

    private fun fireShot(targetUi: PlayerUiState) {
        val realTarget = session.players.find { it.name == targetUi.name }
        if (realTarget != null) {
            session.shot(realTarget)
        }
    }

    fun handlePlayerClick(clickedPlayerUi: PlayerUiState) {
        val clickedIdx = uiState.players.indexOfFirst { it.name == clickedPlayerUi.name }

        // Если в режиме ожидания находится таргетированный предмет
        val pendingItem = uiState.pendingItem
        if (pendingItem != null && uiState.pendingItemUser != null) {
            val realUser = session.players.find { it.name == uiState.pendingItemUser!!.name }
            val realTarget = session.players.find { it.name == clickedPlayerUi.name }

            if (realUser != null && realTarget != null) {
                if (realUser == realTarget) {
                    uiState = uiState.copy(infoMessage = "You cannot target yourself!")
                    return
                }
                session.useItem(realUser, pendingItem, realTarget)
                uiState = uiState.copy(
                    pendingItem = null,
                    pendingItemUser = null,
                    infoMessage = ""
                )
                refreshPlayers()
            }
            return
        }

        if (uiState.targetPlayerIdx == clickedIdx) {
            fireShot(clickedPlayerUi)
            uiState = uiState.copy(targetPlayerIdx = null)
        } else {
            uiState = uiState.copy(targetPlayerIdx = clickedIdx)
        }
    }
}
