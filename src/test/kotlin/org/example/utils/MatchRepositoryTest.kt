package org.example.utils

import org.example.repository.MatchRepository
import org.example.repository.MatchSummary
import org.junit.jupiter.api.Test
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

abstract class MatchRepositoryTest {
    abstract fun createRepo(): MatchRepository

    @Test
    fun `getOrCreateUser creates new user`() {
        val repo = createRepo()
        val user = repo.getOrCreateUser("Alice")
        assertEquals("Alice", user.name)
        assertEquals(0, user.totalGames)
        assertEquals(0, user.winGames)
        assertNotNull(user.id)
    }

    @Test
    fun `getOrCreateUser returns existing user by name`() {
        val repo = createRepo()
        val u1 = repo.getOrCreateUser("Alice")
        val u2 = repo.getOrCreateUser("Alice")
        assertEquals(u1.id, u2.id)
        assertEquals(u1.name, u2.name)
    }

    @Test
    fun `getOrCreateUser creates separate users for different names`() {
        val repo = createRepo()
        val u1 = repo.getOrCreateUser("Alice")
        val u2 = repo.getOrCreateUser("Bob")
        assertTrue(u1.id != u2.id)
    }

    @Test
    fun `findUserByName finds existing user`() {
        val repo = createRepo()
        val created = repo.getOrCreateUser("Alice")
        val found = repo.findUserByName("Alice")
        assertNotNull(found)
        assertEquals(created.id, found.id)
        assertEquals("Alice", found.name)
    }

    @Test
    fun `findUserByName returns null for nonexistent user`() {
        val repo = createRepo()
        assertNull(repo.findUserByName("Nobody"))
    }

    @Test
    fun `saveRecordAction and getMatchReplay return actions in order`() {
        val repo = createRepo()
        val matchId = UUID.randomUUID()
        val player = repo.getOrCreateUser("Alice")

        repo.saveRecordAction(matchId, player.id, "BuildDistrictAction", "build CASTLE")
        repo.saveRecordAction(matchId, player.id, "CollectGoldAction", "gold")

        val replay = repo.getMatchReplay(matchId)
        assertEquals(2, replay.size)
        assertEquals("build CASTLE", replay[0])
        assertEquals("gold", replay[1])
    }

    @Test
    fun `getMatchReplay returns empty list for unknown match`() {
        val repo = createRepo()
        assertTrue(repo.getMatchReplay(UUID.randomUUID()).isEmpty())
    }

    @Test
    fun `saveCompletedMatch stores match and getMatchHistoryForUser finds it`() {
        val repo = createRepo()
        val matchId = UUID.randomUUID()
        val p1 = repo.getOrCreateUser("Alice")
        val p2 = repo.getOrCreateUser("Bob")

        val summary = MatchSummary(
            matchId = matchId,
            winnerUserId = p1.id,
            finalScore = mapOf(p1.id to 42, p2.id to 38),
            playerNames = mapOf(p1.id to "Alice", p2.id to "Bob")
        )
        repo.saveCompletedMatch(matchId, summary)

        val history = repo.getMatchHistoryForUser(p1.id)
        assertEquals(1, history.size)
        assertEquals(matchId, history[0].matchId)
        assertEquals(p1.id, history[0].winnerUserId)
    }

