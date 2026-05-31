package game.engine

import game.models.AmmoType
import game.entities.ItemEffectResult

sealed class ShotResult {
    data class TurnEnded(val ammo: AmmoType, val targetName: String, val damage: Int, val nextPlayer: String) : ShotResult()
    data class SelfBlank(val ammo: AmmoType, val targetName: String, val damage: Int) : ShotResult()
    data class RoundEnded(val ammo: AmmoType, val targetName: String, val damage: Int) : ShotResult()
    data class GameOver(val ammo: AmmoType, val targetName: String, val damage: Int) : ShotResult()
    data object Empty : ShotResult()
}

data class RoundStartResult(
    val liveCount: Int,
    val blankCount: Int,
    val firstPlayerName: String
)

sealed class UseItemResult {
    data class Success(
        val itemName: String,
        val effectResult: ItemEffectResult
    ) : UseItemResult()

    data class RoundEnded(
        val itemName: String,
        val effectResult: ItemEffectResult
    ) : UseItemResult()

    data class GameOver(
        val itemName: String,
        val effectResult: ItemEffectResult
    ) : UseItemResult()

    data object InvalidUser : UseItemResult()
    data object ItemNotFound : UseItemResult()
}