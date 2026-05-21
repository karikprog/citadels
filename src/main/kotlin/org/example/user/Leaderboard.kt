package org.example.user

import org.example.repository.MatchRepository
import org.example.repository.MatchSummary
import org.example.repository.User
import org.example.repository.UserStats
import java.util.UUID

class LeaderboardService(private val repo: MatchRepository) {

    fun getTopPlayers(limit: Int): List<User> {
        return repo.getTopPlayers(limit)
    }

    fun getUserStats(userId: UUID): UserStats {
        return repo.getUserStats(userId)
    }

    fun updateRatingsAfterMatch(matchSummary: MatchSummary) {
        repo.updateRatingsAfterMatch(matchSummary)
    }
}