    @Test
    fun `getMatchHistoryForUser returns only matches where user participated`() {
        val repo = createRepo()
        val p1 = repo.getOrCreateUser("Alice")
        val p2 = repo.getOrCreateUser("Bob")
        val p3 = repo.getOrCreateUser("Charlie")

        val match1 = MatchSummary(
            matchId = UUID.randomUUID(),
            winnerUserId = p1.id,
            finalScore = mapOf(p1.id to 42, p2.id to 38),
            playerNames = mapOf(p1.id to "Alice", p2.id to "Bob")
        )
        repo.saveCompletedMatch(match1.matchId, match1)

        val match2 = MatchSummary(
            matchId = UUID.randomUUID(),
            winnerUserId = p3.id,
            finalScore = mapOf(p3.id to 50, p2.id to 30),
            playerNames = mapOf(p3.id to "Charlie", p2.id to "Bob")
        )
        repo.saveCompletedMatch(match2.matchId, match2)

        val aliceHistory = repo.getMatchHistoryForUser(p1.id)
        assertEquals(1, aliceHistory.size)
        assertEquals(match1.matchId, aliceHistory[0].matchId)

        val bobHistory = repo.getMatchHistoryForUser(p2.id)
        assertEquals(2, bobHistory.size)
    }

    @Test
    fun `getUserStats returns zeros for nonexistent user`() {
        val repo = createRepo()
        val stats = repo.getUserStats(UUID.randomUUID())
        assertEquals(0, stats.totalGames)
        assertEquals(0, stats.winGames)
        assertEquals(0, stats.losses)
        assertEquals(0, stats.rating)
    }

    @Test
    fun `getUserStats returns correct values after matches`() {
        val repo = createRepo()
        val p1 = repo.getOrCreateUser("Alice")
        val p2 = repo.getOrCreateUser("Bob")

        val match1 = MatchSummary(
            matchId = UUID.randomUUID(),
            winnerUserId = p1.id,
            finalScore = mapOf(p1.id to 42, p2.id to 38),
            playerNames = mapOf(p1.id to "Alice", p2.id to "Bob")
        )
        repo.saveCompletedMatch(match1.matchId, match1)
        repo.updateRatingsAfterMatch(match1)

        val stats1 = repo.getUserStats(p1.id)
        assertEquals(1, stats1.totalGames)
        assertEquals(1, stats1.winGames)
        assertEquals(0, stats1.losses)
        assertEquals(100, stats1.rating)

        val stats2 = repo.getUserStats(p2.id)
        assertEquals(1, stats2.totalGames)
        assertEquals(0, stats2.winGames)
        assertEquals(1, stats2.losses)
        assertEquals(0, stats2.rating)
    }

    @Test
    fun `getTopPlayers returns empty list when no players have games`() {
        val repo = createRepo()
        repo.getOrCreateUser("Alice")
        assertTrue(repo.getTopPlayers(10).isEmpty())
    }

    @Test
    fun `getTopPlayers sorted by rating descending`() {
        val repo = createRepo()
        val p1 = repo.getOrCreateUser("Alice")
        val p2 = repo.getOrCreateUser("Bob")

        val match = MatchSummary(
            matchId = UUID.randomUUID(),
            winnerUserId = p1.id,
            finalScore = mapOf(p1.id to 42, p2.id to 38),
            playerNames = mapOf(p1.id to "Alice", p2.id to "Bob")
        )
        repo.saveCompletedMatch(match.matchId, match)
        repo.updateRatingsAfterMatch(match)

        val top = repo.getTopPlayers(5)
        assertEquals(2, top.size)
        assertEquals("Alice", top[0].name)
        assertEquals("Bob", top[1].name)
    }

    @Test
    fun `getTopPlayers respects limit`() {
        val repo = createRepo()
        val p1 = repo.getOrCreateUser("Alice")
        val p2 = repo.getOrCreateUser("Bob")
        val p3 = repo.getOrCreateUser("Charlie")

        for (p in listOf(p1, p2, p3)) {
            val m = MatchSummary(
                matchId = UUID.randomUUID(),
                winnerUserId = p.id,
                finalScore = mapOf(p.id to 50),
                playerNames = mapOf(p.id to p.name)
            )
            repo.saveCompletedMatch(m.matchId, m)
            repo.updateRatingsAfterMatch(m)
        }

        assertEquals(2, repo.getTopPlayers(2).size)
        assertEquals(3, repo.getTopPlayers(5).size)
    }

