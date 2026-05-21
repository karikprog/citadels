package org.example.engine

enum class DistrictType {
    // Blue
    TEMPLE, CHURCH, MONASTERY, CATHEDRAL,

    // Yellow
    MANOR, CASTLE, PALACE,

    // Red
    WATCHTOWER, PRISON, BATTLEGROUND, FORTRESS,

    // Green
    TAVERN, MARKET, TRADING_POST, DOCKS, HARBOR, TOWN_HALL,

    // Purple
    HAUNTED_QUARTER, KEEP, SMITHY, GRAVEYARD, OBSERVATORY,
    LABORATORY, LIBRARY, SCHOOL_OF_MAGIC, GREAT_WALL, DRAGON_GATE, UNIVERSITY
}

abstract class District(
    val type: DistrictType,
    val name: String,
    val cost: Int,
    val color: Color
) {
    abstract fun calculatePoints(): Int
}

class StandardDistrict(type: DistrictType, name: String, cost: Int, color: Color) : District(type, name, cost, color) {
    override fun calculatePoints(): Int {
        return cost
    }
}

class SpecialDistrict(type: DistrictType, name: String, cost: Int, color: Color, val effectDescription: String) :
    District(type, name, cost, color) {
    override fun calculatePoints(): Int {
        return if (type == DistrictType.UNIVERSITY || type == DistrictType.DRAGON_GATE) return 8 else cost
    }
}