package game.entities

import game.models.HEAL_VALUE
import game.models.MAX_INVENTORY_SIZE
import game.models.PLAYER_HEALTH

class Player
(
    val name: String,
    initialHealth: Int,
) {
    var health: Int = initialHealth
        private set

    var isCuffed: Boolean = false
    var isBuffed: Boolean = false

    val inventory = mutableListOf<Any>()

    fun takeDamage(dmg: Int) {
        health = (health - dmg).coerceAtLeast(0)
    }

    fun heal() {
        val nextHealth = health + HEAL_VALUE
        health = if (nextHealth > PLAYER_HEALTH) PLAYER_HEALTH else nextHealth
    }

    fun addItem(item: Any): Boolean {
        if (inventory.size >= MAX_INVENTORY_SIZE) {
            return false
        }

        inventory.add(item)
        return true
    }

    fun removeItem(item: Any): Boolean {
        return inventory.remove(item)
    }
}