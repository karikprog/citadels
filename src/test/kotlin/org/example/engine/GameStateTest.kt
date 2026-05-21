package org.example.engine

import org.junit.jupiter.api.Test
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GameStateTest {

    private fun createPlayers(count: Int = 6): List<Player> {
        return (1..count).map { Player(UUID.randomUUID(), "P$it") }
    }

    private fun district(type: DistrictType, cost: Int = 1, color: Color = Color.GREEN): District {
        return StandardDistrict(type, "Test-$type", cost, color)
    }

    @Test
    fun `initial state has 6 players`() {
        val state = GameState(createPlayers())
        assertEquals(6, state.players.size)
        assertEquals(-1, state.kingInd)
        assertNull(state.activePlayer)
        assertNull(state.gamePhase)
        assertFalse(state.gameOver)
        assertEquals(0, state.availableCharacter.size)
    }

    @Test
    fun `addDistrict and selectDistrict work as queue`() {
        val state = GameState(createPlayers())
        val d1 = district(DistrictType.TAVERN)
        val d2 = district(DistrictType.MARKET)
        state.addDistrict(d1)
        state.addDistrict(d2)
        assertEquals(d1, state.selectDistrict())
        assertEquals(d2, state.selectDistrict())
    }

    @Test
    fun `selectDistrict removes from queue`() {
        val state = GameState(createPlayers())
        state.addDistrict(district(DistrictType.TAVERN))
        state.selectDistrict()
        assertTrue(state.isDistrictDeckEmpty())
    }

    @Test
    fun `addDraftCharacter adds only in DraftPhase`() {
        val state = GameState(createPlayers())
        state.addDraftCharacter(King)
        assertEquals(0, state.availableCharacter.size)

        state.changeGamePhase(DraftPhase)
        state.addDraftCharacter(King)
        assertEquals(1, state.availableCharacter.size)
    }

    @Test
    fun `selectCharacter removes from available`() {
        val state = GameState(createPlayers())
        state.changeGamePhase(DraftPhase)
        val king = King
        state.addDraftCharacter(king)
        state.selectCharacter(king)
        assertEquals(0, state.availableCharacter.size)
    }

    @Test
    fun `clearAvailableCharacter empties the list`() {
        val state = GameState(createPlayers())
        state.changeGamePhase(DraftPhase)
        state.addDraftCharacter(King)
        state.addDraftCharacter(Thief)
        state.clearAvailableCharacter()
        assertEquals(0, state.availableCharacter.size)
    }

    @Test
    fun `switchDraftPlayer cycles through players`() {
        val players = createPlayers(6)
        val state = GameState(players)
        state.activePlayer = players[0]
        state.switchDraftPlayer()
        assertEquals(players[1], state.activePlayer)
        state.switchDraftPlayer()
        assertEquals(players[2], state.activePlayer)
    }

    @Test
    fun `switchDraftPlayer wraps around`() {
        val players = createPlayers(6)
        val state = GameState(players)
        state.activePlayer = players[5]
        state.switchDraftPlayer()
        assertEquals(players[0], state.activePlayer)
    }

    @Test
    fun `changeGamePhase switches phase`() {
        val state = GameState(createPlayers())
        assertNull(state.gamePhase)
        state.changeGamePhase(DraftPhase)
        assertTrue(state.gamePhase is DraftPhase)
        state.changeGamePhase(TurnPhase)
        assertTrue(state.gamePhase is TurnPhase)
    }

    @Test
    fun `gameOver sets flag`() {
        val state = GameState(createPlayers())
        state.gameOver()
        assertTrue(state.gameOver)
    }

    @Test
    fun `DraftPhase handle populates 7 characters for 6 players`() {
        val players = createPlayers(6)
        val state = GameState(players)
        state.changeGamePhase(DraftPhase)
        DraftPhase.handle(state)
        assertTrue(state.gamePhase is DraftPhase)
        assertEquals(7, state.availableCharacter.size)
        val ranks = state.availableCharacter.map { it.rank }.toSet()
        assertEquals(7, ranks.size)
        assertTrue(ranks.all { it in 1..8 })
    }

    @Test
    fun `DraftPhase handle sets kingInd if not set`() {
        val players = createPlayers(6)
        val state = GameState(players)
        assertEquals(-1, state.kingInd)
        DraftPhase.handle(state)
        assertTrue(state.kingInd in 0..5)
    }

    @Test
    fun `DraftPhase handle sets activePlayer to king`() {
        val players = createPlayers(6)
        val state = GameState(players)
        DraftPhase.handle(state)
        assertEquals(state.players[state.kingInd], state.activePlayer)
    }

    @Test
    fun `DraftPhase handle resets all player characters`() {
        val players = createPlayers(6)
        players[0].setCharacter(King)
        val state = GameState(players)
        DraftPhase.handle(state)
        for (p in players) {
            assertEquals(0, p.character)
        }
    }

    @Test
    fun `TurnPhase handle sets activePlayer to null`() {
        val players = createPlayers(6)
        val state = GameState(players)
        state.activePlayer = players[0]
        state.changeGamePhase(TurnPhase)
        TurnPhase.handle(state)
        assertTrue(state.gamePhase is TurnPhase)
        assertNull(state.activePlayer)
    }
}
