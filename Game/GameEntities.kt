import java.util.UUID

interface Item
{
    val name: String
    fun applyEffect
    (
        context: GameContext,
        user: Player,
        target: Player? = null
    )
}

object GameItems {
    val Handsaw = object : Item {
        override val name = "Handsaw"
        override fun applyEffect(context: GameContext, user: Player, target: Player?) {
            context.doubleNextDamage()
        }
    }

    val Magnifier = object : Item {
        override val name = "Magnifer"
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

class Shotgun
{
    private val barrel = mutableListOf<AmmoType>()

    fun load(live: Int, blank: Int)
    {
        barrel.clear()
        repeat(live) {
            barrel.add(AmmoType.LIVE)
        }
        repeat(blank) {
            barrel.add(AmmoType.BLANK)
        }
        barrel.shuffle()
    }

    fun fire(): AmmoType
    {
        if (barrel.isEmpty())
        {    
            throw IllegalStateException("Barrel is empty!")
        }
        
        val ammo = barrel.removeAt(0)
        return ammo
    }

    fun peek(): AmmoType
    {
        if (barrel.isEmpty()) {
            throw IllegalStateException("Nothing to peek")
        }

        val ammo = barrel.first()
        return ammo
    }

    fun findFirstLive(): Int
    {
        return barrel.indexOf(AmmoType.LIVE)
    }

    fun invertCurrentAmmo()
    {
        if (barrel.isEmpty()) {
            return
        }
        
        val current = barrel[0]

        barrel[0] = if (current == AmmoType.LIVE) AmmoType.BLANK else AmmoType.LIVE
    }

    fun isEmpty(): Boolean
    {
        val emptyStatus = barrel.isEmpty()
        return emptyStatus
    }
}

class Player
(
    val id: UUID = UUID.randomUUID(),
    val name: String,
    initialHealth: Int,
) {
    var health: Int = initialHealth
        private set

    var isCuffed: Boolean = false
    var isBuffed: Boolean = false

    val inventory = mutableListOf<Item>()

    fun takeDamage(DMG: Int)
    {
        health = (health - DMG).coerceAtLeast(0)
    }

    fun heal()
    {
        val nextHealth = health + 1
        if (nextHealth > 4) {
        health = 4
        } else {
            health = nextHealth
        }
    }

    fun addItem(item: Item): Boolean
    {
        if (inventory.size >= 8) {
            return false
        }

        inventory.add(item)
        return true
    }
}
