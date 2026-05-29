package game.entities

import game.engine.GameContext

interface Item
{
    val name: String
    fun applyEffect(
        context: GameContext,
        user: Player
    )
}

interface TargetItem {
    val name:String

    fun applyEffect(
        context: GameContext,
        user: Player,
        target: Player
    )
}