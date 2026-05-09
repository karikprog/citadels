package org.example.engine

import org.example.render.BoardRenderer
import org.example.repository.HistoryRecorder
import org.example.user.LeaderboardService
import org.example.utils.Settings

class CitadelsEngine(
    private val settings: Settings,
    private val _render: BoardRenderer,
    private val _state: GameState,
    private val _validator: MoveValidator,
    private val _recorder: HistoryRecorder,
    private val _leaderboard: LeaderboardService
) {
    fun startEngine() {
        val deck = settings.generateCitadelsDeck()
        for (card in deck) {
            _state.addDistrict(card)
        }

    }

    fun processAction(action: GameAction): ValidationResult {
        val result = _validator.canExecute(action, _state)
        if (result is ValidationResult.Valid) {
            action.execute(_state)
        }
        return result
    }

    fun nextTurn() {
        TODO("Implem")
    }
}