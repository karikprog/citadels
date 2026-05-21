package org.example.engine

sealed class GamePhase {
    abstract fun handle(state: GameState)
}

object TurnPhase : GamePhase() {
    override fun handle(state: GameState) {
        state.activePlayer = null
    }
}

object DraftPhase : GamePhase() {
    override fun handle(state: GameState) {
        state.clearAvailableCharacter()
        val characters = state.settings.generateCharactersForSixPlayers()

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