package org.example.utils

import org.example.repository.MatchRepository
import org.example.repository.MatchSummary
import org.example.repository.User
import org.example.repository.UserStats
import java.io.File
import java.sql.Connection
import java.sql.DriverManager
import java.util.UUID
import kotlin.time.Instant

class SqliteMatchRepository(dbPath: String = "data/citadels.db") : MatchRepository, AutoCloseable {

    private val connection: Connection

    init {
        val dir = File(dbPath).parentFile
        if (dir != null && !dir.exists()) dir.mkdirs()

        connection = DriverManager.getConnection("jdbc:sqlite:$dbPath").also { conn ->
            conn.createStatement().use { stmt ->
                stmt.execute("PRAGMA busy_timeout=5000")
                stmt.execute("PRAGMA journal_mode=WAL")
                stmt.execute("PRAGMA foreign_keys=ON")
            }
            createTables(conn)
        }
    }

    override fun close() {
        if (!connection.isClosed) connection.close()
    }

    private fun createTables(conn: Connection) {
        listOf(
            """
            CREATE TABLE IF NOT EXISTS users (
                id          TEXT PRIMARY KEY,
                name        TEXT NOT NULL UNIQUE,
                total_games INTEGER NOT NULL DEFAULT 0,
                win_games   INTEGER NOT NULL DEFAULT 0
            )
            """,
            """
            CREATE TABLE IF NOT EXISTS match_history (
                id          TEXT    PRIMARY KEY,
                date_played INTEGER NOT NULL
            )
            """,
            """
            CREATE TABLE IF NOT EXISTS match_players (
                match_id    TEXT NOT NULL,
                player_id   TEXT NOT NULL,
                player_name TEXT NOT NULL,
                score       INTEGER NOT NULL,
                is_winner   INTEGER NOT NULL DEFAULT 0,
                PRIMARY KEY (match_id, player_id),
                FOREIGN KEY (match_id) REFERENCES match_history(id)
            )
            """,
            """
            CREATE INDEX IF NOT EXISTS idx_match_players_player_id
            ON match_players(player_id)
            """,
            """
            CREATE TABLE IF NOT EXISTS match_actions (
                id             INTEGER PRIMARY KEY AUTOINCREMENT,
                match_id       TEXT    NOT NULL,
                player_id      TEXT    NOT NULL,
                action_order   INTEGER NOT NULL,
                action_type    TEXT    NOT NULL,
                action_command TEXT    NOT NULL,
                FOREIGN KEY (player_id) REFERENCES users(id)
            )
            """,
            """
            CREATE INDEX IF NOT EXISTS idx_match_actions_match_id
            ON match_actions(match_id)
            """
        ).forEach { sql ->
            conn.createStatement().use { it.execute(sql.trimIndent()) }
        }
    }

    private fun <T> withTransaction(block: (Connection) -> T): T {
        val prevAutoCommit = connection.autoCommit
        connection.autoCommit = false
        return try {
            val result = block(connection)
            connection.commit()
            result
        } catch (e: Exception) {
            connection.rollback()
            throw e
        } finally {
            connection.autoCommit = prevAutoCommit
        }
    }

    override fun saveRecordAction(matchId: UUID, playerId: UUID, actionType: String, actionCommand: String) {
        connection.prepareStatement("""
            INSERT INTO match_actions (match_id, player_id, action_order, action_type, action_command)
            VALUES (?, ?, (SELECT COALESCE(MAX(action_order) + 1, 0) FROM match_actions WHERE match_id = ?), ?, ?)
        """.trimIndent()).use { stmt ->
            stmt.setString(1, matchId.toString())
            stmt.setString(2, playerId.toString())
            stmt.setString(3, matchId.toString())
            stmt.setString(4, actionType)
            stmt.setString(5, actionCommand)
            stmt.executeUpdate()
        }
    }

    override fun saveCompletedMatch(matchId: UUID, summary: MatchSummary) {
        withTransaction { conn ->
            conn.prepareStatement(
                "INSERT INTO match_history (id, date_played) VALUES (?, ?)"
            ).use { stmt ->
                stmt.setString(1, summary.matchId.toString())
                stmt.setLong(2, summary.datePlayed.toEpochMilliseconds())
                stmt.executeUpdate()
            }

            conn.prepareStatement(
                "INSERT INTO match_players (match_id, player_id, player_name, score, is_winner) VALUES (?, ?, ?, ?, ?)"
            ).use { stmt ->
                for ((playerId, score) in summary.finalScore) {
                    stmt.setString(1, summary.matchId.toString())
                    stmt.setString(2, playerId.toString())
                    stmt.setString(3, summary.playerNames[playerId] ?: "Unknown")
                    stmt.setInt(4, score)
                    stmt.setInt(5, if (playerId == summary.winnerUserId) 1 else 0)
                    stmt.addBatch()
                }
                stmt.executeBatch()
            }
        }
    }

