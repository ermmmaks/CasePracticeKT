interface GameContext
{
    fun peekNextAmmo(): AmmoType
    fun ejectAmmo(): AmmoType
    fun healActivePlayer()
    fun doubleNextDamage()
    fun skipOpponent(target: Player)
    fun getPhoneCall()
    fun invertCurrentAmmo()
    fun sendInfo(message: String)
}

class GameSession
(
    private val players: List<Player>,
) : GameContext {

    val id: java.util.UUID = java.util.UUID.randomUUID()
    
    private val shotgun = Shotgun()
    private var currentPlayerIdx: Int = 0
    private var damageMultiplier: Int = 1
    
    var onEvent: ((GameEvent) -> Unit)? = null

    var status: SessionStatus = SessionStatus.LOADING
        private set

    fun startRound(live: Int, blank: Int) {
        for (player in players) {
            player.isCuffed = false
            player.isBuffed = false
        }

        shotgun.load(live, blank)
        status = SessionStatus.PLAYER_TURN

        val firstPlayer = players[currentPlayerIdx]
        onEvent?.invoke(GameEvent.TurnChanged(firstPlayer.name))
        onEvent?.invoke(GameEvent.ActionLog("New round: ${live} live, ${blank} blank"))
    }

    // SSHHOOTT
    fun shot(target: Player)
    {
        if (status != SessionStatus.PLAYER_TURN) {
            return
        }

        val ammo = shotgun.fire()
        var damageResult = 0

        if (ammo == AmmoType.LIVE) {
            damageResult = 1 * damageMultiplier
        } else {
            damageResult = 0
        }

        target.takeDamage(damageResult)

        onEvent?.invoke(
            GameEvent.ShotFired(
                type = ammo,
                targetName = target.name,
                damage = damageResult
            )
        )

        damageMultiplier = 1

        checkGameCondition()

        if (status != SessionStatus.GAME_OVER) {
            val shotSelfWithBlank = (target == players[currentPlayerIdx])
            val isBlank = (ammo == AmmoType.BLANK)
            
            if (shotSelfWithBlank && isBlank) {
                onEvent?.invoke(GameEvent.ActionLog("${target.name} is lucky bastard"))
            } else {
                nextTurn()
            }
        }
    }

    // NNEEXXTT TTUURRNN
    private fun nextTurn()
    {
        var nextIdx = (currentPlayerIdx + 1) % players.size
        
        if (players[nextIdx].isCuffed) {
            val skippedPlayer = players[nextIdx]
            skippedPlayer.isCuffed = false

            onEvent?.invoke(GameEvent.ActionLog("${skippedPlayer.name} skipped turn"))

            nextIdx = (nextIdx + 1) % players.size
        }

        currentPlayerIdx = nextIdx
        val activePlayer = players[currentPlayerIdx]
        onEvent?.invoke(GameEvent.TurnChanged(activePlayer.name))
    }

    // CCOONNDDIITTIIOONN
    private fun checkGameCondition()
    {
        val losers = players.find { it.health <= 0 }

        if (losers != null) {
            status = SessionStatus.GAME_OVER
            onEvent?.invoke(GameEvent.GameOver)
        }
    }

    // EFFECTS
    override fun peekNextAmmo(): AmmoType
    {
        return shotgun.peek()
    }

    override fun ejectAmmo(): AmmoType
    {
        val ammo = shotgun.fire()
        onEvent?.invoke(GameEvent.ActionLog("${ammo} was ejected"))
        return ammo
    }

    override fun healActivePlayer()
    { 
        players[currentPlayerIdx].heal()
    }    

    override fun doubleNextDamage()
    {
        damageMultiplier = 2
    }

    override fun skipOpponent(target: Player)
    {
        val currentPlayer = players[currentPlayerIdx]

        if (target == currentPlayer) {
            sendInfo("Are u stpd?")
            return
        }

        target.isCuffed = true
        onEvent?.invoke(GameEvent.ActionLog("${target.name} was cuffed"))
    }

    override fun getPhoneCall()
    {
        val idx = shotgun.findFirstLive()

        if (idx == -1) {
            sendInfo("No live left")
        } else {
            val position = idx + 1
            sendInfo("${position} is live")
        }
    }

    override fun invertCurrentAmmo()
    {
        shotgun.invertCurrentAmmo()
        onEvent?.invoke(GameEvent.ActionLog("reverse!"))
    }

    override fun sendInfo(message: String)
    {
        onEvent?.invoke(GameEvent.InfoMessage(message))
    }

    fun useItem(player: Player, item: Item, target: Player? = null) {
        if (player.inventory.remove(item)) {
            item.applyEffect(this, player, target)
            onEvent?.invoke(GameEvent.ItemUsed(player.name, item.name))
        }
    }
}
