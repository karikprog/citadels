package org.example.cli

import org.example.engine.CitadelsEngine
import org.example.engine.GameState
import org.example.handlers.CommandHandler


class CLISession(
    private val _handler: CommandHandler,
    private val _engine: CitadelsEngine,
    private val _state: GameState
) {
    fun run() {

    }
}