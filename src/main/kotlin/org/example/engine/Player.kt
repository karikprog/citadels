package org.example.engine

import java.util.UUID

class Player(val id: UUID, initialName: String) {
    val name: String = initialName
    var gold: Int = 0
        private set
    private val _hand = mutableListOf<District>()
    private val _city = mutableListOf<District>()
    private var _character: GameCharacter? = null
    private val _destroyedDistricts = mutableListOf<District>()

    val temporaryHand = mutableListOf<District>()

    val city: List<District> get() = _city
    val destroyedDistricts: List<District> get() = _destroyedDistricts
    val character: Int
        get() = _character?.rank ?: 0

    val hand: List<District> get() = _hand
    var isAssassinated: Boolean = false
        private set
    var isRobbed: Boolean = false
        private set
    var hasBuildThisTurn = false
        private set
    var isFirstToFinish = false
        private set
    var hasCollectedIncome = false
        private set

    var hasTakenResources = false
        private set

    fun getScore(): Int {
        var score = 0
        if (isFirstToFinish) {
            score += 4
        } else if (_city.size == 7) {
            score += 2
        }
        val colorSet = _city.map { it.color }.toSet()
        if (colorSet.size == 5
            || (colorSet.size == 4 && _city.any() { it.type == DistrictType.HAUNTED_QUARTER })
        ) {
            score += 3
        }
        for (district in _city) {
            score += district.calculatePoints()
        }
        return score
    }

    fun takeResourcesFlag() {
        hasTakenResources = true
    }

    fun addGold(coins: Int) {
        if (coins >= 0) {
            gold += coins
        }
    }

    fun spendGold(coins: Int) {
        if (coins <= gold) {
            gold -= coins
        }
    }

    fun containsHand(districtType: DistrictType): Boolean {
        return _hand.any() { it.type == districtType }
    }

    fun containsCity(districtType: DistrictType): Boolean {
        return _city.any() { it.type == districtType }
    }

    fun addHand(district: District) {
        _hand.add(district)
    }

    fun assassinated() {
        isAssassinated = true
    }

    fun robbedFlag() {
        gold = 0
        isRobbed = true
    }

    fun buildDistrict(districtType: DistrictType) {
        if (_city.any() { it.type == districtType }) {
            return
        }
        val cardToBuild = _hand.find { it.type == districtType }
        require(!containsCity(districtType)) { "District ${districtType.name} already built" }
        require(cardToBuild != null) { "Player $districtType does not have a card to build district" }
        require(gold >= cardToBuild.cost) { "Player $id doesn't have enough gold to build district" }
        _city.add(cardToBuild)
        _hand.remove(cardToBuild)
        gold -= cardToBuild.cost
        hasBuildThisTurn = true

    }

    fun swapHandCards(other: Player) {
        val tempHand = _hand.toMutableList()
        _hand.clear()
        _hand.addAll(other._hand)
        other._hand.clear()
        other._hand.addAll(tempHand)
    }

    fun deleteCardFromHand(cardToDelete: District) {
        _hand.remove(cardToDelete)
    }

    fun deleteCardFromCity(cityToDelete: District) {
        _city.remove(cityToDelete)
    }

    fun addDestroyedDistrict(district: District) {
        _destroyedDistricts.add(district)
    }

    fun setCharacter(character: GameCharacter) {
        _character = character
    }

    fun passiveTakeGold() {
        var score = 0
        val targetColor = when (_character?.rank) {
            4 -> Color.YELLOW
            5 -> Color.BLUE
            6 -> Color.GREEN
            8 -> Color.RED
            else -> null
        }
        if (targetColor != null) {
            val count = _city.count {
                it.color == targetColor || it.type == DistrictType.SCHOOL_OF_MAGIC
            }

            addGold(count)
        }
        hasCollectedIncome = true
    }

    fun resetTurnFlags() {
        isRobbed = false
        isAssassinated = false
        hasBuildThisTurn = false
        hasCollectedIncome = false
        hasTakenResources = false
    }

    fun resetCharacter() {
        _character = null
    }
}

