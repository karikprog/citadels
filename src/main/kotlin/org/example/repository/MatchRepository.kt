package org.example.repository

import java.util.UUID
import kotlin.time.Clock
import kotlin.time.Instant

interface MatchRepository {
    fun saveRecordAction(matchId: UUID)
    fun saveCompletedMatch(matchId: UUID, summary: MatchSummary)
    fun getMatchHistoryForUser(userId: UUID): List<MatchSummary>
    fun getMatchReplay(matchId: UUID)
}

class HistoryRecorder(private val _matchId: UUID, private val _matchRepo: MatchRepository) {
    fun record() {
        TODO("implementation")
    }
}

class MatchReplayer(private val _matchRepo: MatchRepository) {
    fun replayMatch(matchId: UUID) {
        TODO("Impl")
    }
}

data class MatchSummary(
    val matchId: UUID,
    val winnerUserId: UUID,
    val finalScore: Map<UUID, Int>,
    val datePlayed: Instant = Clock.System.now()
)