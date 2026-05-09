package org.example.engine


sealed class ValidationResult {
    object Valid : ValidationResult()
    data class Invalid(val reason: String) : ValidationResult()
}

interface MoveValidator {
    fun canExecute(action: GameAction, state: GameState): ValidationResult
}
