package org.example.engine

import java.util.ArrayDeque
import java.util.UUID

class GameState {
    private val _players = mutableListOf<Player>()
    private val _districtsQueue = ArrayDeque<District>()
    private val _availableCharacter = mutableListOf<Character>()
    private var _gamePhase: GamePhase? = null

    val gamePhase: GamePhase? get() = _gamePhase
    val players: List<Player> get() = _players
    var kingId: UUID? = null
    var activePlayer: Player? = null
    var isLastRound: Boolean = false

    fun addDistrict(district: District) {
        if (_gamePhase is DraftPhase) {
            return
        }
        _districtsQueue.addLast(district)
    }

    fun selectDistrict(): District {
        return _districtsQueue.removeFirst()
    }

}

abstract class GamePhase {
    abstract fun handle(state: GameState)
}

class TurnPhase : GamePhase() {
    override fun handle(state: GameState) {
        TODO("Not yet implemented")
    }
}

class DraftPhase : GamePhase() {
    override fun handle(state: GameState) {
        TODO("Not yet implemented")
    }
}