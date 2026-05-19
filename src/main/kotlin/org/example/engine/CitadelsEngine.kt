package org.example.engine

import org.example.utils.Settings

class CitadelsEngine(
    private val settings: Settings,
    private val _state: GameState,
    private val _validator: MoveValidator,
) {
    fun startEngine() {
        val deck = settings.generateCitadelsDeck()
        for (card in deck) {
            _state.addDistrict(card)
        }

        for (player in _state.players) {
            player.addGold(2)
            repeat(4) { player.addHand(_state.selectDistrict()) }
        }

        _state.changeGamePhase(DraftPhase)
        _state.gamePhase?.handle(_state)
    }

    fun processAction(action: GameAction): ProcessResult {
        val result = _validator.canExecute(action, _state)
        if (result is ValidationResult.Valid) {
            action.execute(_state)
            val events = mutableListOf<GameEvent>()
            when (action) {
                is EndDraftAction -> events.addAll(moveDraftNextTurn())
                is EndTurnAction -> events.addAll(moveNextTurn())
            }
            return ProcessResult.Valid(events)
        }
        return ProcessResult.Invalid((result as ValidationResult.Invalid).reason)
    }

    private fun moveDraftNextTurn(): List<GameEvent> {
        val charactersPicked = _state.players.count { it.character != 0 }
        if (charactersPicked == _state.players.size) {
            _state.changeGamePhase(TurnPhase)
            _state.gamePhase?.handle(_state)
            return listOf(GameEvent.NewRound) + moveNextTurn()
        }
        _state.switchDraftPlayer()
        return emptyList()
    }

    private fun moveNextTurn(): List<GameEvent> {
        val events = mutableListOf<GameEvent>()
        var rankToSearch = (_state.activePlayer?.character ?: 0) + 1
        while (rankToSearch <= 8) {
            val nextPlayer = _state.players.find { it.character == rankToSearch }

            if (nextPlayer == null) {
                rankToSearch++
                continue
            }

            if (nextPlayer.isAssassinated) {
                events.add(GameEvent.PlayerSkipped(nextPlayer, rankToSearch, SkipReason.ASSASSINATED))
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
                    events.add(GameEvent.GoldStolen(thief, nextPlayer, stolenGold))
                }
            }

            if (nextPlayer.character == 6) {
                nextPlayer.addGold(1)
            } else if (nextPlayer.character == 7) {
                events.add(GameEvent.ArchitectBonus(nextPlayer))
                repeat(2) {
                    nextPlayer.addHand(_state.selectDistrict())
                }
            } else if (nextPlayer.character == 4) {
                _state.kingInd = _state.players.indexOf(nextPlayer)
            }

            events.add(GameEvent.TurnStarted(nextPlayer, rankToSearch))
            return events
        }

        events.add(GameEvent.RoundEnded)
        if (_state.players.any() { it.city.size == 7 }) {
            events.addAll(endGame())
        } else {
            _state.changeGamePhase(DraftPhase)
            _state.gamePhase?.handle(_state)
            events.add(GameEvent.NewRound)
        }
        return events
    }

    private fun endGame(): List<GameEvent> {
        _state.gameOver()
        val scores = _state.players.associateWith { player ->
            player.getScore()
        }

        val sortedPlayers = _state.players.sortedWith(
            compareByDescending<Player> { scores[it] }
                .thenByDescending { it.character }
        )

        return listOf(GameEvent.GameOver(scores, sortedPlayers.first(), sortedPlayers))
    }
}
