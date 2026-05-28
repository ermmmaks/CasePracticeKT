package game

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

class GameSession
    (
    private val players: List<Player>,
) : GameContext {

    private val shotgun = Shotgun()
    private var currentPlayerIdx: Int = 0
    private var damageMultiplier: Int = DAMAGE_MULTIPLIER

    var onEvent: ((GameEvent) -> Unit)? = null

    var status: SessionStatus = SessionStatus.LOADING
        private set

    fun startRound(live: Int? = null, blank: Int? = null) {
        status = SessionStatus.DISTRIBUTION

        val finalLive = live ?: (MIN_BULLETS_PER_ROUND..MAX_BULLETS_PER_ROUND).random()
        val finalBlank = blank ?: (MIN_BULLETS_PER_ROUND..MAX_BULLETS_PER_ROUND).random()

        shotgun.load(finalLive, finalBlank)

        val allPossibleItems = listOf(
            GameItems.Handsaw, GameItems.Magnifier, GameItems.Beer,
            GameItems.Phone, GameItems.Inverter, GameItems.Handcuffs,
            GameItems.Cigarette,
        )

        for (player in players) {
            if (player.health <= 0) continue

            player.isCuffed = false
            player.isBuffed = false

            val freeCell = MAX_INVENTORY_SIZE - player.inventory.size
            val itemsToAdd = listOf(ITEMS_DISTRIBUTION_PER_ROUND, freeCell).minOrNull() ?: 0

            if (itemsToAdd > 0) {
                repeat(itemsToAdd) {
                    val randomItem = allPossibleItems.random()
                    player.addItem(randomItem)
                }
            }
        }

        status = SessionStatus.PLAYER_TURN

        val firstPlayer = players[currentPlayerIdx]
        onEvent?.invoke(GameEvent.ActionLog("$finalLive LIVE, $finalBlank BLANK. SOMEONE WILL BE HURT"))
        onEvent?.invoke(GameEvent.TurnChanged(firstPlayer.name))
    }

    fun shot(target: Player) {
        if (status != SessionStatus.PLAYER_TURN || shotgun.isEmpty()) {
            checkGameCondition()
            return
        }

        val ammo = shotgun.fire()
        val damageResult = if (ammo == AmmoType.LIVE) {
            BASIC_DAMAGE * damageMultiplier
        } else {
            0
        }

        target.takeDamage(damageResult)

        onEvent?.invoke(
            GameEvent.ShotFired(
                type = ammo,
                targetName = target.name,
                damage = damageResult
            )
        )

        damageMultiplier = DAMAGE_MULTIPLIER

        if (checkGameCondition()) return

        // Если патроны кончились после выстрела
        if (shotgun.isEmpty()) {
            onEvent?.invoke(GameEvent.ActionLog("Get ready for another round >:)"))
            onEvent?.invoke(GameEvent.RoundEnded)
            return
        }

        val shotSelfWithBlank = (target == players[currentPlayerIdx])
        val isBlank = (ammo == AmmoType.BLANK)

        if (shotSelfWithBlank && isBlank) {
            onEvent?.invoke(GameEvent.ActionLog("${target.name} is lucky bastard"))
            onEvent?.invoke(GameEvent.TurnChanged(players[currentPlayerIdx].name))
        } else {
            nextTurn()
        }
    }

    private fun nextTurn() {
        var nextIdx = (currentPlayerIdx + 1) % players.size

        while (players[nextIdx].health <= 0) {
            nextIdx = (nextIdx + 1) % players.size
            if (nextIdx == currentPlayerIdx) break
        }

        if (players[nextIdx].isCuffed) {
            val skippedPlayer = players[nextIdx]
            skippedPlayer.isCuffed = false
            onEvent?.invoke(GameEvent.ActionLog("${skippedPlayer.name} skipped turn"))
            currentPlayerIdx = nextIdx
            nextTurn()
            return
        }

        currentPlayerIdx = nextIdx
        val activePlayer = players[currentPlayerIdx]
        onEvent?.invoke(GameEvent.TurnChanged(activePlayer.name))
    }

    private fun checkGameCondition(): Boolean {
        val activePlayers = players.filter { it.health > 0 }

        if (activePlayers.size <= 1) {
            status = SessionStatus.GAME_OVER
            onEvent?.invoke(GameEvent.GameOver)
            return true
        }
        return false
    }

    override fun peekNextAmmo(): AmmoType? {
        return shotgun.peek()
    }

    override fun ejectAmmo(): AmmoType {
        if (shotgun.isEmpty()) return AmmoType.BLANK

        val ammo = shotgun.fire()
        onEvent?.invoke(GameEvent.ActionLog("$ammo was ejected"))

        if (checkGameCondition()) return ammo

        if (shotgun.isEmpty()) {
            onEvent?.invoke(GameEvent.ActionLog("Get ready for another round >:)"))
            onEvent?.invoke(GameEvent.RoundEnded)
        }

        return ammo
    }

    override fun healActivePlayer() {
        players[currentPlayerIdx].heal()
    }

    override fun upNextDamage() {
        damageMultiplier = DAMAGE_MULTIPLIER * BASIC_DAMAGE
    }

    override fun skipOpponent(target: Player) {
        val currentPlayer = players[currentPlayerIdx]
        if (target == currentPlayer) {
            sendInfo("Are u stpd?")
            return
        }
        target.isCuffed = true
        onEvent?.invoke(GameEvent.ActionLog("${target.name} was cuffed"))
    }

    override fun getPhoneCall() {
        val idx = shotgun.findFirstLive()
        if (idx == -1) {
            sendInfo("No live left")
        } else {
            val position = idx + 1
            sendInfo("$position is live")
        }
    }

    override fun invertCurrentAmmo() {
        shotgun.invertCurrentAmmo()
        onEvent?.invoke(GameEvent.ActionLog("Reverse!"))
    }

    override fun sendInfo(message: String) {
        onEvent?.invoke(GameEvent.InfoMessage(message))
    }

    fun useItem(player: Player, item: Item, target: Player? = null) {
        if (player != players[currentPlayerIdx]) return
        if (player.health <= 0) {
            sendInfo("Dead men tell no tales... and use no items")
            return
        }

        if (player.inventory.remove(item)) {
            item.applyEffect(this, player, target)
            onEvent?.invoke(GameEvent.ItemUsed(player.name, item.name))
        }
    }
}
