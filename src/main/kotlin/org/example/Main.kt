package org.example

import org.example.cli.CLISession
import org.example.engine.*
import org.example.repository.MatchSummary
import org.example.utils.InMemoryMatchRepository
import org.example.utils.Settings


fun main() {
    val repo = InMemoryMatchRepository()

    while (true) {
        println("\n=== ЦИТАДЕЛИ ===")
        println("1. Новая игра")
        println("2. Статистика пользователя")
        println("3. Выход")
        print("> ")

        when (readlnOrNull()?.trim()?.lowercase()) {
            "1", "new", "игра" -> startNewGame(repo)
            "2", "stats", "статистика" -> showUserStats(repo)
            "3", "exit", "выход" -> {
                println("До свидания!")
                return
            }
            else -> println("Неизвестная команда")
        }
    }
}

private fun startNewGame(repo: InMemoryMatchRepository) {
    println("\n=== ПОДГОТОВКА К ИГРЕ: ЛОББИ ===")
    val lobby = Lobby()

    while (true) {
        if (lobby.playersSize < 6) {
            println("Введите имя игрока №${lobby.playersSize + 1}:")
        } else {
            println("Введите имя игрока №${lobby.playersSize + 1} или напишите 'start' для запуска игры (макс. 6 игроков):")
        }

        print("> ")
        val input = readlnOrNull()?.trim() ?: break

        if (input.lowercase() == "start") {
            if (lobby.playersSize != 6) {
                println("Нужно 6 игроков")
            }
        }

        if (input.isBlank()) {
            println("Имя игрока не может быть пустым.")
            continue
        }

        lobby.addPlayer(input)
        println("Игрок $input добавлен в лобби.")

        if (lobby.playersSize == 6) {
            println("Достигнуто максимальное количество игроков (6). Переходим к игре...")
            break
        }
    }

    println("\n=== ИНИЦИАЛИЗАЦИЯ ИГРОВОЙ СЕССИИ ===")

    val state = lobby.createGameState()

    val settings = Settings()
    val validator = ClassicMoveValidator()

    val engine = CitadelsEngine(settings, state, validator)
    val session = CLISession(engine, state, repo)

    session.start()
}

private fun showUserStats(repo: InMemoryMatchRepository) {
    print("Введите имя пользователя: ")
    val name = readlnOrNull()?.trim() ?: return
    if (name.isBlank()) {
        println("Имя не может быть пустым.")
        return
    }

    val user = repo.findUserByName(name)
    if (user == null) {
        println("Пользователь '$name' не найден.")
        return
    }

    val stats = repo.getUserStats(user.id)
    println()
    println("=== СТАТИСТИКА: ${user.name} ===")
    println("Всего игр: ${stats.totalGames}")
    println("Побед: ${stats.winGames}")
    println("Поражений: ${stats.losses}")
    println("Рейтинг: ${stats.rating}%")

    val matchHistory = repo.getMatchHistoryForUser(user.id)
    if (matchHistory.isNotEmpty()) {
        println()
        println("История матчей:")
        for ((i, match) in matchHistory.withIndex()) {
            val result = if (match.winnerUserId == user.id) "🏆 Победа" else "Поражение"
            val players = match.playerNames.values.joinToString(", ")
            println("  ${i + 1}. $result | Игроки: $players")
        }
    }
    println()
    println("Нажмите Enter чтобы продолжить...")
    readlnOrNull()
}
