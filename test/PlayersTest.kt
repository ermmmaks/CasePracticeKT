import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class PlayerTest
{
    @Test
    fun `test health boundaries`()
    {
        val player = Player(name = "Test", initialHealth = 3)
        player.heal() // 4
        player.heal() // 4 is max
        assertEquals(4, player.health)

        player.takeDamage(10)
        assertEquals(0, player.health)
    }

    @Test
    fun `test inventory limit`()
    {
        val player = Player(name = "Test", initialHealth = 4)
        repeat(8) {
            assertTrue(player.addItem(GameItems.Beer))
        }
        // 8 is max
        assertFalse(player.addItem(GameItems.Cigarette))
        assertEquals(8, player.inventory.size)
    }
}
