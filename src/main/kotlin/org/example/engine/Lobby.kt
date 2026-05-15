package org.example.engine

import java.util.UUID

// В будущем тут будет обращение к базе данных и доставание пользователя либо его создание
class Lobby {
    private val _players = mutableListOf<Player>()

    fun addPlayer(name: String) {
        if (_players.size <= 6) {
            val player = Player(UUID.randomUUID(), name)
            _players.add(player)
        }
    }

    fun createGameState(): GameState {
        return GameState(_players.toList())
    }
}