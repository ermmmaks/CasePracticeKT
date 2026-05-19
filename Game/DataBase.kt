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
    val matchId = uuid("match_id")
    val winnerName = varchar("winner_name", 50)
    val timestamp = long("timestamp")
    override val primaryKey = PrimaryKey(matchId)
}

class StatisticsService(
    dbUrl: String = "jdbc:sqlite:./stats.db"
) {
    init {
        Database.connect(dbUrl, "org.sqlite.JDBC")
        transaction {
            SchemaUtils.createMissingTablesAndColumns(PlayersTable, MatchHistoryTable)
        }
    }

    private fun ensurePlayerExists(playerName: String) {
        val exists = PlayersTable.selectAll().where { PlayersTable.name eq playerName }.any()
        if (!exists) {
            PlayersTable.insert {
                it[name] = playerName
                it[wins] = 0
                it[totalGames] = 0
            }
        }
    }

    fun getOrCreateStats(playerName: String): Statistics = transaction {
        ensurePlayerExists(playerName)

        PlayersTable.selectAll()
            .where { PlayersTable.name eq playerName }
            .map { Statistics(it[PlayersTable.wins], it[PlayersTable.totalGames]) }
            .single()
    }

    fun updateStats(winnerName: String, participants: List<String>) = transaction {
        MatchHistoryTable.insert {
            it[matchId] = UUID.randomUUID()
            it[MatchHistoryTable.winnerName] = winnerName
            it[timestamp] = System.currentTimeMillis()
        }

        participants.forEach { pName ->
            ensurePlayerExists(pName)

            PlayersTable.update({ PlayersTable.name eq pName }) {
                with(SqlExpressionBuilder) {
                    it[totalGames] = PlayersTable.totalGames + 1

                    if (pName == winnerName) {
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
