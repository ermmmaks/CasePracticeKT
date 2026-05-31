package game.entities

import game.engine.GameContext

interface Item {
    val name: String
    fun applyEffect(context: GameContext, user: Player): ItemEffectResult
}

interface TargetItem {
    val name: String
    fun applyEffect(context: GameContext, user: Player, target: Player): ItemEffectResult
}

sealed class ItemEffectResult {
    data class Success(val message: String) : ItemEffectResult()
    data class ActionLog(val message: String) : ItemEffectResult()
    data class InfoMessage(val message: String) : ItemEffectResult()
}