import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.UUID

object PlayersTable : Table("players") {
    val name = varchar("name", 50)
    val wins = integer("wins").default(0)
    val totalGames = integer("total_games").default(0)
    override val primaryKey = PrimaryKey(name)
}

object MatchHistoryTable : Table("match_history") {
    val matchId = uuid("math_id")
    val winnerName = varchar("winner_name", 50)
    val timestamp = long("timestamp")
    override val primaryKey = PrimaryKey(matchId)
}

class StatisticsService(
    // Default production URL for the actual application session
    dbUrl: String = "jdbc:sqlite:./stats.db"
) {
    init {
        // Dynamically connect using the passed parameter
        Database.connect(dbUrl, "org.sqlite.JDBC")
        transaction {
            SchemaUtils.create(PlayersTable, MatchHistoryTable)
        }
    }

    fun getOrCreateStats(playerName: String): Statistics = transaction {
        val row = PlayersTable.selectAll().where { PlayersTable.name eq playerName }.singleOrNull()
        if (row == null) {
            PlayersTable.insert {
                it[name] = playerName
                it[wins] = 0
                it[totalGames] = 0
            }
            Statistics(0, 0)
        } else {
            Statistics(row[PlayersTable.wins], row[PlayersTable.totalGames])
        }
    }

    fun updateStats(winnerName: String, participants: List<String>) = transaction {
        // Save match history metadata
        MatchHistoryTable.insert {
            it[matchId] = UUID.randomUUID()
            it[MatchHistoryTable.winnerName] = winnerName
            it[timestamp] = System.currentTimeMillis()
        }

        // Loop through each participant to perform a safe atomic update
        participants.forEach { pName ->
            // Ensure the player profile exists in the database first
            getOrCreateStats(pName)

            // Perform direct atomic SQL increment without using CustomOperator
            PlayersTable.update({ PlayersTable.name eq pName }) {
                with(SqlExpressionBuilder) {
                    // This generates pure "total_games = total_games + 1" in SQL
                    it[totalGames] = PlayersTable.totalGames + 1

                    if (pName == winnerName) {
                        // This generates pure "wins = wins + 1" in SQL
                        it[wins] = PlayersTable.wins + 1
                    }
                }
            }
        }
    }

    fun getLeaderboard(): List<Pair<String, Statistics>> = transaction {
        PlayersTable.selectAll()
            .orderBy(PlayersTable.wins to SortOrder.DESC)
            .map { it[PlayersTable.name] to Statistics(it[PlayersTable.wins], it[PlayersTable.totalGames]) }
    }
}