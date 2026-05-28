package game.entities

import game.engine.GameContext

object GameItems {
    val Handsaw = object : Item {
        override val name = "Handsaw"
        override fun applyEffect(context: GameContext, user: Player, target: Player?) {
            context.upNextDamage()
        }
    }

    val Magnifier = object : Item {
        override val name = "Magnifier"
        override fun applyEffect(context: GameContext, user: Player, target: Player?) {
            val ammo = context.peekNextAmmo()
            context.sendInfo("Current ammo is: $ammo")
        }
    }

    val Beer = object : Item {
        override val name = "Beer"
        override fun applyEffect(context: GameContext, user: Player, target: Player?) {
            val ejected = context.ejectAmmo()
            context.sendInfo("Ejected ammo is: $ejected")
        }
    }

    val Cigarette = object : Item {
        override val name = "Cigarette"
        override fun applyEffect(context: GameContext, user: Player, target: Player?) {
            user.heal()
        }
    }

    val Handcuffs = object : Item {
        override val name = "Handcuffs"
        override fun applyEffect(context: GameContext, user: Player, target: Player?) {
            if (target != null) {
                context.skipOpponent(target)
            }
        }
    }

    val Phone = object : Item {
        override val name = "Phone"
        override fun applyEffect(context: GameContext, user: Player, target: Player?) {
            context.getPhoneCall()
        }
    }

    val Inverter = object : Item {
        override val name = "Inverter"

        override fun applyEffect(context: GameContext, user: Player, target: Player?)
        {
            context.invertCurrentAmmo()
        }
    }
}