import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import java.io.File
import java.util.UUID

class DBSpecificationTest {
    private lateinit var dbService: StatisticsService
    private lateinit var tempDbFile: File

    @BeforeEach
    fun setup() {
        // Generate a completely isolated temporary file name for each test
        val uniqueName = "test_db_${UUID.randomUUID()}.db"
        tempDbFile = File(uniqueName)

        // Initialize the service targeting the sterile temporary database file
        dbService = StatisticsService("jdbc:sqlite:${tempDbFile.absolutePath}")
    }

    @AfterEach
    fun tearDown() {
        // Explicitly clean up and physically delete the temporary database file
        if (tempDbFile.exists()) {
            tempDbFile.delete()
        }
    }

    @Test
    fun `test user registration and statistics initializing`() {
        val stats = dbService.getOrCreateStats("NewSoul")
        assertEquals(0, stats.wins)
        assertEquals(0, stats.totalGames)
        assertEquals(0.0, stats.calculateWinRate())
    }

    @Test
    fun `test win and loss recording and calculating correct`() {
        dbService.getOrCreateStats("Winner")
        dbService.getOrCreateStats("Loser")

        dbService.updateStats("Winner", listOf("Winner", "Loser"))

        val winnerStats = dbService.getOrCreateStats("Winner")
        assertEquals(1, winnerStats.wins)
        assertEquals(1, winnerStats.totalGames)

        val loserStats = dbService.getOrCreateStats("Loser")
        assertEquals(0, loserStats.wins)
        assertEquals(1, loserStats.totalGames)
        assertEquals(0.0, loserStats.calculateWinRate())
    }

    @Test
    fun `test leaderboard order`() {
        dbService.getOrCreateStats("MaksPro")
        dbService.getOrCreateStats("PoorNoob")
        dbService.getOrCreateStats("Normise")
        dbService.getOrCreateStats("Undefined")

        dbService.updateStats("MaksPro", listOf("MaksPro", "Normise", "PoorNoob", "Undefined"))
        dbService.updateStats("MaksPro", listOf("MaksPro", "Normise"))
        dbService.updateStats("MaksPro", listOf("MaksPro", "Undefined"))

        dbService.updateStats("Normise", listOf("MaksPro", "Normise", "PoorNoob"))
        dbService.updateStats("Normise", listOf("Normise", "Undefined"))

        dbService.updateStats("Undefined", listOf("PoorNoob", "Undefined"))

        val leaderboard = dbService.getLeaderboard()

        assertEquals(4, leaderboard.size)

        assertEquals("MaksPro", leaderboard[0].first)
        assertEquals(3, leaderboard[0].second.wins)
        assertEquals(4, leaderboard[0].second.totalGames)

        assertEquals("Normise", leaderboard[1].first)
        assertEquals(2, leaderboard[1].second.wins)
        assertEquals(4, leaderboard[1].second.totalGames)

        assertEquals("Undefined", leaderboard[2].first)
        assertEquals(1, leaderboard[2].second.wins)
        assertEquals(4, leaderboard[2].second.totalGames)

        assertEquals("PoorNoob", leaderboard[3].first)
        assertEquals(0, leaderboard[3].second.wins)
        assertEquals(3, leaderboard[3].second.totalGames)
    }
}
