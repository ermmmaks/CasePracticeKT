class AppLauncher
{
    private var currentSession: GameSession? = null

    companion object {
        @JvmStatic
        fun main(args: Array<String>)
        {
            println("Buckshot roulette Admin Tool Started")

            val launcher = AppLauncher()
            launcher.startNewGame(playerCount = 4)
        }
    }

    fun startNewGame(playerCount: Int)
    {
        if (playerCount < 2 || playerCount > 4) {
            println("Maximum is 4 players. Kill unnecessary bug of bones")
            return
        }

        val players = List(playerCount) { i ->
            Player(name = "Player ${i + 1}", initialHealth = 4)
        }

        val session = GameSession(players)
        currentSession = session

        session.onEvent = { event ->
            handleGameEvent(event)
        }

        session.startRound(live = 4, blank = 2)
    }

    private fun handleGameEvent(event: GameEvent) 
    {
        when (event) {
            is GameEvent.ShotFired -> {
                println("Shot animation ${event.type}")
            }

            is GameEvent.TurnChanged -> {
                println("Now is ${event.newActivePlayerName}'s turn")
            }

            is GameEvent.ItemUsed -> {
                println("${event.playerName} used ${event.itemName}")
            }

            is GameEvent.InfoMessage -> {
                println("Yo! ${event.text}")
            }

            is GameEvent.ActionLog -> {
                println("Log: ${event.text}")
            }

            is GameEvent.GameOver -> {
                println("Show statistic")
            }
        }
    }
}
