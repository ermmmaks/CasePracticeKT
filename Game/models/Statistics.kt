package game.models

class Statistics
(
    var wins: Int = 0,
    var totalGames: Int = 0
) {
    fun calculateWinRate(): Double
    {
        if (totalGames == 0) {
            return 0.0
        }

        val rate = wins.toDouble() / totalGames
        return rate
    }
    val formattedWinRate: String
        get() = "${(calculateWinRate() * 100).toInt()}%"
}