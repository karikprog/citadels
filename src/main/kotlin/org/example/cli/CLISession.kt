package org.example.cli

import org.example.engine.CitadelsEngine
import org.example.engine.DraftPhase
import org.example.engine.GameState
import org.example.engine.ValidationResult
import java.lang.IllegalArgumentException

class CLISession(
    private val engine: CitadelsEngine,
    private val state: GameState
) {
    private val handler = CommandHandler(state)

    fun start() {
        println("=== ЦИТАДЕЛИ: ЗАПУСК ===")
        engine.startEngine()

        while (!state.gameOver) {
            printGameState()

            print("\n> ")
            val input = readlnOrNull() ?: continue

            if (input.lowercase() == "exit") {
                println("Выход из игры...")
                break
            }

            val action = handler.parse(input)

            if (action != null) {
                try {
                    when (val result = engine.processAction(action)) {
                        is ValidationResult.Valid -> {
                            println("✅ Действие успешно выполнено.")
                        }
                        is ValidationResult.Invalid -> {
                            println("❌ Ошибка хода: ${result.reason}")
                        }
                    }
                } catch (e: IllegalArgumentException) {
                    println("Критическая ошибка правил (execute): ${e.message}")
                } catch (e: Exception) {
                    println("Системная ошибка: ${e.message}")
                }
            }
        }
        println("=== ИГРА ЗАВЕРШЕНА ===")
    }

    private fun printGameState() {
        val player = state.activePlayer
        println("\n=================================================")
        if (player == null) {
            println("Ожидание системы...")
            return
        }

        val phaseName = state.gamePhase?.javaClass?.simpleName ?: "Неизвестна"
        println("ФАЗА: $phaseName | ХОДИТ: ${player.name}")

        if (state.gamePhase is DraftPhase) {
            val available = state.availableCharacter.joinToString(", ") { "${it.name}(${it.rank})" }
            println("Доступные роли для выбора: $available")
        } else {
            println("Ранг: ${player.character} | Золото: ${player.gold}")

            val cityStr = if (player.city.isEmpty()) "Пусто" else player.city.joinToString { it.type.name }
            val handStr = if (player.hand.isEmpty()) "Пусто" else player.hand.joinToString { it.type.name }

            println("Город: [$cityStr]")
            println("Рука:  [$handStr]")

            if (player.temporaryHand.isNotEmpty()) {
                val tempStr = player.temporaryHand.joinToStringIndexed { index, district -> "$index:${district.type.name}" }
                println("Карты на выбор (нужно сделать keep <index>): [$tempStr]")
            }
        }
    }

    private fun <T> Iterable<T>.joinToStringIndexed(transform: (Int, T) -> String): String {
        return this.mapIndexed { index, item -> transform(index, item) }.joinToString(", ")
    }
}