    @Test
    fun `updateRatingsAfterMatch correctly updates win count for winner`() {
        val repo = createRepo()
        val winner = repo.getOrCreateUser("Winner")
        val loser = repo.getOrCreateUser("Loser")

        val match = MatchSummary(
            matchId = UUID.randomUUID(),
            winnerUserId = winner.id,
            finalScore = mapOf(winner.id to 50, loser.id to 30),
            playerNames = mapOf(winner.id to "Winner", loser.id to "Loser")
        )
        repo.saveCompletedMatch(match.matchId, match)
        repo.updateRatingsAfterMatch(match)

        assertEquals(1, repo.findUserByName("Winner")!!.totalGames)
        assertEquals(1, repo.findUserByName("Winner")!!.winGames)
        assertEquals(1, repo.findUserByName("Loser")!!.totalGames)
        assertEquals(0, repo.findUserByName("Loser")!!.winGames)
    }

    @Test
    fun `updateRatingsAfterMatch accumulates stats over multiple matches`() {
        val repo = createRepo()
        val p1 = repo.getOrCreateUser("Alice")
        val p2 = repo.getOrCreateUser("Bob")

        val match1 = MatchSummary(
            matchId = UUID.randomUUID(),
            winnerUserId = p1.id,
            finalScore = mapOf(p1.id to 42, p2.id to 38),
            playerNames = mapOf(p1.id to "Alice", p2.id to "Bob")
        )
        repo.saveCompletedMatch(match1.matchId, match1)
        repo.updateRatingsAfterMatch(match1)

        val match2 = MatchSummary(
            matchId = UUID.randomUUID(),
            winnerUserId = p2.id,
            finalScore = mapOf(p1.id to 35, p2.id to 45),
            playerNames = mapOf(p1.id to "Alice", p2.id to "Bob")
        )
        repo.saveCompletedMatch(match2.matchId, match2)
        repo.updateRatingsAfterMatch(match2)

        val aStats = repo.getUserStats(p1.id)
        assertEquals(2, aStats.totalGames)
        assertEquals(1, aStats.winGames)
        assertEquals(1, aStats.losses)
        assertEquals(50, aStats.rating)

        val bStats = repo.getUserStats(p2.id)
        assertEquals(2, bStats.totalGames)
        assertEquals(1, bStats.winGames)
        assertEquals(1, bStats.losses)
        assertEquals(50, bStats.rating)
    }

    @Test
    fun `full game flow with actions match and stats`() {
        val repo = createRepo()
        val matchId = UUID.randomUUID()
        val p1 = repo.getOrCreateUser("Alice")
        val p2 = repo.getOrCreateUser("Bob")

        repo.saveRecordAction(matchId, p1.id, "SelectCharacterAction", "draft 1")
        repo.saveRecordAction(matchId, p2.id, "SelectCharacterAction", "draft 2")
        repo.saveRecordAction(matchId, p1.id, "CollectGoldAction", "gold")
        repo.saveRecordAction(matchId, p2.id, "BuildDistrictAction", "build TEMPLE")

        val summary = MatchSummary(
            matchId = matchId,
            winnerUserId = p1.id,
            finalScore = mapOf(p1.id to 42, p2.id to 38),
            playerNames = mapOf(p1.id to "Alice", p2.id to "Bob")
        )
        repo.saveCompletedMatch(matchId, summary)
        repo.updateRatingsAfterMatch(summary)

        val replay = repo.getMatchReplay(matchId)
        assertEquals(4, replay.size)
        assertEquals("draft 1", replay[0])
        assertEquals("gold", replay[2])

        val aStats = repo.getUserStats(p1.id)
        assertEquals(1, aStats.totalGames)
        assertEquals(1, aStats.winGames)

        val history = repo.getMatchHistoryForUser(p1.id)
        assertEquals(1, history.size)
        assertEquals(matchId, history[0].matchId)
    }
}
