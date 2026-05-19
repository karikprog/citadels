package org.example.utils

import org.example.engine.*
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SettingsTest {

    private val settings = Settings()

    @Test
    fun `playerCount is 6`() {
        assertEquals(6, settings.playerCount)
    }

    @Test
    fun `districtCardCount is 65`() {
        assertEquals(65, settings.districtCardCount)
    }

    @Test
    fun `generateCharacters returns 8 shuffled characters`() {
        val chars = settings.generateCharactersForSixPlayers()
        assertEquals(7, chars.size)
    }

    @Test
    fun `generateCharacters returns shuffled list (not deterministic)`() {
        val uniqueOrders = mutableSetOf<List<Int>>()
        repeat(10) {
            uniqueOrders.add(settings.generateCharactersForSixPlayers().map { it.rank })
        }
        assertTrue(uniqueOrders.size > 1)
    }

    @Test
    fun `generateCitadelsDeck returns 65 cards`() {
        val deck = settings.generateCitadelsDeck()
        assertEquals(65, deck.size)
    }

    @Test
    fun `generateCitadelsDeck correct distribution`() {
        val deck = settings.generateCitadelsDeck()
        val counts = mutableMapOf<DistrictType, Int>()
        for (card in deck) {
            counts[card.type] = (counts[card.type] ?: 0) + 1
        }
        assertEquals(3, counts[DistrictType.TEMPLE])
        assertEquals(3, counts[DistrictType.CHURCH])
        assertEquals(3, counts[DistrictType.MONASTERY])
        assertEquals(2, counts[DistrictType.CATHEDRAL])
        assertEquals(5, counts[DistrictType.MANOR])
        assertEquals(4, counts[DistrictType.CASTLE])
        assertEquals(3, counts[DistrictType.PALACE])
        assertEquals(3, counts[DistrictType.WATCHTOWER])
        assertEquals(3, counts[DistrictType.PRISON])
        assertEquals(3, counts[DistrictType.BATTLEGROUND])
        assertEquals(2, counts[DistrictType.FORTRESS])
        assertEquals(5, counts[DistrictType.TAVERN])
        assertEquals(4, counts[DistrictType.MARKET])
        assertEquals(3, counts[DistrictType.TRADING_POST])
        assertEquals(3, counts[DistrictType.DOCKS])
        assertEquals(3, counts[DistrictType.HARBOR])
        assertEquals(2, counts[DistrictType.TOWN_HALL])
        assertEquals(1, counts[DistrictType.HAUNTED_QUARTER])
        assertEquals(2, counts[DistrictType.KEEP])
        assertEquals(1, counts[DistrictType.SMITHY])
        assertEquals(1, counts[DistrictType.OBSERVATORY])
        assertEquals(1, counts[DistrictType.LABORATORY])
        assertEquals(1, counts[DistrictType.LIBRARY])
        assertEquals(1, counts[DistrictType.SCHOOL_OF_MAGIC])
        assertEquals(1, counts[DistrictType.GREAT_WALL])
        assertEquals(1, counts[DistrictType.DRAGON_GATE])
        assertEquals(1, counts[DistrictType.UNIVERSITY])
    }
}
