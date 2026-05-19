package org.example.engine

import org.junit.jupiter.api.Test
import java.util.UUID
import kotlin.test.assertTrue
import kotlin.test.assertFalse

class ClassicMoveValidatorTest {

    private val validator = ClassicMoveValidator()

    private fun createPlayers(count: Int = 6): List<Player> {
        return (1..count).map { Player(UUID.randomUUID(), "P$it") }
    }

    private fun district(type: DistrictType, cost: Int = 1, color: Color = Color.GREEN): District {
        return StandardDistrict(type, "Test-$type", cost, color)
    }

    private fun createState(
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

    private fun isValid(action: GameAction, state: GameState): Boolean {
        return validator.canExecute(action, state) is ValidationResult.Valid
    }

    private fun isInvalid(action: GameAction, state: GameState): Boolean {
        return validator.canExecute(action, state) is ValidationResult.Invalid
    }

    @Test
    fun `SelectCharacterAction valid during DraftPhase`() {
        val players = createPlayers()
        val state = createState(players, players[0], DraftPhase)
        state.addDraftCharacter(King)
        assertTrue(isValid(SelectCharacterAction(4, players[0]), state))
    }

    @Test
    fun `SelectCharacterAction invalid during TurnPhase`() {
        val players = createPlayers()
        players[0].setCharacter(King)
        val state = createState(players, players[0])
        assertFalse(isValid(SelectCharacterAction(4, players[0]), state))
    }

    @Test
    fun `non-draft action invalid during DraftPhase`() {
        val players = createPlayers()
        players[0].setCharacter(King)
        val state = createState(players, players[0], DraftPhase)
        assertFalse(isValid(CollectGoldAction(4), state))
    }

    @Test
    fun `assassinated player cannot act`() {
        val players = createPlayers()
        players[0].setCharacter(King)
        players[0].assassinated()
        val state = createState(players, players[0])
        assertFalse(isValid(CollectGoldAction(4), state))
    }

    @Test
    fun `rank mismatch returns invalid`() {
        val players = createPlayers()
        players[0].setCharacter(Architect)
        val state = createState(players, players[0])
        assertFalse(isValid(CollectGoldAction(4), state))
    }

    @Test
    fun `no active player returns invalid`() {
        val players = createPlayers()
        val state = createState(players, null)
        assertFalse(isValid(CollectGoldAction(4), state))
    }

    @Test
    fun `canSelectCharacter valid when character available`() {
        val players = createPlayers()
        val state = createState(players, players[0], DraftPhase)
        state.addDraftCharacter(King)
        assertTrue(isValid(SelectCharacterAction(4, players[0]), state))
    }

    @Test
    fun `canSelectCharacter invalid when character not available`() {
        val players = createPlayers()
        val state = createState(players, players[0], DraftPhase)
        assertFalse(isValid(SelectCharacterAction(4, players[0]), state))
    }

    @Test
    fun `canSelectCharacter invalid when already selected`() {
        val players = createPlayers()
        players[0].setCharacter(King)
        val state = createState(players, players[0], DraftPhase)
        state.addDraftCharacter(Thief)
        assertFalse(isValid(SelectCharacterAction(2, players[0]), state))
    }

    @Test
    fun `canCollectResources valid when not taken`() {
        val players = createPlayers()
        players[0].setCharacter(King)
        val state = createState(players, players[0])
        assertTrue(isValid(CollectGoldAction(4), state))
    }

    @Test
    fun `canCollectResources invalid when already taken`() {
        val players = createPlayers()
        players[0].setCharacter(King)
        players[0].takeResourcesFlag()
        val state = createState(players, players[0])
        assertFalse(isValid(CollectGoldAction(4), state))
    }

    @Test
    fun `canKeepCard valid with cards in temp hand`() {
        val players = createPlayers()
        players[0].setCharacter(King)
        players[0].temporaryHand.add(district(DistrictType.TAVERN))
        players[0].temporaryHand.add(district(DistrictType.MARKET))
        val state = createState(players, players[0])
        assertTrue(isValid(CollectCardAction(4, 0), state))
        assertTrue(isValid(CollectCardAction(4, 1), state))
    }

    @Test
    fun `canKeepCard invalid with empty temp hand`() {
        val players = createPlayers()
        players[0].setCharacter(King)
        val state = createState(players, players[0])
        assertFalse(isValid(CollectCardAction(4, 0), state))
    }

    @Test
    fun `canKeepCard invalid with bad index`() {
        val players = createPlayers()
        players[0].setCharacter(King)
        players[0].temporaryHand.add(district(DistrictType.TAVERN))
        val state = createState(players, players[0])
        assertFalse(isValid(CollectCardAction(4, 5), state))
    }

    @Test
    fun `canBuildDistrict valid when all conditions met`() {
        val players = createPlayers()
        players[0].setCharacter(King)
        players[0].addHand(district(DistrictType.TAVERN))
        players[0].addGold(5)
        players[0].takeResourcesFlag()
        val state = createState(players, players[0])
        assertTrue(isValid(BuildDistrictAction(4, DistrictType.TAVERN), state))
    }

    @Test
    fun `canBuildDistrict invalid without taking resources`() {
        val players = createPlayers()
        players[0].setCharacter(King)
        players[0].addHand(district(DistrictType.TAVERN))
        players[0].addGold(5)
        val state = createState(players, players[0])
        assertFalse(isValid(BuildDistrictAction(4, DistrictType.TAVERN), state))
    }

    @Test
    fun `canBuildDistrict invalid when already built (non-Architect)`() {
        val players = createPlayers()
        players[0].setCharacter(King)
        players[0].addHand(district(DistrictType.TAVERN))
        players[0].addHand(district(DistrictType.MARKET))
        players[0].addGold(5)
        players[0].takeResourcesFlag()
        players[0].buildDistrict(DistrictType.TAVERN)
        val state = createState(players, players[0])
        assertFalse(isValid(BuildDistrictAction(4, DistrictType.MARKET), state))
    }

    @Test
    fun `canBuildDistrict valid for Architect even after building`() {
        val players = createPlayers()
        players[0].setCharacter(Architect)
        players[0].addHand(district(DistrictType.TAVERN))
        players[0].addHand(district(DistrictType.MARKET))
        players[0].addGold(5)
        players[0].takeResourcesFlag()
        players[0].buildDistrict(DistrictType.TAVERN)
        val state = createState(players, players[0])
        assertTrue(isValid(BuildDistrictAction(7, DistrictType.MARKET), state))
    }

    @Test
    fun `canBuildDistrict invalid without card in hand`() {
        val players = createPlayers()
        players[0].setCharacter(King)
        players[0].addGold(5)
        players[0].takeResourcesFlag()
        val state = createState(players, players[0])
        assertFalse(isValid(BuildDistrictAction(4, DistrictType.TAVERN), state))
    }

    @Test
    fun `canBuildDistrict invalid with duplicate in city`() {
        val players = createPlayers()
        players[0].setCharacter(King)
        val d = district(DistrictType.TAVERN)
        val d2 = district(DistrictType.MARKET)
        players[0].addHand(d)
        players[0].addHand(d2)
        players[0].addGold(5)
        players[0].buildDistrict(DistrictType.TAVERN)
        players[0].takeResourcesFlag()
        val state = createState(players, players[0])
        assertFalse(isValid(BuildDistrictAction(4, DistrictType.TAVERN), state))
    }

    @Test
    fun `canBuildDistrict invalid without enough gold`() {
        val players = createPlayers()
        players[0].setCharacter(King)
        players[0].addHand(district(DistrictType.PALACE, 5, Color.YELLOW))
        players[0].addGold(2)
        players[0].takeResourcesFlag()
        val state = createState(players, players[0])
        assertFalse(isValid(BuildDistrictAction(4, DistrictType.PALACE), state))
    }

    @Test
    fun `canAssassinate valid for ranks 2-8`() {
        val players = createPlayers()
        players[0].setCharacter(Assassin)
        val state = createState(players, players[0])
        for (rank in 2..8) {
            assertTrue(isValid(UseAssassinAction(1, rank), state), "should allow rank $rank")
        }
    }

    @Test
    fun `canAssassinate invalid for rank 1`() {
        val players = createPlayers()
        players[0].setCharacter(Assassin)
        val state = createState(players, players[0])
        assertFalse(isValid(UseAssassinAction(1, 1), state))
    }

    @Test
    fun `canAssassinate invalid for rank outside 1-8`() {
        val players = createPlayers()
        players[0].setCharacter(Assassin)
        val state = createState(players, players[0])
        assertFalse(isValid(UseAssassinAction(1, 0), state))
        assertFalse(isValid(UseAssassinAction(1, 9), state))
    }

    @Test
    fun `canStealFrom valid for non-assassin non-thief non-assassinated target`() {
        val players = createPlayers()
        players[0].setCharacter(Thief)
        players[1].setCharacter(King)
        players[2].setCharacter(Assassin)
        players[2].assassinated()
        val state = createState(players, players[0])
        assertTrue(isValid(UseThiefAction(2, 4), state))
    }

    @Test
    fun `canStealFrom invalid for rank 1`() {
        val players = createPlayers()
        players[0].setCharacter(Thief)
        players[1].setCharacter(Assassin)
        players[2].assassinated()
        val state = createState(players, players[0])
        assertFalse(isValid(UseThiefAction(2, 1), state))
    }

    @Test
    fun `canStealFrom invalid for rank 2`() {
        val players = createPlayers()
        players[0].setCharacter(Thief)
        players[2].assassinated()
        val state = createState(players, players[0])
        assertFalse(isValid(UseThiefAction(2, 2), state))
    }

    @Test
    fun `canStealFrom invalid for assassinated victim`() {
        val players = createPlayers()
        players[0].setCharacter(Thief)
        players[1].setCharacter(King)
        players[1].assassinated()
        players[2].assassinated()
        val state = createState(players, players[0])
        assertFalse(isValid(UseThiefAction(2, 4), state))
    }

    @Test
    fun `canDestroyDistrict valid for non-Bishop`() {
        val players = createPlayers()
        players[0].setCharacter(Warlord)
        players[1].setCharacter(King)
        players[1].addHand(district(DistrictType.TAVERN))
        players[1].addGold(10)
        players[1].buildDistrict(DistrictType.TAVERN)
        players[0].addGold(10)
        players[0].takeResourcesFlag()
        val state = createState(players, players[0])
        val card = players[1].city[0]
        assertTrue(isValid(UseWarlordAction(8, 4, card), state))
    }

    @Test
    fun `canDestroyDistrict invalid for non-assassinated Bishop`() {
        val players = createPlayers()
        players[0].setCharacter(Warlord)
        players[1].setCharacter(Bishop)
        players[1].addHand(district(DistrictType.TEMPLE, 1, Color.BLUE))
        players[1].addGold(10)
        players[1].buildDistrict(DistrictType.TEMPLE)
        players[0].addGold(10)
        players[0].takeResourcesFlag()
        val state = createState(players, players[0])
        val card = players[1].city[0]
        assertFalse(isValid(UseWarlordAction(8, 5, card), state))
    }

    @Test
    fun `canDestroyDistrict valid for assassinated Bishop`() {
        val players = createPlayers()
        players[0].setCharacter(Warlord)
        players[1].setCharacter(Bishop)
        players[1].assassinated()
        players[1].addHand(district(DistrictType.TEMPLE, 1, Color.BLUE))
        players[1].addGold(10)
        players[1].buildDistrict(DistrictType.TEMPLE)
        players[0].addGold(10)
        players[0].takeResourcesFlag()
        val state = createState(players, players[0])
        val card = players[1].city[0]
        assertTrue(isValid(UseWarlordAction(8, 5, card), state))
    }

    @Test
    fun `canDestroyDistrict invalid for completed city (7+)`() {
        val players = createPlayers()
        players[0].setCharacter(Warlord)
        players[1].setCharacter(King)
        players[1].addGold(10)
        val types = listOf(
            DistrictType.TEMPLE, DistrictType.CHURCH, DistrictType.MONASTERY,
            DistrictType.CATHEDRAL, DistrictType.MANOR, DistrictType.CASTLE,
            DistrictType.PALACE
        )
        for (t in types) {
            players[1].addHand(district(t, 1, Color.GREEN))
            players[1].buildDistrict(t)
        }
        players[0].addGold(10)
        players[0].takeResourcesFlag()
        val state = createState(players, players[0])
        val card = players[1].city[0]
        assertFalse(isValid(UseWarlordAction(8, 4, card), state))
    }

    @Test
    fun `canDestroyDistrict invalid for Keep`() {
        val players = createPlayers()
        players[0].setCharacter(Warlord)
        players[1].setCharacter(King)
        val keep = SpecialDistrict(
            DistrictType.KEEP, "Keep", 3, Color.LILAC, "indestructible"
        )
        players[1].addHand(keep)
        players[1].addGold(10)
        players[1].buildDistrict(DistrictType.KEEP)
        players[0].addGold(10)
        players[0].takeResourcesFlag()
        val state = createState(players, players[0])
        val card = players[1].city[0]
        assertFalse(isValid(UseWarlordAction(8, 4, card), state))
    }

    @Test
    fun `canDestroyDistrict invalid when card not in victims city`() {
        val players = createPlayers()
        players[0].setCharacter(Warlord)
        players[1].setCharacter(King)
        players[0].addGold(10)
        players[0].takeResourcesFlag()
        val state = createState(players, players[0])
        val card = district(DistrictType.TAVERN)
        assertFalse(isValid(UseWarlordAction(8, 4, card), state))
    }

    @Test
    fun `canDestroyDistrict invalid with not enough gold`() {
        val players = createPlayers()
        players[0].setCharacter(Warlord)
        players[1].setCharacter(King)
        players[1].addHand(district(DistrictType.PALACE, 5, Color.YELLOW))
        players[1].addGold(10)
        players[1].buildDistrict(DistrictType.PALACE)
        players[0].addGold(2)
        players[0].takeResourcesFlag()
        val state = createState(players, players[0])
        val card = players[1].city[0]
        assertFalse(isValid(UseWarlordAction(8, 4, card), state))
    }

    @Test
    fun `canBuildMultiple valid for up to 3 districts`() {
        val players = createPlayers()
        players[0].setCharacter(Architect)
        players[0].addHand(district(DistrictType.TAVERN))
        players[0].addHand(district(DistrictType.MARKET))
        players[0].addHand(district(DistrictType.TRADING_POST, 2))
        players[0].addGold(10)
        players[0].takeResourcesFlag()
        val state = createState(players, players[0])
        assertTrue(isValid(UseArchitectBuildAction(7, players[0].hand.toList()), state))
    }

    @Test
    fun `canBuildMultiple invalid with too many cards`() {
        val players = createPlayers()
        players[0].setCharacter(Architect)
        val manyCards = listOf(
            district(DistrictType.TAVERN),
            district(DistrictType.MARKET),
            district(DistrictType.TRADING_POST, 2),
            district(DistrictType.DOCKS, 3),
        )
        val state = createState(players, players[0])
        assertFalse(isValid(UseArchitectBuildAction(7, manyCards), state))
    }

    @Test
    fun `canUseSmithy valid with Smithy and enough gold`() {
        val players = createPlayers()
        players[0].setCharacter(King)
        val smithy = SpecialDistrict(
            DistrictType.SMITHY, "Smithy", 5, Color.LILAC, "draw"
        )
        players[0].addHand(smithy)
        players[0].addGold(10)
        players[0].buildDistrict(DistrictType.SMITHY)
        val state = createState(players, players[0])
        assertTrue(isValid(UseSmithyCardAction(4), state))
    }

    @Test
    fun `canUseSmithy invalid without Smithy`() {
        val players = createPlayers()
        players[0].setCharacter(King)
        players[0].addGold(10)
        val state = createState(players, players[0])
        assertFalse(isValid(UseSmithyCardAction(4), state))
    }

    @Test
    fun `canUseSmithy invalid with not enough gold`() {
        val players = createPlayers()
        players[0].setCharacter(King)
        val smithy = SpecialDistrict(
            DistrictType.SMITHY, "Smithy", 5, Color.LILAC, "draw"
        )
        players[0].addHand(smithy)
        players[0].addGold(5)
        players[0].buildDistrict(DistrictType.SMITHY)
        val state = createState(players, players[0])
        assertFalse(isValid(UseSmithyCardAction(4), state))
    }

    @Test
    fun `canUseLaboratory valid with Lab and card in hand`() {
        val players = createPlayers()
        players[0].setCharacter(King)
        val lab = SpecialDistrict(
            DistrictType.LABORATORY, "Lab", 5, Color.LILAC, "convert"
        )
        players[0].addHand(lab)
        players[0].addGold(10)
        players[0].buildDistrict(DistrictType.LABORATORY)
        val card = district(DistrictType.TAVERN)
        players[0].addHand(card)
        val state = createState(players, players[0])
        assertTrue(isValid(UseLaboratoryCardAction(4, card), state))
    }

    @Test
    fun `canUseLaboratory invalid without Lab`() {
        val players = createPlayers()
        players[0].setCharacter(King)
        val card = district(DistrictType.TAVERN)
        players[0].addHand(card)
        val state = createState(players, players[0])
        assertFalse(isValid(UseLaboratoryCardAction(4, card), state))
    }

    @Test
    fun `canUseLaboratory invalid without card in hand`() {
        val players = createPlayers()
        players[0].setCharacter(King)
        val lab = SpecialDistrict(
            DistrictType.LABORATORY, "Lab", 5, Color.LILAC, "convert"
        )
        players[0].addHand(lab)
        players[0].addGold(10)
        players[0].buildDistrict(DistrictType.LABORATORY)
        val state = createState(players, players[0])
        assertFalse(isValid(UseLaboratoryCardAction(4, district(DistrictType.TAVERN)), state))
    }

    @Test
    fun `canSwapHandsWithPlayer valid for another player`() {
        val players = createPlayers()
        players[0].setCharacter(Magician)
        val state = createState(players, players[0])
        assertTrue(isValid(UseSwapOtherPlayerMagicianAction(3, "P2"), state))
    }

    @Test
    fun `canSwapHandsWithPlayer invalid for self`() {
        val players = createPlayers()
        players[0].setCharacter(Magician)
        val state = createState(players, players[0])
        assertFalse(isValid(UseSwapOtherPlayerMagicianAction(3, "P1"), state))
    }

    @Test
    fun `canSwapHandsWithPlayer invalid for non-existent player`() {
        val players = createPlayers()
        players[0].setCharacter(Magician)
        val state = createState(players, players[0])
        assertFalse(isValid(UseSwapOtherPlayerMagicianAction(3, "Nobody"), state))
    }

    @Test
    fun `canSwapCardsWithDeck valid with cards in hand`() {
        val players = createPlayers()
        players[0].setCharacter(Magician)
        val card = district(DistrictType.TAVERN)
        players[0].addHand(card)
        val state = createState(players, players[0])
        assertTrue(isValid(UseSwapDeckMagicianAction(3, listOf(card)), state))
    }

    @Test
    fun `canSwapCardsWithDeck invalid with empty list`() {
        val players = createPlayers()
        players[0].setCharacter(Magician)
        val state = createState(players, players[0])
        assertFalse(isValid(UseSwapDeckMagicianAction(3, emptyList()), state))
    }

    @Test
    fun `canSwapCardsWithDeck invalid with card not in hand`() {
        val players = createPlayers()
        players[0].setCharacter(Magician)
        val state = createState(players, players[0])
        assertFalse(isValid(UseSwapDeckMagicianAction(3, listOf(district(DistrictType.TAVERN))), state))
    }

    @Test
    fun `canEndDraftTurn valid when character selected`() {
        val players = createPlayers()
        players[0].setCharacter(King)
        val state = createState(players, players[0], DraftPhase)
        assertTrue(isValid(EndDraftAction(players[0]), state))
    }

    @Test
    fun `canEndDraftTurn invalid when no character selected`() {
        val players = createPlayers()
        val state = createState(players, players[0], DraftPhase)
        assertFalse(isValid(EndDraftAction(players[0]), state))
    }

    @Test
    fun `canEndTurn valid when resources taken`() {
        val players = createPlayers()
        players[0].setCharacter(King)
        players[0].takeResourcesFlag()
        val state = createState(players, players[0])
        assertTrue(isValid(EndTurnAction(4), state))
    }

    @Test
    fun `canEndTurn invalid when resources not taken`() {
        val players = createPlayers()
        players[0].setCharacter(King)
        val state = createState(players, players[0])
        assertFalse(isValid(EndTurnAction(4), state))
    }

    @Test
    fun `canTakePassiveGold valid when income not collected`() {
        val players = createPlayers()
        players[0].setCharacter(King)
        val state = createState(players, players[0])
        assertTrue(isValid(PassiveTakeGoldAction(4), state))
    }

    @Test
    fun `canTakePassiveGold invalid when income already collected`() {
        val players = createPlayers()
        players[0].setCharacter(King)
        players[0].passiveTakeGold()
        val state = createState(players, players[0])
        assertFalse(isValid(PassiveTakeGoldAction(4), state))
    }
}
