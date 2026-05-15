package org.example.engine

import org.example.cli.CLISession
import org.example.repository.HistoryRecorder
import org.example.repository.MatchSummary
import org.example.user.LeaderboardService
import org.example.utils.Settings
import java.util.UUID

class CitadelsEngine(
    private val settings: Settings,
    private val _state: GameState,
    private val _validator: MoveValidator,
    private val _recorder: HistoryRecorder,
) {
    fun startEngine() {
        println("Движок запускается")
        val deck = settings.generateCitadelsDeck()
        for (card in deck) {
            _state.addDistrict(card)
        }

        for (player in _state.players) {
            player.addGold(2)
            repeat(4) { player.addHand(_state.selectDistrict()) }
        }

        _state.changeGamePhase(DraftPhase())
        _state.gamePhase?.handle(_state)
    }

    fun processAction(action: GameAction): ValidationResult {
        val result = _validator.canExecute(action, _state)
        if (result is ValidationResult.Valid) {
            action.execute(_state)
            if (action is EndDraftAction) {
                moveDraftNextTurn()
            } else if (action is EndTurnAction) {
                moveNextTurn()
            }
        }
        return result
    }

    private fun moveDraftNextTurn() {
        val charactersPicked = _state.players.count {it.character != 0}
        if (charactersPicked  == _state.players.size) {
            _state.changeGamePhase(TurnPhase())
            _state.gamePhase?.handle(_state)
        }
        _state.switchDraftPlayer()
    }

    private fun moveNextTurn() {
        var rankToSearch = (_state.activePlayer?.character ?: 0) + 1
        while (rankToSearch <= 8) {
            val nextPlayer = _state.players.find { it.character == rankToSearch }

            if (nextPlayer == null) {
                rankToSearch++
                continue
            }

            if (nextPlayer.isAssassinated) {
                println("Ранг $rankToSearch (${nextPlayer.name}) был убит и пропускает ход.")
                rankToSearch++
                continue
            }

            _state.activePlayer = nextPlayer

            if (nextPlayer.isRobbed) {
                val thief = _state.players.find { it.character == 2 }
                if (thief != null) {
                    val stolenGold = nextPlayer.gold
                    nextPlayer.spendGold(stolenGold)
                    thief.addGold(stolenGold)
                    println("Вор (ранг 2) украл $stolenGold золотых у игрока ${nextPlayer.name}!")
                }
            }
            // Passive merchant ability
            if (nextPlayer.character == 6) {
                nextPlayer.addGold(1)
            }

            println("Вызывается ранг $rankToSearch: ${nextPlayer.name}")
            return
        }

        handleRoundEnd()
        }

    private fun handleRoundEnd() {
        println("Все персонажи завершили свои ходы")
        if (_state.players.any() {it.city.size == 7}) {
            println("Игра закончена")
            endGame()
        } else {
            _state.changeGamePhase(DraftPhase())
            _state.gamePhase?.handle(_state)
        }
    }

    private fun endGame() {
        _state.gameOver()
        val scores = _state.players.associateWith {
            player -> player.getScore()
        }

        val sortedScores = _state.players.sortedWith(
            compareByDescending<Player> { scores[it] }
                .thenByDescending { it.character }
        )
        println("Победил: ${sortedScores.first()}")
        println("Финальная таблица:")
        for (player in sortedScores) {
            println("${player.name}: ${scores[player]}")
        }

    }
}