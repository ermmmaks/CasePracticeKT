package game

import game.engine.*
import game.entities.*
import game.models.*

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
        players = List(MIN_PLAYERS_COUNT) { i ->
            Player(name = "P${i + 1}", initialHealth = PLAYER_HEALTH)
        }
        session = GameSession(players)
    }

    @Test
    fun `test magnifier effect`()
    {
        session.startRound(1, 0)
        assertEquals(AmmoType.LIVE, session.shotgun.peek())
        GameItems.Magnifier.applyEffect(session, players[0])
    }

    @Test
    fun `test cigarette heal`()
    {
        players[0].takeDamage(BASIC_DAMAGE)
        val healthBeforeHeal = players[0].health

        GameItems.Cigarette.applyEffect(session, players[0])

        assertEquals(healthBeforeHeal + HEAL_VALUE, players[0].health)
    }

    @Test
    fun `test beer ejects ammo`()
    {
        session.startRound(1, 0)

        assertEquals(AmmoType.LIVE, session.shotgun.peek())

        GameItems.Beer.applyEffect(session, players[0])

        assertNull(session.shotgun.peek(), "Дробовик должен быть пуст!")
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

        val initialHealth = players[1].health
        GameItems.Handsaw.applyEffect(session, players[0])
        session.shot(players[1])

        val expectedDamage = BASIC_DAMAGE * DAMAGE_MULTIPLIER
        assertEquals(initialHealth - expectedDamage, players[1].health)
    }

    @Test
    fun `test inverter effect`()
    {
        session.startRound(1, 0)
        GameItems.Inverter.applyEffect(session, players[0])
        assertEquals(AmmoType.BLANK, session.shotgun.peek())
    }
}
