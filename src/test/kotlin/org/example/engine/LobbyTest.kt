package org.example.engine

import org.example.utils.InMemoryMatchRepository
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class LobbyTest {
    private val repo = InMemoryMatchRepository()

    @Test
    fun `addPlayer adds up to 6 players`() {
        val lobby = Lobby(repo)
        for (i in 1..6) {
            lobby.addPlayer("Player$i")
        }
        assertEquals(6, lobby.playersSize)
    }

    @Test
    fun `addPlayer ignores 7th player`() {
        val lobby = Lobby(repo)
        for (i in 1..7) {
            lobby.addPlayer("Player$i")
        }
        assertEquals(6, lobby.playersSize)
    }

    @Test
    fun `createGameState returns GameState with same players`() {
        val lobby = Lobby(repo)
        lobby.addPlayer("Alice")
        lobby.addPlayer("Bob")
        lobby.addPlayer("Charlie")
        lobby.addPlayer("Diana")
        lobby.addPlayer("Eve")
        lobby.addPlayer("Frank")
        val state = lobby.createGameState()
        assertEquals(6, state.players.size)
        assertEquals("Alice", state.players[0].name)
        assertEquals("Bob", state.players[1].name)
        assertEquals("Frank", state.players[5].name)
    }

    @Test
    fun `empty lobby creates empty game state`() {
        val lobby = Lobby(repo)
        val state = lobby.createGameState()
        assertEquals(0, state.players.size)
    }
}
