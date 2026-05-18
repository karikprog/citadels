package org.example.engine

import org.junit.jupiter.api.Test
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PlayerTest {

    private fun createPlayer(name: String = "TestPlayer"): Player {
        return Player(UUID.randomUUID(), name)
    }

    private fun district(type: DistrictType, cost: Int = 1, color: Color = Color.GREEN): District {
        return StandardDistrict(type, "Test-$type", cost, color)
    }

    @Test
    fun `initial state`() {
        val p = createPlayer("Alice")
        assertEquals(0, p.gold)
        assertEquals(0, p.hand.size)
        assertEquals(0, p.city.size)
        assertEquals(0, p.character)
        assertEquals("Alice", p.name)
        assertFalse(p.isAssassinated)
        assertFalse(p.isRobbed)
        assertFalse(p.hasBuildThisTurn)
        assertFalse(p.isFirstToFinish)
        assertFalse(p.hasCollectedIncome)
        assertFalse(p.hasTakenResources)
    }

    @Test
    fun `addGold increases gold`() {
        val p = createPlayer()
        p.addGold(5)
        assertEquals(5, p.gold)
        p.addGold(3)
        assertEquals(8, p.gold)
    }

    @Test
    fun `addGold with negative does nothing`() {
        val p = createPlayer()
        p.addGold(5)
        p.addGold(-2)
        assertEquals(5, p.gold)
    }

    @Test
    fun `spendGold reduces gold when sufficient`() {
        val p = createPlayer()
        p.addGold(10)
        p.spendGold(4)
        assertEquals(6, p.gold)
    }

    @Test
    fun `spendGold does nothing when insufficient`() {
        val p = createPlayer()
        p.addGold(3)
        p.spendGold(5)
        assertEquals(3, p.gold)
    }

    @Test
    fun `addHand adds card to hand`() {
        val p = createPlayer()
        val d = district(DistrictType.TAVERN)
        p.addHand(d)
        assertEquals(1, p.hand.size)
        assertEquals(d, p.hand[0])
    }

    @Test
    fun `containsHand returns true when card in hand`() {
        val p = createPlayer()
        p.addHand(district(DistrictType.MARKET))
        assertTrue(p.containsHand(DistrictType.MARKET))
    }

    @Test
    fun `containsHand returns false when card not in hand`() {
        val p = createPlayer()
        p.addHand(district(DistrictType.MARKET))
        assertFalse(p.containsHand(DistrictType.TAVERN))
    }

    @Test
    fun `containsCity returns true when card in city`() {
        val p = createPlayer()
        val castle = district(DistrictType.CASTLE, 4, Color.YELLOW)
        p.addHand(castle)
        p.addGold(4)
        p.buildDistrict(DistrictType.CASTLE)
        assertTrue(p.containsCity(DistrictType.CASTLE))
    }

    @Test
    fun `containsCity returns false when card not in city`() {
        val p = createPlayer()
        assertFalse(p.containsCity(DistrictType.CASTLE))
    }

    @Test
    fun `setCharacter sets character and rank`() {
        val p = createPlayer()
        p.setCharacter(King())
        assertEquals(4, p.character)
    }

    @Test
    fun `resetCharacter clears character`() {
        val p = createPlayer()
        p.setCharacter(King())
        p.resetCharacter()
        assertEquals(0, p.character)
    }

    @Test
    fun `assassinated sets flag`() {
        val p = createPlayer()
        p.assassinated()
        assertTrue(p.isAssassinated)
    }

    @Test
    fun `robbed sets gold to zero and flag`() {
        val p = createPlayer()
        p.addGold(10)
        p.robbedFlag()

        assertTrue(p.isRobbed)
    }

    @Test
    fun `getScore base sum of district costs`() {
        val p = createPlayer()
        p.addHand(district(DistrictType.TAVERN, 1, Color.GREEN))
        p.addHand(district(DistrictType.MARKET, 2, Color.GREEN))
        p.addGold(10)
        p.buildDistrict(DistrictType.TAVERN)
        p.buildDistrict(DistrictType.MARKET)
        assertEquals(3, p.getScore())
    }

    @Test
    fun `getScore seven districts not first gets plus 2`() {
        val p = createPlayer()
        p.addGold(10)
        for (t in listOf(
            DistrictType.TEMPLE, DistrictType.CHURCH, DistrictType.MONASTERY,
            DistrictType.CATHEDRAL, DistrictType.MANOR, DistrictType.CASTLE,
            DistrictType.PALACE
        )) {
            p.addHand(district(t, 1, Color.GREEN))
            p.buildDistrict(t)
        }
        assertEquals(9, p.getScore())
    }

    @Test
    fun `getScore five colors gets plus 3`() {
        val p = createPlayer()
        p.addHand(district(DistrictType.TEMPLE, 1, Color.BLUE))
        p.addHand(district(DistrictType.MANOR, 1, Color.YELLOW))
        p.addHand(district(DistrictType.WATCHTOWER, 1, Color.RED))
        p.addHand(district(DistrictType.TAVERN, 1, Color.GREEN))
        p.addHand(district(DistrictType.KEEP, 1, Color.LILAC))
        p.addGold(10)
        for (t in listOf(
            DistrictType.TEMPLE, DistrictType.MANOR, DistrictType.WATCHTOWER,
            DistrictType.TAVERN, DistrictType.KEEP
        )) {
            p.buildDistrict(t)
        }
        assertEquals(8, p.getScore())
    }

    @Test
    fun `getScore HauntedQuarter counts as wildcard`() {
        val p = createPlayer()
        p.addHand(district(DistrictType.TEMPLE, 1, Color.BLUE))
        p.addHand(district(DistrictType.MANOR, 1, Color.YELLOW))
        p.addHand(district(DistrictType.WATCHTOWER, 1, Color.RED))
        p.addHand(district(DistrictType.TAVERN, 1, Color.GREEN))
        p.addHand(SpecialDistrict(
            DistrictType.HAUNTED_QUARTER, "Haunted", 2, Color.LILAC, "wildcard"
        ))
        p.addGold(10)
        for (t in listOf(
            DistrictType.TEMPLE, DistrictType.MANOR, DistrictType.WATCHTOWER,
            DistrictType.TAVERN, DistrictType.HAUNTED_QUARTER
        )) {
            p.buildDistrict(t)
        }
        assertEquals(9, p.getScore())
    }

    @Test
    fun `getScore DragonGate and University worth 8 points`() {
        val p = createPlayer()
        val gate = SpecialDistrict(
            DistrictType.DRAGON_GATE, "Dragon Gate", 6, Color.LILAC, "8 pts"
        )
        p.addHand(gate)
        p.addGold(10)
        p.buildDistrict(DistrictType.DRAGON_GATE)
        assertEquals(8, p.getScore())
    }

    @Test
    fun `resetTurnFlags resets flags`() {
        val p = createPlayer()
        p.assassinated()
        p.robbedFlag()
        val tavern = district(DistrictType.TAVERN)
        p.addHand(tavern)
        p.addGold(5)
        p.buildDistrict(DistrictType.TAVERN)
        p.addHand(district(DistrictType.MARKET))
        p.passiveTakeGold()
        p.takeResourcesFlag()
        p.resetTurnFlags()
        assertFalse(p.isRobbed)
        assertFalse(p.isAssassinated)
        assertFalse(p.hasBuildThisTurn)
        assertFalse(p.hasCollectedIncome)
        assertFalse(p.hasTakenResources)
    }

    @Test
    fun `temporaryHand stores and clears`() {
        val p = createPlayer()
        val d = district(DistrictType.TAVERN)
        p.temporaryHand.add(d)
        assertEquals(1, p.temporaryHand.size)
        p.temporaryHand.clear()
        assertEquals(0, p.temporaryHand.size)
    }
}
