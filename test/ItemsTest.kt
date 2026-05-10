import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class GameItemsTest
{
    private lateinit var players: List<Player>
    private lateinit var session: GameSession

    @BeforeEach
    fun setup()
    {
        players = listOf(Player(name = "P1", initialHealth = 4), Player(name = "P2", initialHealth = 4))
        session = GameSession(players)
    }

    @Test
    fun `test magnifier effect`()
    {
        session.startRound(1, 0)
        // Предмет не должен падать, даже если мы просто проверяем логи
        GameItems.Magnifier.applyEffect(session, players[0])
    }

    @Test
    fun `test cigarette heal`()
    {
        players[0].takeDamage(2)
        GameItems.Cigarette.applyEffect(session, players[0])
        assertEquals(3, players[0].health)
    }

    @Test
    fun `test beer ejects ammo`()
    {
        session.startRound(1, 0)
        players[0].inventory.clear()
        GameItems.Beer.applyEffect(session, players[0], null)
        assertEquals(8, players[0].inventory.size)
        assertNotNull(session.peekNextAmmo())
    }

    @Test
    fun `test handcuffs apply status`()
    {
        GameItems.Handcuffs.applyEffect(session, players[0], players[1])
        assertTrue(players[1].isCuffed)
    }

    @Test
    fun `test handsaw effect`()
    {
        session.startRound(1, 0)
        GameItems.Handsaw.applyEffect(session, players[0])
        session.shot(players[1])
        assertEquals(2, players[1].health) // 4 - (1 * 2) = 2
    }

    @Test
    fun `test inverter effect`()
    {
        session.startRound(1, 0) // LIVE
        GameItems.Inverter.applyEffect(session, players[0])
        assertEquals(AmmoType.BLANK, session.peekNextAmmo())
    }
}
