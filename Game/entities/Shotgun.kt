package game.entities

import game.models.AmmoType

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

    fun peek(): AmmoType?
    {
        if (barrel.isEmpty()) {
            return null
        }

        return barrel.first()
    }

    fun findFirstLive(): Int
    {
        if (barrel.isEmpty()) {
            return -1
        }
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