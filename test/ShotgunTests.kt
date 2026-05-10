import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class ShotgunTest
{
    @Test
    fun `test loading and ammo count`()
    {
        val shotgun = Shotgun()
        shotgun.load(2, 3)
        assertFalse(shotgun.isEmpty())
        // 5 shot
        repeat(5) { shotgun.fire() }
        assertTrue(shotgun.isEmpty())
    }

    @Test
    fun `test peek returns null on empty`()
    {
        val shotgun = Shotgun()
        assertNull(shotgun.peek()) // check emptiness
    }

    @Test
    fun `test invert current ammo`()
    {
        val shotgun = Shotgun()
        shotgun.load(1, 0) // one LIVE
        shotgun.invertCurrentAmmo()
        assertEquals(AmmoType.BLANK, shotgun.peek())
        shotgun.invertCurrentAmmo()
        assertEquals(AmmoType.LIVE, shotgun.peek())
    }

    @Test
    fun `test find first live for phone`()
    {
        val shotgun = Shotgun()
        shotgun.load(0, 2) // two BLANK
        assertEquals(-1, shotgun.findFirstLive())

        val loaded = Shotgun()
        loaded.load(1, 0)
        assertEquals(0, loaded.findFirstLive())
    }
}
