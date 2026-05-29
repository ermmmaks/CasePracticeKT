package game.engine

import game.models.*
import game.entities.*

class GameSession(
    val players: List<Player>,
) : GameContext {
    override val shotgun = Shotgun()
    override var damageMultiplier: Int = 1
    override var onEvent: ((GameEvent) -> Unit)? = null

    private var currentPlayerIdx: Int = 0

    var status: SessionStatus = SessionStatus.LOADING
        private set

    fun startRound(live: Int? = null, blank: Int? = null) {
        status = SessionStatus.DISTRIBUTION

        val finalLive = live ?: (MIN_BULLETS_PER_ROUND..MAX_BULLETS_PER_ROUND).random()
        val finalBlank = blank ?: (MIN_BULLETS_PER_ROUND..MAX_BULLETS_PER_ROUND).random()

        shotgun.load(finalLive, finalBlank)

        val allPossibleItems: List<Any> = listOf(
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

        damageMultiplier = 1

        if (checkGameCondition()) return

        if (shotgun.isEmpty()) {
            onEvent?.invoke(GameEvent.ActionLog("Get ready for another round >:)"))
            onEvent?.invoke(GameEvent.RoundEnded)
            startRound()
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

    fun useItem(player: Player, item: Item) {
        if (!validateUser(player)) return

        if (player.removeItem(item)) {
            item.applyEffect(this, player)
            onEvent?.invoke(GameEvent.ItemUsed(player.name, item.name))
            checkPostItemCondition()
        }
    }

    fun useItem(player: Player, item: TargetItem, target: Player) {
        if (!validateUser(player)) return

        if (player.removeItem(item)) {
            item.applyEffect(this, player, target)
            onEvent?.invoke(GameEvent.ItemUsed(player.name, item.name))
            checkPostItemCondition()
        }
    }

    private fun validateUser(player: Player): Boolean {
        if (player != players[currentPlayerIdx]) return false
        if (player.health <= 0) {
            onEvent?.invoke(GameEvent.InfoMessage("Dead men tell no tales... and use no items"))
            return false
        }
        return true
    }

    private fun checkPostItemCondition() {
        if (checkGameCondition()) return
        if (shotgun.isEmpty()) {
            onEvent?.invoke(GameEvent.ActionLog("Get ready for another round >:)"))
            onEvent?.invoke(GameEvent.RoundEnded)
            startRound()
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
}
