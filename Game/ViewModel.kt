import androidx.compose.runtime.*
import kotlinx.coroutines.flow.*

data class PlayerUiState
(
    val id: UUID,
    val name: String,
    val health: Int,
    val isCuffed: Boolean,
    val inventory: List<Item>,
    val isAlive: Boolean
)

data class UiState
(
    val players: List<PlayerUiState> = emptyList(),
    val activePlayerIdx: Int = 0,
    val logs: List<String> = emptyList(),
    val isShotgunSawedOff: Boolean = false,
    val infoMessage: String = ""
)

class ViewModel(private val session: GameSession, private val playersFromSession: List<Player>)
{
    var uiState by mutableStateOf(UiState(
        players = playersFromSession.map { it.toUiState() }
    ))
        private set
    
    init {
        session.onEvent = { event -> handleEvent(event) }
    }

    private fun handleEvent(event: GameEvent)
    {
        when (event) {
            is GameEvent.TurnChanged -> {
                val newIdx = uiState.players.indexOfFirst { it.name == event.newActivePlayeerName }
                uiState = uiState.copy(activePlayerIdx = newIdx, isShotgunSawedOff = false)
                refreshPlayers()
            }
            is GameEvent.ShotFired, is GameEvent.ItemUsed -> {
                if (event is GameEvent.ItemUsed && event.itemName == "Handsaw") {
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

            is GameEvent.GameOver -> {
                uiState = uiState.copy(infoMessage = "GAME OVER")
            }
        }
    }

    private fun refreshPlayers()
    {
        uiState = uiState.copy (
            players = playersFromSession.map { it.toUiState() }
        )
    }

    private fun Player.toUiState() = PlayerUiState
    (
        id = this.id,
        name = this.name,
        health = this.health,
        isCuffed = this.isCuffed,
        isBuffed = this.isBuffed,
        inventory = this.inventory.toList()
        isAlive = this.health > 0
    )
}
