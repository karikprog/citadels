package org.example.repository

import org.example.engine.GameAction
import org.example.engine.Player
import java.util.UUID
import kotlin.time.Clock
import kotlin.time.Instant

interface MatchRepository {
    fun saveRecordAction(matchId: UUID, playerId: UUID, actionType: String, actionCommand: String)
    fun saveCompletedMatch(matchId: UUID, summary: MatchSummary)
    fun getMatchHistoryForUser(userId: UUID): List<MatchSummary>
    fun getMatchReplay(matchId: UUID): List<String>

    fun getOrCreateUser(name: String): User
    fun findUserByName(name: String): User?
    fun getTopPlayers(limit: Int): List<User>
    fun getUserStats(userId: UUID): UserStats
    fun updateRatingsAfterMatch(matchSummary: MatchSummary)
}

class HistoryRecorder(private val matchId: UUID, private val repo: MatchRepository) {
    fun record(player: Player, action: GameAction, command: String) {
        repo.saveRecordAction(
            matchId,
            player.id,
            action::class.simpleName ?: "UnknownAction",
            command
        )
    }
}

data class MatchSummary(
    val matchId: UUID,
    val winnerUserId: UUID,
    val finalScore: Map<UUID, Int>,
    val playerNames: Map<UUID, String> = emptyMap(),
    val datePlayed: Instant = Clock.System.now()
)

data class User(
    val id: UUID,
    val name: String,
    var totalGames: Int,
    var winGames: Int
) {
    val rating: Int get() = if (totalGames > 0) (winGames * 100) / totalGames else 0
}

data class UserStats(
    val id: UUID,
    val totalGames: Int,
    val winGames: Int,
    val losses: Int,
    val rating: Int
)
