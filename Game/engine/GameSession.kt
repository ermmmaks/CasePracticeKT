package game.engine

import game.models.*
import game.entities.*

class GameSession(
    val players: List<Player>,
) : GameContext {
    override val shotgun = Shotgun()
    override var damageMultiplier: Int = 1

    private var currentPlayerIdx: Int = 0
    var status: SessionStatus = SessionStatus.LOADING
        private set

    fun startRound(live: Int? = null, blank: Int? = null): RoundStartResult {
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

        return RoundStartResult(
            liveCount = finalLive,
            blankCount = finalBlank,
            firstPlayerName = firstPlayer.name
        )
    }

    fun shot(target: Player): ShotResult {
        if (status != SessionStatus.PLAYER_TURN || shotgun.isEmpty()) {
            return ShotResult.Empty
        }

        val ammo = shotgun.fire()
        val damageResult = if (ammo == AmmoType.LIVE) {
            BASIC_DAMAGE * damageMultiplier
        } else {
            0
        }

        target.takeDamage(damageResult)

        damageMultiplier = 1

        val isGameOver = checkGameCondition()
        if (isGameOver) {
            status = SessionStatus.GAME_OVER
            return ShotResult.GameOver(ammo, target.name, damageResult)
        }

        if (shotgun.isEmpty()) {
            return ShotResult.RoundEnded(ammo, target.name, damageResult)
        }

        val shotSelfWithBlank = (target == players[currentPlayerIdx])
        val isBlank = (ammo == AmmoType.BLANK)

        if (shotSelfWithBlank && isBlank) {
            return ShotResult.SelfBlank(ammo, target.name, damageResult)
        } else {
            val nextPlayer = nextTurn()
            return ShotResult.TurnEnded(ammo, target.name, damageResult, nextPlayer)
        }
    }

    fun useItem(player: Player, item: Item): UseItemResult {
        if (!validateUser(player)) {
            return UseItemResult.InvalidUser
        }

        if (!player.removeItem(item)) {
            return UseItemResult.ItemNotFound
        }

        val effectResult = item.applyEffect(this, player)

        val isGameOver = checkGameCondition()
        if (isGameOver) {
            status = SessionStatus.GAME_OVER
            return UseItemResult.GameOver(item.name, effectResult)
        }

        if (shotgun.isEmpty()) {
            return UseItemResult.RoundEnded(item.name, effectResult)
        }

        return UseItemResult.Success(item.name, effectResult)
    }

    fun useItem(player: Player, item: TargetItem, target: Player): UseItemResult {
        if (!validateUser(player)) {
            return UseItemResult.InvalidUser
        }

        if (!player.removeItem(item)) {
            return UseItemResult.ItemNotFound
        }

        val effectResult = item.applyEffect(this, player, target)

        val isGameOver = checkGameCondition()
        if (isGameOver) {
            status = SessionStatus.GAME_OVER
            return UseItemResult.GameOver(item.name, effectResult)
        }

        if (shotgun.isEmpty()) {
            return UseItemResult.RoundEnded(item.name, effectResult)
        }

        return UseItemResult.Success(item.name, effectResult)
    }

    fun getCurrentPlayer(): Player? {
        return players.getOrNull(currentPlayerIdx)?.takeIf { it.health > 0 }
    }

    fun getWinner(): Player? {
        return players.find { it.health > 0 }
    }

    fun getPlayersCopy(): List<Player> {
        return players.map { player ->
            Player(player.name, player.health).apply {
                isCuffed = player.isCuffed
                isBuffed = player.isBuffed
                inventory.addAll(player.inventory)
            }
        }
    }

    private fun validateUser(player: Player): Boolean {
        return player == players[currentPlayerIdx] && player.health > 0
    }

    private fun nextTurn(): String {
        var nextIdx = (currentPlayerIdx + 1) % players.size

        while (players[nextIdx].health <= 0) {
            nextIdx = (nextIdx + 1) % players.size
            if (nextIdx == currentPlayerIdx) break
        }

        if (players[nextIdx].isCuffed) {
            players[nextIdx].isCuffed = false
            currentPlayerIdx = nextIdx
            return nextTurn()
        }

        currentPlayerIdx = nextIdx
        return players[currentPlayerIdx].name
    }

    private fun checkGameCondition(): Boolean {
        val activePlayers = players.filter { it.health > 0 }
        return activePlayers.size <= 1
    }
}
