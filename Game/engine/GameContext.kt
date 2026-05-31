package game.engine

import game.entities.Shotgun

interface GameContext {
    val shotgun: Shotgun
    var damageMultiplier: Int
}