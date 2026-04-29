import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class GameLogicTest
{
    private lateinit var players: List<Player>
    private lateinit var session: GameSession

    @BeforeEach
    fun setup()
    {
        players = listOf(
            Player(name = "Bastard", initialHealth = 4),
            Player(name = "Freak", initialHealth = 4),
            Player(name = "Clown", initialHealth = 4),
            Player(name = "God Maksim", initialHealth = 4),
        )
        session = GameSession(players)
    }

    @Test
    fun testShotgunLoading()
    {
        session.startRound(live = 1, blank = 0)

        assertEquals(AmmoType.LIVE, session.peekNextAmmo())
    }

    @Test
    fun testHealth()
    {
        val player = players[0]
        player.takeDamage(10)

        assertEquals(0, player.health)
    }

    @Test
    fun testHandcuff()
    {
        session.startRound(live = 2, blank = 2)

        session.skipOpponent(players[1])
        assertTrue(players[1].isCuffed)

        session.shot(players[2])
    }

    @Test
    fun testShotSelfWithBlank()
    {
        session.startRound(live = 0, blank = 2)

        val activeBefore = players[0]

        session.shot(activeBefore)

        assertNotEquals(SessionStatus.GAME_OVER, session.status)
    }

    @Test
    fun testGameOverCondition()
    {
        session.startRound(live = 1, blank = 0)

        players[1].takeDamage(4)

        session.shot(players[1])

        assertEquals(SessionStatus.GAME_OVER, session.status)
    }
}