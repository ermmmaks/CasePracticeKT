package game.engine

import game.entities.Shotgun
import game.models.GameEvent

interface GameContext {
    val shotgun: Shotgun
    var damageMultiplier: Int
    val onEvent: ((GameEvent) -> Unit)?
}