    override fun getMatchHistoryForUser(userId: UUID): List<MatchSummary> {
        data class Row(
            val matchId: UUID,
            val playerId: UUID,
            val playerName: String,
            val score: Int,
            val isWinner: Boolean,
            val datePlayed: Instant,
        )

        val rows = connection.prepareStatement("""
            SELECT mh.id, mh.date_played,
                   mp.player_id, mp.player_name, mp.score, mp.is_winner
            FROM match_history mh
            JOIN match_players mp ON mh.id = mp.match_id
            WHERE mh.id IN (
                SELECT match_id FROM match_players WHERE player_id = ?
            )
            ORDER BY mh.date_played DESC
        """.trimIndent()).use { stmt ->
            stmt.setString(1, userId.toString())
            stmt.executeQuery().use { rs ->
                buildList {
                    while (rs.next()) {
                        add(Row(
                            matchId    = UUID.fromString(rs.getString("id")),
                            playerId   = UUID.fromString(rs.getString("player_id")),
                            playerName = rs.getString("player_name"),
                            score      = rs.getInt("score"),
                            isWinner   = rs.getInt("is_winner") == 1,
                            datePlayed = Instant.fromEpochMilliseconds(rs.getLong("date_played")),
                        ))
                    }
                }
            }
        }

        return rows.groupBy { it.matchId }.map { (matchId, matchRows) ->
            MatchSummary(
                matchId      = matchId,
                winnerUserId = matchRows.firstOrNull { it.isWinner }?.playerId ?: NO_WINNER,
                finalScore   = matchRows.associate { it.playerId to it.score },
                playerNames  = matchRows.associate { it.playerId to it.playerName },
                datePlayed   = matchRows.first().datePlayed,
            )
        }
    }

    override fun getMatchReplay(matchId: UUID): List<String> {
        return connection.prepareStatement("""
            SELECT action_command FROM match_actions
            WHERE match_id = ?
            ORDER BY action_order ASC
        """.trimIndent()).use { stmt ->
            stmt.setString(1, matchId.toString())
            stmt.executeQuery().use { rs ->
                buildList {
                    while (rs.next()) add(rs.getString("action_command"))
                }
            }
        }
    }

    override fun getOrCreateUser(name: String): User {
        return withTransaction { conn ->
            conn.prepareStatement(
                "INSERT OR IGNORE INTO users (id, name, total_games, win_games) VALUES (?, ?, 0, 0)"
            ).use { stmt ->
                stmt.setString(1, UUID.randomUUID().toString())
                stmt.setString(2, name)
                stmt.executeUpdate()
            }

            conn.prepareStatement(
                "SELECT id, name, total_games, win_games FROM users WHERE name = ?"
            ).use { stmt ->
                stmt.setString(1, name)
                stmt.executeQuery().use { rs ->
                    check(rs.next()) { "User '$name' not found after INSERT OR IGNORE" }
                    User(
                        id         = UUID.fromString(rs.getString("id")),
                        name       = rs.getString("name"),
                        totalGames = rs.getInt("total_games"),
                        winGames   = rs.getInt("win_games"),
                    )
                }
            }
        }
    }

    override fun findUserByName(name: String): User? {
        return connection.prepareStatement(
            "SELECT id, name, total_games, win_games FROM users WHERE name = ?"
        ).use { stmt ->
            stmt.setString(1, name)
            stmt.executeQuery().use { rs ->
                if (rs.next()) User(
                    id         = UUID.fromString(rs.getString("id")),
                    name       = rs.getString("name"),
                    totalGames = rs.getInt("total_games"),
                    winGames   = rs.getInt("win_games"),
                ) else null
            }
        }
    }

    override fun getTopPlayers(limit: Int): List<User> {
        return connection.prepareStatement("""
            SELECT id, name, total_games, win_games
            FROM users
            WHERE total_games > 0
            ORDER BY (win_games * 100 / total_games) DESC
            LIMIT ?
        """.trimIndent()).use { stmt ->
            stmt.setInt(1, limit)
            stmt.executeQuery().use { rs ->
                buildList {
                    while (rs.next()) add(User(
                        id         = UUID.fromString(rs.getString("id")),
                        name       = rs.getString("name"),
                        totalGames = rs.getInt("total_games"),
                        winGames   = rs.getInt("win_games"),
                    ))
                }
            }
        }
    }

    override fun getUserStats(userId: UUID): UserStats {
        return connection.prepareStatement(
            "SELECT total_games, win_games FROM users WHERE id = ?"
        ).use { stmt ->
            stmt.setString(1, userId.toString())
            stmt.executeQuery().use { rs ->
                if (rs.next()) {
                    val totalGames = rs.getInt("total_games")
                    val winGames   = rs.getInt("win_games")
                    UserStats(
                        id         = userId,
                        totalGames = totalGames,
                        winGames   = winGames,
                        losses     = totalGames - winGames,
                        rating     = if (totalGames > 0) (winGames * 100) / totalGames else 0,
                    )
                } else {
                    UserStats(userId, 0, 0, 0, 0)
                }
            }
        }
    }

    override fun updateRatingsAfterMatch(matchSummary: MatchSummary) {
        withTransaction { conn ->
            conn.prepareStatement("""
                INSERT INTO users (id, name, total_games, win_games) VALUES (?, ?, 1, ?)
                ON CONFLICT(id) DO UPDATE SET
                    total_games = total_games + 1,
                    win_games   = win_games   + excluded.win_games
            """.trimIndent()).use { stmt ->
                for ((playerId, playerName) in matchSummary.playerNames) {
                    val isWinner = if (playerId == matchSummary.winnerUserId) 1 else 0
                    stmt.setString(1, playerId.toString())
                    stmt.setString(2, playerName)
                    stmt.setInt(3, isWinner)
                    stmt.addBatch()
                }
                stmt.executeBatch()
            }
        }
    }

    companion object {
        val NO_WINNER: UUID = UUID(0L, 0L)
    }
}
