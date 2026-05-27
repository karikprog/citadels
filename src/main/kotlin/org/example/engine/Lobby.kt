package org.example.engine

import org.example.repository.MatchRepository

class Lobby(private val repo: MatchRepository) {
    private val _players = mutableListOf<Player>()
    val playersSize: Int get() = _players.size

    fun addPlayer(name: String) {
        if (_players.size < 6) {
            val user = repo.getOrCreateUser(name)
            val player = Player(user.id, name)
            _players.add(player)
        }
    }

    fun createGameState(): GameState {
        return GameState(_players.toList())
    }
}