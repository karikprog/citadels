package org.example.engine

import org.example.utils.Settings
import java.util.ArrayDeque

class GameState(private val _players: List<Player>) {
    val settings = Settings()
    private val _districtsQueue = ArrayDeque<District>()
    private val _availableCharacter = mutableListOf<GameCharacter>()
    private var _gamePhase: GamePhase? = null

    val availableCharacter: List<GameCharacter> get() = _availableCharacter
    val gamePhase: GamePhase? get() = _gamePhase
    val players: List<Player> get() = _players
    var kingInd: Int = -1
    var activePlayer: Player? = null
    var gameOver: Boolean = false
        private set

    fun addDistrict(district: District) {
        _districtsQueue.addLast(district)
    }

    fun isDistrictDeckEmpty(): Boolean {
        return _districtsQueue.isEmpty()
    }

    fun switchDraftPlayer() {
        val currentIndex = players.indexOf(activePlayer)
        activePlayer = _players[(currentIndex + 1) % _players.size]
    }

    fun addDraftCharacter(draftCharacter: GameCharacter) {
        if (_gamePhase is DraftPhase) {
            _availableCharacter.add(draftCharacter)
        }
    }

    fun changeGamePhase(gamePhase: GamePhase) {
        _gamePhase = gamePhase
    }

    fun selectDistrict(): District {
        return _districtsQueue.removeFirst()
    }

    fun selectCharacter(character: GameCharacter) {
        _availableCharacter.remove(character)
    }

    fun clearAvailableCharacter() {
        _availableCharacter.clear()
    }

    fun gameOver() {
        gameOver = true
    }
}

