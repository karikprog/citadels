package org.example.engine

import org.junit.jupiter.api.Test
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class GameActionTest {

    private fun createPlayers(count: Int = 6): List<Player> {
        return (1..count).map { Player(UUID.randomUUID(), "P$it") }
    }

    private fun district(type: DistrictType, cost: Int = 1, color: Color = Color.GREEN): District {
        return StandardDistrict(type, "Test-$type", cost, color)
    }

    private fun setupState(
        players: List<Player> = createPlayers(),
        activePlayer: Player? = null,
        phase: GamePhase = TurnPhase,
        kingInd: Int = 0
    ): GameState {
        val state = GameState(players)
        state.changeGamePhase(phase)
        if (activePlayer != null) state.activePlayer = activePlayer
        state.kingInd = kingInd
        return state
    }

    @Test
    fun `SelectCharacterAction selects available character`() {
        val players = createPlayers()
        val state = setupState(players, players[0], DraftPhase)
        val king = King
        state.addDraftCharacter(king)
        state.addDraftCharacter(Thief)
        val action = SelectCharacterAction(4, players[0])
        action.execute(state)
        assertEquals(4, players[0].character)
        assertEquals(1, state.availableCharacter.size)
    }

    @Test
    fun `CollectGoldAction gives 2 gold`() {
        val players = createPlayers()
        players[0].setCharacter(King)
        val state = setupState(players, players[0])
        val action = CollectGoldAction(4)
        action.execute(state)
        assertEquals(2, players[0].gold)
        assertTrue(players[0].hasTakenResources)
    }

    @Test
    fun `DrowCardAction draws 2 cards normally`() {
        val players = createPlayers()
        players[0].setCharacter(King)
        val state = setupState(players, players[0])
        repeat(5) { state.addDistrict(district(DistrictType.TAVERN)) }
        val action = DrowCardAction(4)
        action.execute(state)
        assertEquals(2, players[0].temporaryHand.size)
        assertTrue(players[0].hasTakenResources)
    }

    @Test
    fun `DrowCardAction draws 3 cards with Observatory`() {
        val players = createPlayers()
        players[0].setCharacter(King)
        val obs = SpecialDistrict(
            DistrictType.OBSERVATORY, "Observatory", 5, Color.LILAC, "draw 3"
        )
        players[0].addHand(obs)
        players[0].addGold(10)
        players[0].buildDistrict(DistrictType.OBSERVATORY)
        val state = setupState(players, players[0])
        repeat(10) { state.addDistrict(district(DistrictType.TAVERN)) }
        val action = DrowCardAction(4)
        action.execute(state)
        assertEquals(3, players[0].temporaryHand.size)
    }

    @Test
    fun `CollectCardAction keeps selected card`() {
        val players = createPlayers()
        players[0].setCharacter(King)
        val state = setupState(players, players[0])
        val d1 = district(DistrictType.TAVERN)
        val d2 = district(DistrictType.MARKET)
        players[0].temporaryHand.add(d1)
        players[0].temporaryHand.add(d2)
        val action = CollectCardAction(4, 1)
        action.execute(state)
        assertEquals(1, players[0].hand.size)
        assertEquals(DistrictType.MARKET, players[0].hand[0].type)
        assertEquals(0, players[0].temporaryHand.size)
    }

    @Test
    fun `CollectCardAction with Library keeps all cards`() {
        val players = createPlayers()
        players[0].setCharacter(King)
        val lib = SpecialDistrict(
            DistrictType.LIBRARY, "Library", 6, Color.LILAC, "keep all"
        )
        players[0].addHand(lib)
        players[0].addGold(10)
        players[0].buildDistrict(DistrictType.LIBRARY)
        val state = setupState(players, players[0])
        val d1 = district(DistrictType.TAVERN)
        val d2 = district(DistrictType.MARKET)
        players[0].temporaryHand.add(d1)
        players[0].temporaryHand.add(d2)
        val action = CollectCardAction(4, 0)
        action.execute(state)
        assertEquals(2, players[0].hand.size)
    }

    @Test
    fun `UseAssassinAction assassinates target player`() {
        val players = createPlayers()
        players[0].setCharacter(Assassin)
        players[1].setCharacter(King)
        val state = setupState(players, players[0])
        val action = UseAssassinAction(1, 4)
        action.execute(state)
        assertTrue(players[1].isAssassinated)
    }

    @Test
    fun `UseAssassinAction no-op when victim rank not owned`() {
        val players = createPlayers()
        players[0].setCharacter(Assassin)
        val state = setupState(players, players[0])
        val action = UseAssassinAction(1, 7)
        action.execute(state)
    }

    @Test
    fun `BuildDistrictAction builds via player`() {
        val players = createPlayers()
        players[0].setCharacter(King)
        players[0].addHand(district(DistrictType.TAVERN))
        players[0].addGold(5)
        players[0].takeResourcesFlag()
        val state = setupState(players, players[0])
        val action = BuildDistrictAction(4, DistrictType.TAVERN)
        action.execute(state)
        assertEquals(1, players[0].city.size)
    }

    @Test
    fun `UseWarlordAction destroys opponent district`() {
        val players = createPlayers()
        players[0].setCharacter(Warlord)
        players[1].setCharacter(King)
        val target = district(DistrictType.TAVERN)
        players[1].addHand(target)
        players[1].addGold(10)
        players[1].buildDistrict(DistrictType.TAVERN)
        players[0].addGold(10)
        players[0].takeResourcesFlag()
        val state = setupState(players, players[0])
        val card = players[1].city.find { it.type == DistrictType.TAVERN }!!
        val action = UseWarlordAction(8, 4, card)
        action.execute(state)
        assertEquals(0, players[1].city.size)
        assertEquals(1, players[1].destroyedDistricts.size)
    }

    @Test
    fun `UseWarlordAction respects Great Wall cost increase`() {
        val players = createPlayers()
        players[0].setCharacter(Warlord)
        players[1].setCharacter(King)
        val wall = SpecialDistrict(
            DistrictType.GREAT_WALL, "Great Wall", 6, Color.LILAC, "cost +1"
        )
        players[1].addHand(district(DistrictType.TAVERN))
        players[1].addHand(wall)
        players[1].addGold(10)
        players[1].buildDistrict(DistrictType.GREAT_WALL)
        players[1].buildDistrict(DistrictType.TAVERN)
        players[0].addGold(10)
        players[0].takeResourcesFlag()
        val state = setupState(players, players[0])
        val card = players[1].city.find { it.type == DistrictType.TAVERN }!!
        val action = UseWarlordAction(8, 4, card)
        action.execute(state)
        assertEquals(9, players[0].gold)
    }

    @Test
    fun `UseWarlordAction costs cost-1 without Great Wall`() {
        val players = createPlayers()
        players[0].setCharacter(Warlord)
        players[1].setCharacter(King)
        players[1].addHand(district(DistrictType.TAVERN, 2))
        players[1].addGold(10)
        players[1].buildDistrict(DistrictType.TAVERN)
        players[0].addGold(10)
        players[0].takeResourcesFlag()
        val state = setupState(players, players[0])
        val card = players[1].city.find { it.type == DistrictType.TAVERN }!!
        val action = UseWarlordAction(8, 4, card)
        action.execute(state)
        assertEquals(9, players[0].gold)
    }

    @Test
    fun `UseArchitectBuildAction builds up to 3 districts`() {
        val players = createPlayers()
        players[0].setCharacter(Architect)
        players[0].addHand(district(DistrictType.TAVERN))
        players[0].addHand(district(DistrictType.MARKET))
        players[0].addHand(district(DistrictType.TRADING_POST, 2))
        players[0].addGold(10)
        players[0].takeResourcesFlag()
        val state = setupState(players, players[0])
        val cards = players[0].hand.toList()
        val action = UseArchitectBuildAction(7, cards)
        action.execute(state)
        assertEquals(3, players[0].city.size)
    }

    @Test
    fun `UseSmithyCardAction pays 2 gold and draws 3 cards`() {
        val players = createPlayers()
        players[0].setCharacter(King)
        val smithy = SpecialDistrict(
            DistrictType.SMITHY, "Smithy", 5, Color.LILAC, "draw 3"
        )
        players[0].addHand(smithy)
        players[0].addGold(10)
        players[0].buildDistrict(DistrictType.SMITHY)
        val state = setupState(players, players[0])
        repeat(10) { state.addDistrict(district(DistrictType.TAVERN)) }
        val action = UseSmithyCardAction(4)
        action.execute(state)
        assertEquals(3, players[0].gold)
        assertEquals(3, players[0].hand.size)
    }

    @Test
    fun `UseLaboratoryCardAction discards card and gains 1 gold`() {
        val players = createPlayers()
        players[0].setCharacter(King)
        val lab = SpecialDistrict(
            DistrictType.LABORATORY, "Lab", 5, Color.LILAC, "discard->gold"
        )
        players[0].addHand(lab)
        players[0].addGold(10)
        players[0].buildDistrict(DistrictType.LABORATORY)
        val d = district(DistrictType.TAVERN)
        players[0].addHand(d)
        val state = setupState(players, players[0])
        val action = UseLaboratoryCardAction(4, d)
        action.execute(state)
        assertEquals(0, players[0].hand.size)
        assertEquals(6, players[0].gold)
    }

    @Test
    fun `UseSwapOtherPlayerMagicianAction swaps hands`() {
        val players = createPlayers()
        players[0].setCharacter(Magician)
        players[1].setCharacter(King)
        players[0].addHand(district(DistrictType.TAVERN))
        players[1].addHand(district(DistrictType.MARKET))
        val state = setupState(players, players[0])
        val action = UseSwapOtherPlayerMagicianAction(3, "P2")
        action.execute(state)
        assertEquals(DistrictType.MARKET, players[0].hand[0].type)
        assertEquals(DistrictType.TAVERN, players[1].hand[0].type)
    }

    @Test
    fun `UseSwapDeckMagicianAction swaps cards with deck`() {
        val players = createPlayers()
        players[0].setCharacter(Magician)
        val d1 = district(DistrictType.TAVERN)
        players[0].addHand(d1)
        val state = setupState(players, players[0])
        state.addDistrict(district(DistrictType.MARKET))
        val action = UseSwapDeckMagicianAction(3, listOf(d1))
        action.execute(state)
        assertEquals(1, players[0].hand.size)
        assertEquals(DistrictType.MARKET, players[0].hand[0].type)
    }

    @Test
    fun `EndTurnAction resets flags`() {
        val players = createPlayers()
        players[0].setCharacter(King)
        players[0].takeResourcesFlag()
        val state = setupState(players, players[0])
        val action = EndTurnAction(4)
        action.execute(state)
        assertFalse(players[0].hasTakenResources)
    }

    @Test
    fun `EndDraftAction succeeds for active player`() {
        val players = createPlayers()
        val state = setupState(players, players[0], DraftPhase)
        val action = EndDraftAction(players[0])
        action.execute(state)
    }

}
