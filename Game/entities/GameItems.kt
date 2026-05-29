package game.entities

import game.engine.GameContext

object GameItems {
    val Handsaw = object : Item {
        override val name = "Handsaw"
        override fun applyEffect(context: GameContext, user: Player): ItemEffectResult {
            context.damageMultiplier = 2
            return ItemEffectResult.Success("Handsaw applied - damage multiplier increased")
        }
    }

    val Magnifier = object : Item {
        override val name = "Magnifier"
        override fun applyEffect(context: GameContext, user: Player): ItemEffectResult {
            val ammo = context.shotgun.peek()
            return ItemEffectResult.InfoMessage("Current ammo is: $ammo")
        }
    }

    val Beer = object : Item {
        override val name = "Beer"
        override fun applyEffect(context: GameContext, user: Player): ItemEffectResult {
            if (!context.shotgun.isEmpty()) {
                val ejected = context.shotgun.fire()
                return ItemEffectResult.ActionLog("$ejected ammo was ejected")
            }
            return ItemEffectResult.Success("Beer did nothing - shotgun is empty")
        }
    }

    val Cigarette = object : Item {
        override val name = "Cigarette"
        override fun applyEffect(context: GameContext, user: Player): ItemEffectResult {
            user.heal()
            return ItemEffectResult.Success("${user.name} healed")
        }
    }

    val Phone = object : Item {
        override val name = "Phone"
        override fun applyEffect(context: GameContext, user: Player): ItemEffectResult {
            val idx = context.shotgun.findFirstLive()
            val message = if (idx == -1) "No life left" else "${idx + 1} is live"
            return ItemEffectResult.InfoMessage(message)
        }
    }

    val Inverter = object : Item {
        override val name = "Inverter"

        override fun applyEffect(context: GameContext, user: Player): ItemEffectResult {
            context.shotgun.invertCurrentAmmo()
            return ItemEffectResult.ActionLog("Reverse! Ammo chamber inverted")
        }
    }

    val Handcuffs = object : TargetItem {
        override val name = "Handcuffs"
        override fun applyEffect(context: GameContext, user: Player, target: Player): ItemEffectResult {
            target.isCuffed = true
            return ItemEffectResult.ActionLog("${target.name} was cuffed lol")
        }
    }
}