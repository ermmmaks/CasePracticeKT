package game.models

sealed class GameEvent
{
    data class ShotFired
    (
        val type: AmmoType,
        val targetName: String,
        val damage: Int,
    ) : GameEvent()

    data class InfoMessage
    (
        val text: String
    ) : GameEvent()

    data class ActionLog
    (
        val text: String
    ) : GameEvent()

    data class ItemUsed
    (
        val playerName: String,
        val itemName: String,
    ) : GameEvent()

    data class TurnChanged
    (
        val newActivePlayerName: String,
    ) : GameEvent()

    data object RoundEnded : GameEvent()

    data object GameOver : GameEvent()
}