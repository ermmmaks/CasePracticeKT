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
        val live = (1..4).random()
        val blank = (1..4).random()
        shotgun.load(live, blank)

        val allPossibleItems = listOf(
            GameItems.Handsaw, GameItems.Magnifier, GameItems.Beer,
            GameItems.Phone, GameItems.Inverter, GameItems.Handcuffs, 
            GameItems.Cigarette, GameItems.Handsaw, 
        )

        for (player in players) {
            player.isCuffed = false
            player.isBuffed = false

            repeat(8) {
                val randomItem = allPossibleItems.random()
                player.addItem(randomItem)
            }
        }

        status = SessionStatus.PLAYER_TURN

        val firstPlayer = players[currentPlayerIdx]
        onEvent?.invoke(GameEvent.ActionLog("${live} LIVE, ${blank} BLANK. SOMEONE WILL BE HURT"))
        onEvent?.invoke(GameEvent.TurnChanged(firstPlayer.name))
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

        checkGameCondition()

        onEvent?.invoke(
            GameEvent.ShotFired(
                type = ammo,
                targetName = target.name,
                damage = damageResult
            )
        )

        damageMultiplier = 1

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
        val activePlayers = players.filter { it.health > 0 }

        if (activePlayers.size <= 1) {
            status = SessionStatus.GAME_OVER
            onEvent?.invoke(GameEvent.GameOver)
            return
        }

        if (shotgun.isEmpty()) {
            onEvent?.invoke(GameEvent.ActionLog("Get ready for another round >:)"))
            startRound(live = (1..4).random(), blank = (1..4).random())
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
        if (player != players[currentPlayerIdx]) {
            sendInfo("Cheater?! FU")
        }

        if (player.inventory.remove(item)) {
            item.applyEffect(this, player, target)
            onEvent?.invoke(GameEvent.ItemUsed(player.name, item.name))
        }
    }
}
