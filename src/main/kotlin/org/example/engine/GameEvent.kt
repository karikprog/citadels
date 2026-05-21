package org.example.engine

sealed class GameEvent {
    data class PlayerSkipped(val player: Player, val rank: Int, val reason: SkipReason) : GameEvent()
    data class GoldStolen(val thief: Player, val victim: Player, val amount: Int) : GameEvent()
    data class ArchitectBonus(val player: Player) : GameEvent()
    data class TurnStarted(val player: Player, val rank: Int) : GameEvent()
    data object RoundEnded : GameEvent()
    data class GameOver(val scores: Map<Player, Int>, val winner: Player, val sortedPlayers: List<Player>) : GameEvent()
    data object NewRound : GameEvent()
}

enum class SkipReason { ASSASSINATED }
