enum class AmmoType
{
    LIVE,
    BLANK
}

enum class SessionStatus
{
    LOADING,
    DISTRIBUTION,
    PLAYER_TURN,
    GAME_OVER
}

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

    object GameOver : GameEvent()
}

class Statistics 
(   
    var wins: Int = 0,
    var totalGames: Int = 0
) {
    fun calculateWinRate(): Double
    {
        if (totalGames == 0) {
            return 0.0
        }

        val rate = wins.toDouble() / totalGames
        return rate
    }
    val formattedWinRate: String
        get() = "${(calculateWinRate() * 100).toInt()}%"
}
