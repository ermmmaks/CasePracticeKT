package game.entities

import game.engine.GameContext
import game.models.GameEvent

object GameItems {
    val Handsaw = object : Item {
        override val name = "Handsaw"
        override fun applyEffect(context: GameContext, user: Player) {
            context.damageMultiplier = 2
        }
    }

    val Magnifier = object : Item {
        override val name = "Magnifier"
        override fun applyEffect(context: GameContext, user: Player) {
            val ammo = context.shotgun.peek()
            context.onEvent?.invoke(GameEvent.InfoMessage("Current ammo is: $ammo"))
        }
    }

    val Beer = object : Item {
        override val name = "Beer"
        override fun applyEffect(context: GameContext, user: Player) {
            if (!context.shotgun.isEmpty()) {
                val ejected = context.shotgun.fire()
                context.onEvent?.invoke(GameEvent.ActionLog("$ejected ammo was ejected"))
            }
        }
    }

    val Cigarette = object : Item {
        override val name = "Cigarette"
        override fun applyEffect(context: GameContext, user: Player) {
            user.heal()
        }
    }

    val Phone = object : Item {
        override val name = "Phone"
        override fun applyEffect(context: GameContext, user: Player) {
            val idx = context.shotgun.findFirstLive()
            val message = if (idx == -1) "No life left" else "${idx + 1} is live"
            context.onEvent?.invoke(GameEvent.InfoMessage(message))
        }
    }

    val Inverter = object : Item {
        override val name = "Inverter"

        override fun applyEffect(context: GameContext, user: Player) {
            context.shotgun.invertCurrentAmmo()
            context.onEvent?.invoke(GameEvent.ActionLog("Reverse!"))
        }
    }

    val Handcuffs = object : TargetItem {
        override val name = "Handcuffs"
        override fun applyEffect(context: GameContext, user: Player, target: Player) {
            target.isCuffed = true
            context.onEvent?.invoke(GameEvent.ActionLog("${target.name} was cuffed lol"))
        }
    }

}