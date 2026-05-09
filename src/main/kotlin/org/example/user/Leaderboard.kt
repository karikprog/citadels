package org.example.user

import org.example.repository.MatchRepository
import org.example.repository.MatchSummary
import java.util.UUID

class LeaderboardService(private val _mathRepository: MatchRepository) {
    fun getTopPlayers(limit: Int): List<User> {
        TODO("Implement")
    }

    fun getUserStats(userId: UUID): UserStats {
        TODO("Implementation")
    }

    fun updateRatingsAfterMatch(matchSummary: MatchSummary) {
        TODO("Imp")
    }
}

data class User(
    val id: UUID,
    val name: String,
    var rating: Int,
    var totalGames: Int
)

data class UserStats(
    val id: UUID,
    val totalGames: Int,
    val winGames: Int,
    val rating: Int
)