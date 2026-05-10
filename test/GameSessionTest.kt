import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class GameSessionTest
{
    @Test
    fun `test item distribution on round start`()
    {
        val players = listOf(Player(name = "P1", initialHealth = 4), Player(name = "P2", initialHealth = 4))
        val session = GameSession(players)
        session.startRound(2, 2)

        // Each living player receives 8 items
        assertEquals(8, players[0].inventory.size)
        assertEquals(8, players[1].inventory.size)
    }

    @Test
    fun `test turn change and skipping cuffed player`()
    {
        val players = (1..3).map { Player(name = "P$it", initialHealth = 4) }
        val session = GameSession(players)
        session.startRound(2, 0)

        players[1].isCuffed = true

        // P1 shot P3
        session.shot(players[2])

        // Turn must be skip P2 and go to P3 (or return to P1 if is only 2 player)
    }

    @Test
    fun `test player death and game over`()
    {
        val players = listOf(Player(name = "P1", initialHealth = 1), Player(name = "P2", initialHealth = 1))
        val session = GameSession(players)
        session.startRound(1, 0) // One LIVE

        session.shot(players[1])

        assertEquals(0, players[1].health)
        assertEquals(SessionStatus.GAME_OVER, session.status)
    }

    @Test
    fun `test automatic reload when empty`()
    {
        val players = listOf(Player(name = "P1", initialHealth = 4), Player(name = "P2", initialHealth = 4))
        val session = GameSession(players)
        session.startRound(1, 0) // Only 1 ammo

        session.shot(players[1])

        // checkGameCondition and start new round
        assertNotNull(session.peekNextAmmo())
        assertEquals(SessionStatus.PLAYER_TURN, session.status)
    }

    @Test
    fun `E2E - Full duel scenario`()
    {
        val players = listOf(Player(name = "SuperHero", initialHealth = 2), Player(name = "Victim", initialHealth = 1))
        val session = GameSession(players)

        // Round 1
        session.startRound(1, 0)
        session.shot(players[1]) // Victim takes 2 damage and dies

        assertEquals(0, players[1].health)
        assertEquals(SessionStatus.GAME_OVER, session.status)
    }
}
