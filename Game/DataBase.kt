import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.UUID

data class MatchLog(
    val matchId: UUID,
    val winnerName: String,
    val timestamp: Long,
    val gameNumber: Int,
    val participants: List<String>
)

object PlayersTable : Table("players") {
    val name = varchar("name", 10)
    val wins = integer("wins").default(0)
    val totalGames = integer("total_games").default(0)
    override val primaryKey = PrimaryKey(name)
}

object MatchHistoryTable : Table("match_history") {
    val matchId = uuid("match_id")
    val winnerName = varchar("winner_name", 10)
    val timestamp = long("timestamp")
    override val primaryKey = PrimaryKey(matchId)
}

object MatchParticipantsTable : Table("match_participants") {
    val matchId = uuid("match_id")
    val participantName = varchar("participant_name", 10)
    override val primaryKey = PrimaryKey(matchId, participantName)
}

class StatisticsService(
    dbUrl: String = "jdbc:sqlite:./stats.db"
) {
    init {
        Database.connect(dbUrl, "org.sqlite.JDBC")
        transaction {
            SchemaUtils.createMissingTablesAndColumns(PlayersTable, MatchHistoryTable, MatchParticipantsTable)
        }
    }

    private fun ensurePlayerExists(playerName: String) {
        val exists = transaction {
            PlayersTable.selectAll().where { PlayersTable.name eq playerName }.any()
        }
        if (!exists) {
            transaction {
                PlayersTable.insert {
                    it[name] = playerName
                    it[wins] = 0
                    it[totalGames] = 0
                }
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
        val matchId = UUID.randomUUID()

        // Сохраняем матч
        MatchHistoryTable.insert {
            it[MatchHistoryTable.matchId] = matchId
            it[MatchHistoryTable.winnerName] = winnerName
            it[timestamp] = System.currentTimeMillis()
        }

        // Сохраняем участников
        participants.forEach { participantName ->
            ensurePlayerExists(participantName)
            MatchParticipantsTable.insert {
                it[MatchParticipantsTable.matchId] = matchId
                it[MatchParticipantsTable.participantName] = participantName
            }
        }

        // Обновляем статистику - ИСПРАВЛЕННЫЙ ВАРИАНТ
        participants.forEach { pName ->
            // Получаем текущие значения
            val currentStats = PlayersTable
                .selectAll()
                .where { PlayersTable.name eq pName }
                .single()

            val newTotalGames = currentStats[PlayersTable.totalGames] + 1
            val newWins = if (pName == winnerName) {
                currentStats[PlayersTable.wins] + 1
            } else {
                currentStats[PlayersTable.wins]
            }

            // Обновляем записи
            PlayersTable.update({ PlayersTable.name eq pName }) {
                it[totalGames] = newTotalGames
                it[wins] = newWins
            }
        }
    }

    fun getLeaderboard(): List<Pair<String, Statistics>> = transaction {
        PlayersTable.selectAll()
            .orderBy(PlayersTable.wins to SortOrder.DESC)
            .map { it[PlayersTable.name] to Statistics(it[PlayersTable.wins], it[PlayersTable.totalGames]) }
    }

    fun getMatchHistory(): List<MatchLog> = transaction {
        val totalCount = MatchHistoryTable.selectAll().count().toInt()

        MatchHistoryTable.selectAll()
            .orderBy(MatchHistoryTable.timestamp to SortOrder.DESC)
            .mapIndexed { index, matchRow ->
                val matchId = matchRow[MatchHistoryTable.matchId]

                val participants = MatchParticipantsTable
                    .select(MatchParticipantsTable.participantName)
                    .where { MatchParticipantsTable.matchId eq matchId }
                    .map { it[MatchParticipantsTable.participantName] }
                    .sorted()

                MatchLog(
                    matchId = matchId,
                    winnerName = matchRow[MatchHistoryTable.winnerName],
                    timestamp = matchRow[MatchHistoryTable.timestamp],
                    gameNumber = totalCount - index,
                    participants = participants
                )
            }
    }
}
