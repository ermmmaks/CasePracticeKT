import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.plus
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

class StatisticsService {
    init {
        Database.connect("jdbc:sqlite:./stats.db", "org.sqlite.JDBC")
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
        MatchHistoryTable.insert {
            it[matchId] = UUID.randomUUID()
            it[MatchHistoryTable.winnerName] = winnerName
            it[timestamp] = System.currentTimeMillis()
        }

        participants.forEach { pName ->
            PlayersTable.update({ PlayersTable.name eq pName }) {
                it[PlayersTable.totalGames] = PlayersTable.totalGames + 1
                if (pName == winnerName) {
                    it[PlayersTable.wins] = PlayersTable.wins + 1
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