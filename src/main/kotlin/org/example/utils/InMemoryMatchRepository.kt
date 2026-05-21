package org.example.utils

import org.example.repository.MatchRepository
import org.example.repository.MatchSummary
import org.example.repository.User
import org.example.repository.UserStats
import java.util.UUID

class InMemoryMatchRepository : MatchRepository {
    private val _matches = mutableListOf<MatchSummary>()
    private val _replays = mutableMapOf<UUID, MutableList<String>>()
    private val _users = mutableMapOf<UUID, User>()

    override fun saveRecordAction(matchId: UUID, actionDescription: String) {
        _replays.getOrPut(matchId) { mutableListOf() }.add(actionDescription)
    }

    override fun saveCompletedMatch(matchId: UUID, summary: MatchSummary) {
        _matches.add(summary)
    }

    override fun getMatchHistoryForUser(userId: UUID): List<MatchSummary> {
        return _matches.filter { summary ->
            summary.winnerUserId == userId || summary.finalScore.containsKey(userId)
        }
    }

    override fun getMatchReplay(matchId: UUID): List<String> {
        return _replays[matchId]?.toList() ?: emptyList()
    }

    override fun getOrCreateUser(id: UUID, name: String): User {
        return _users.getOrPut(id) { User(id, name, 0, 0) }
    }

    override fun findUserByName(name: String): User? {
        return _users.values.find { it.name.equals(name, ignoreCase = true) }
    }

    override fun getTopPlayers(limit: Int): List<User> {
        return _users.values
            .filter { it.totalGames > 0 }
            .sortedByDescending { it.rating }
            .take(limit)
    }

    override fun getUserStats(userId: UUID): UserStats {
        val user = _users[userId]
        if (user == null || user.totalGames == 0) {
            return UserStats(userId, 0, 0, 0, 0)
        }
        return UserStats(
            id = userId,
            totalGames = user.totalGames,
            winGames = user.winGames,
            losses = user.totalGames - user.winGames,
            rating = user.rating
        )
    }

    override fun updateRatingsAfterMatch(matchSummary: MatchSummary) {
        for ((playerId, playerName) in matchSummary.playerNames) {
            val user = getOrCreateUser(playerId, playerName)
            user.totalGames++
            if (playerId == matchSummary.winnerUserId) {
                user.winGames++
            }
        }
    }
}
