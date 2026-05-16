package org.example

import org.example.cli.CLISession
import org.example.engine.*
import org.example.utils.Settings
import org.example.repository.HistoryRecorder

fun main() {
    println("=== ПОДГОТОВКА К ИГРЕ: ЛОББИ ===")
    val lobby = Lobby()
    val scanner = java.util.Scanner(System.`in`)

    while (true) {
        if (lobby.playersSize < 6) {
            println("Введите имя игрока №${lobby.playersSize + 1}:")
        } else {
            println("Введите имя игрока №${lobby.playersSize + 1} или напишите 'start' для запуска игры (макс. 6 игроков):")
        }

        print("> ")
        if (!scanner.hasNextLine()) break
        val input = scanner.nextLine().trim()

        if (input.lowercase() == "start") {
            if (lobby.playersSize != 6) {
                println("Нужно 6 игроков")
            }
        }

        if (input.isBlank()) {
            println("❌ Имя игрока не может быть пустым.")
            continue
        }

        lobby.addPlayer(input)
        println("✅ Игрок $input добавлен в лобби.")

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
    val session = CLISession(engine, state)

    session.start()
}