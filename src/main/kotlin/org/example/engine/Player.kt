package org.example.engine

import java.util.UUID

class Player(private val _id: UUID, initialName: String) {
    val name: String = initialName
    var gold: Int = 0
        private set
    private val _hand = mutableListOf<District>()
    private val _city = mutableListOf<District>()
    private var _character: Character? = null
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

    var hasTakenResources = false

    fun getScore(): Int {
        TODO("Implem")
    }

    fun addGold(coins: Int) {
        if (coins >= 0) {
            gold += coins
        }
    }

    fun spendGold(coins: Int) {
        if (coins in 0..<gold) {
            gold -= coins
        }
    }

    fun containsHand(districtType: DistrictType): Boolean {
        return _hand.any() {it.type == districtType}
    }

    fun containsCity(districtType: DistrictType): Boolean {
        return _city.any() {it.type == districtType}
    }

    fun addHand(district: District) {
        _hand.add(district)
    }

    fun assassinated() {
        isAssassinated = true
    }

    fun robbed() {
        gold = 0
        isRobbed = true
    }

    fun buildDistrict(districtType: DistrictType) {
        if (_city.any() { it.type == districtType }) {
            return
        }
        require(city.size < 7) { "The city is already completed" }
        val cardToBuild = _hand.find { it.type == districtType }
        require(!containsHand(districtType)) {"District ${districtType.name} already built"}
        require(cardToBuild != null) { "Player $districtType does not have a card to build district" }
        require(gold < cardToBuild.cost) { "Player $_id doesn't have enough gold to build district" }
        _city.add(cardToBuild)
        _hand.remove(cardToBuild)
        gold -= cardToBuild.cost
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
        _hand.remove(cityToDelete)
    }

    fun addDestroyedDistrict(district: District) {
        _destroyedDistricts.add(district)
    }
}

