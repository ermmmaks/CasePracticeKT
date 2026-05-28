package game.engine

import game.entities.Player
import game.models.AmmoType

interface GameContext
{
    fun peekNextAmmo(): AmmoType?
    fun ejectAmmo(): AmmoType
    fun healActivePlayer()
    fun upNextDamage()
    fun skipOpponent(target: Player)
    fun getPhoneCall()
    fun invertCurrentAmmo()
    fun sendInfo(message: String)
}