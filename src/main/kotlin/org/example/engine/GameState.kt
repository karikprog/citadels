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

abstract class GamePhase {
    abstract fun handle(state: GameState)
}

class TurnPhase : GamePhase() {
    override fun handle(state: GameState) {
        state.changeGamePhase(this)
        state.activePlayer = null
    }
}

class DraftPhase : GamePhase() {
    override fun handle(state: GameState) {
        state.clearAvailableCharacter()
        val characters = state.settings.generateCharacters()
        // Правила игры для 6 человек
        val randomChar = characters.randomOrNull()
        characters.remove(randomChar)
        state.changeGamePhase(this)
        for (character in characters) {
            state.addDraftCharacter(character)
        }
        if (state.kingInd == -1) {
            state.kingInd = state.players.indices.random()
        }
        for (player in state.players) {
            player.resetCharacter()
        }
        state.activePlayer = state.players[state.kingInd]
    }
}
