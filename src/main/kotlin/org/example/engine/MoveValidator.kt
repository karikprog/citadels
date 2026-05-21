package org.example.engine


sealed class ValidationResult {
    object Valid : ValidationResult()
    data class Invalid(val reason: String) : ValidationResult()
}

sealed class ProcessResult {
    data class Valid(val events: List<GameEvent> = emptyList()) : ProcessResult()
    data class Invalid(val reason: String) : ProcessResult()
}

interface MoveValidator {
    fun canExecute(action: GameAction, state: GameState): ValidationResult
}
