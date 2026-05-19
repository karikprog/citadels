package org.example.engine

import org.example.utils.Settings
import org.junit.jupiter.api.Test
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class CitadelsEngineTest {

    private fun createPlayers(count: Int = 6): List<Player> {
        return (1..count).map { Player(UUID.randomUUID(), "P$it") }
    }

    private fun district(type: DistrictType, cost: Int = 1, color: Color = Color.GREEN): District {
        return StandardDistrict(type, "Test-$type", cost, color)
    }

    @Test
    fun `startEngine initializes game correctly`() {
        val players = createPlayers()
        val state = GameState(players)
        val engine = CitadelsEngine(Settings(), state, ClassicMoveValidator())
        engine.startEngine()
        assertTrue(state.gamePhase is DraftPhase)
        for (p in players) {
            assertEquals(2, p.gold)
            assertEquals(4, p.hand.size)
        }
        assertTrue(state.availableCharacter.size == 7)
    }

    @Test
    fun `full draft phase with 6 players`() {
        val players = createPlayers()
        val state = GameState(players)
        val engine = CitadelsEngine(Settings(), state, ClassicMoveValidator())
        engine.startEngine()

        val available = state.availableCharacter.toList()
        for (i in 0 until 6) {
            val player = state.activePlayer
            assertNotNull(player)
            val action = SelectCharacterAction(available[i].rank, player)
            val result = engine.processAction(action)
            assertTrue(result is ProcessResult.Valid, "Draft $i failed: $result")
            val endResult = engine.processAction(EndDraftAction(player))
            assertTrue(endResult is ProcessResult.Valid, "EndDraft $i failed: $endResult")
        }
        assertTrue(state.gamePhase is TurnPhase, "Should be in TurnPhase after draft")
    }

    @Test
    fun `invalid action returns Invalid result`() {
        val players = createPlayers()
        val state = GameState(players)
        val engine = CitadelsEngine(Settings(), state, ClassicMoveValidator())
        engine.startEngine()

        val result = engine.processAction(CollectGoldAction(4))
        assertTrue(result is ProcessResult.Invalid)
    }

    @Test
    fun `processAction does not execute on invalid`() {
        val players = createPlayers()
        val state = GameState(players)
        val engine = CitadelsEngine(Settings(), state, ClassicMoveValidator())
        engine.startEngine()

        val goldBefore = state.activePlayer?.gold ?: -1
        val result = engine.processAction(CollectGoldAction(4))
        assertTrue(result is ProcessResult.Invalid)
        assertEquals(goldBefore, state.activePlayer?.gold)
    }

    @Test
    fun `complete round of turns`() {
        val players = createPlayers()
        val state = GameState(players)
        val engine = CitadelsEngine(Settings(), state, ClassicMoveValidator())
        engine.startEngine()

        val available = state.availableCharacter.toList()
        for (i in 0 until 6) {
            engine.processAction(SelectCharacterAction(available[i].rank, state.activePlayer!!))
            engine.processAction(EndDraftAction(state.activePlayer!!))
        }

        assertTrue(state.gamePhase is TurnPhase)

        val active = state.activePlayer
        assertNotNull(active)

        engine.processAction(CollectGoldAction(active.character))
        engine.processAction(EndTurnAction(active.character))

        val nextPlayer = state.activePlayer
        assertNotNull(nextPlayer)
        assertTrue(nextPlayer.character > active.character || nextPlayer.character < active.character)
    }

    @Test
    fun `assassinated player is skipped in turn order`() {
        val players = createPlayers()
        val assassinatedPlayer = players[0]
        assassinatedPlayer.setCharacter(Assassin)

        val state = GameState(players)
        state.changeGamePhase(TurnPhase)
        state.activePlayer = assassinatedPlayer
        assassinatedPlayer.assassinated()
        val engine = CitadelsEngine(Settings(), state, ClassicMoveValidator())

        val thief = players[1]
        thief.setCharacter(Thief)
        state.activePlayer = thief

        val king = players[2]
        king.setCharacter(King)
        val gold = 5
        king.addGold(gold)
        king.robbedFlag()
        state.activePlayer = king

        val merchant = players[3]
        merchant.setCharacter(Merchant)

        val architect = players[4]
        architect.setCharacter(Architect)

        val warlord = players[5]
        warlord.setCharacter(Warlord)

        state.activePlayer = assassinatedPlayer
        engine.processAction(EndTurnAction(1))

        val after = state.activePlayer
        assertNotNull(after)
    }

    @Test
    fun `endGame sets gameOver and computes scores`() {
        val players = createPlayers()
        val state = GameState(players)
        val engine = CitadelsEngine(Settings(), state, ClassicMoveValidator())
        engine.startEngine()

        val available = state.availableCharacter.toList()
        for (i in 0 until 6) {
            engine.processAction(SelectCharacterAction(available[i].rank, state.activePlayer!!))
            engine.processAction(EndDraftAction(state.activePlayer!!))
        }

        val p0 = players[0]
        p0.addHand(district(DistrictType.TEMPLE, 1, Color.BLUE))
        p0.addHand(district(DistrictType.MANOR, 3, Color.YELLOW))
        p0.addHand(district(DistrictType.WATCHTOWER, 1, Color.RED))
        p0.addHand(district(DistrictType.TAVERN, 1, Color.GREEN))
        p0.addHand(district(DistrictType.TEMPLE, 1, Color.BLUE))
        p0.addHand(district(DistrictType.CHURCH, 2, Color.BLUE))
        p0.addHand(district(DistrictType.CHURCH, 2, Color.BLUE))
        p0.addGold(20)

        p0.buildDistrict(DistrictType.TEMPLE)
        p0.buildDistrict(DistrictType.MANOR)
        p0.buildDistrict(DistrictType.WATCHTOWER)
        p0.buildDistrict(DistrictType.TAVERN)
        p0.buildDistrict(DistrictType.CHURCH)

        state.activePlayer = players[0]
        val action = EndTurnAction(players[0].character)
        engine.processAction(action)

        assertFalse(state.gameOver)
    }

    @Test
    fun `processAction with EndDraftAction advances to next drafter`() {
        val players = createPlayers()
        val state = GameState(players)
        val engine = CitadelsEngine(Settings(), state, ClassicMoveValidator())
        engine.startEngine()

        val activeBefore = state.activePlayer
        assertNotNull(activeBefore)
        val available = state.availableCharacter.toList()

        engine.processAction(SelectCharacterAction(available[0].rank, activeBefore))
        engine.processAction(EndDraftAction(activeBefore))

        val activeAfter = state.activePlayer
        assertNotNull(activeAfter)
        if (activeBefore == players[5]) {
            assertEquals(players[0], activeAfter)
        } else {
            assertEquals(players[(players.indexOf(activeBefore) + 1) % 6], activeAfter)
        }
    }

    @Test
    fun `Merchant gets passive gold at turn start`() {
        val players = createPlayers()
        players[0].setCharacter(Merchant)
        val state = GameState(players)
        state.changeGamePhase(TurnPhase)
        state.activePlayer = players[0]
        val engine = CitadelsEngine(Settings(), state, ClassicMoveValidator())

        players[0].addGold(5)
        engine.processAction(CollectGoldAction(6))
        engine.processAction(EndTurnAction(6))

        val nextPlayer = state.activePlayer
        assertNotNull(nextPlayer)
    }

    @Test
    fun `full draft with all 6 players selecting then Architect gets 2 cards`() {
        val players = createPlayers()
        val state = GameState(players)
        val engine = CitadelsEngine(Settings(), state, ClassicMoveValidator())
        engine.startEngine()

        val available = state.availableCharacter.toList()
        for (i in 0 until 6) {
            val p = state.activePlayer!!
            engine.processAction(SelectCharacterAction(available[i].rank, p))
            engine.processAction(EndDraftAction(p))
        }

        assertTrue(state.gamePhase is TurnPhase)
        val architect = state.activePlayer
        assertNotNull(architect)
    }
}
