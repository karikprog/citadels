package org.example.cli

import org.example.engine.*
import org.example.repository.MatchRepository

class CLISession(
    private val engine: CitadelsEngine,
    private val state: GameState,
    repo: MatchRepository? = null
) {
    private val handler = CommandHandler(state, repo)

    fun start() {
        println("=== ЦИТАДЕЛИ: ЗАПУСК ===")
        engine.startEngine()

        while (!state.gameOver) {
            printGameState()

            print("\n> ")
            val input = readlnOrNull() ?: continue

            when (val parseResult = handler.parse(input)) {
                is ParseResult.Exit -> {
                    println("Выход из игры...")
                    break
                }
                is ParseResult.Error -> println("${parseResult.message}")
                is ParseResult.Display -> println(parseResult.message)
                is ParseResult.Success -> {
                    when (val result = engine.processAction(parseResult.action)) {
                        is ProcessResult.Valid -> {
                            result.events.forEach { println(formatEvent(it)) }
                            println("Действие успешно выполнено.")
                        }
                        is ProcessResult.Invalid -> {
                            println("Ошибка хода: ${result.reason}")
                        }
                    }
                }
            }
        }

        println("=== ИГРА ЗАВЕРШЕНА ===")
        val summary = handler.saveFinishedMatch()
        if (summary != null) {
            println("Результат сохранён.")
        }
        println("Нажмите Enter чтобы продолжить...")
        readlnOrNull()
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
            val sorted = state.availableCharacter.sortedBy { it.rank }
            println("Доступные роли для выбора:")
            for (ch in sorted) {
                println("  ${ch.rank}: ${characterName(ch.rank)} — ${characterAbility(ch.rank)}")
            }
            println("Команды: $DRAFT_COMMANDS")
        } else {
            println("Ранг: ${player.character} (${characterName(player.character)}) | Золото: ${player.gold}")

            if (player.city.isNotEmpty()) {
                println("Город: [${player.city.joinToString { formatDistrict(it) }}]")
            }
            if (player.hand.isNotEmpty()) {
                println("Рука:  [${player.hand.joinToString { formatDistrict(it) }}]")
            }

            if (player.temporaryHand.isNotEmpty()) {
                val tempStr = player.temporaryHand.joinToStringIndexed { index, district ->
                    "$index:${formatDistrict(district)}"
                }
                println("Карты на выбор (keep <index>): [$tempStr]")
            }
            println("Команды: $TURN_COMMANDS")
        }
    }

    private fun formatDistrict(district: District): String {
        val effect = if (district is SpecialDistrict) district.effectDescription else null
        return if (effect != null) "${district.type.name}(${district.cost}): $effect"
        else "${district.type.name}(${district.cost})"
    }

    private fun formatEvent(event: GameEvent): String {
        return when (event) {
            is GameEvent.PlayerSkipped -> "${event.player.name} (ранг ${event.rank}) был убит и пропускает ход."
            is GameEvent.GoldStolen -> "Вор украл ${event.amount} золотых у игрока ${event.victim.name}!"
            is GameEvent.ArchitectBonus -> "Зодчий получает 2 карты на руку"
            is GameEvent.TurnStarted -> "Ходит ранг ${event.rank}: ${event.player.name}"
            is GameEvent.RoundEnded -> "Все персонажи завершили свои ходы"
            is GameEvent.NewRound -> "--- Новый раунд! ---"
            is GameEvent.GameOver -> buildString {
                appendLine("Игра закончена!")
                appendLine("Победил: ${event.winner.name}")
                appendLine("Финальная таблица:")
                event.sortedPlayers.forEach { appendLine("  ${it.name}: ${event.scores[it]}") }
            }
        }
    }

    companion object {
        private fun characterName(rank: Int): String = when (rank) {
            1 -> "Убийца"
            2 -> "Вор"
            3 -> "Маг"
            4 -> "Король"
            5 -> "Епископ"
            6 -> "Купец"
            7 -> "Зодчий"
            8 -> "Властелин"
            else -> "Неизвестно"
        }

        private fun characterAbility(rank: Int): String = when (rank) {
            1 -> "Назовите персонажа, которого вы хотите убить. \nИгрок, выбравший этого персонажа, полностью пропускает текущий раунд"
            2 -> "Назовите любого персонажа, кроме Ассасина и его жертвы. \nКогда вызванный персонаж вскроется, вы забираете все его золотые монеты себе"
            3 -> "Поменяйтесь всеми своими картами с руки с картами с руки любого другого игрока \n(даже если у вас 0 карт, вы просто забираете его карты)"
            4 -> "Вы получаете жетон короны и будете первым распределять персонажей в следующем раунде. \nПолучите по 1 золотому за каждый благородный (жёлтый) квартал в вашем городе"
            5 -> "Кондотьер не может разрушать кварталы в вашем городе. \nПолучите по 1 золотому за каждый церковный (синий) квартал в вашем городе"
            6 -> "Вы получаете 1 дополнительный золотой после выполнения основного действия. \nПолучите по 1 золотому за каждый торговый (зелёный) квартал в вашем городе"
            7 -> "После выполнения обязательного действия возьмите 2 дополнительные карты из колоды в руку. \nВы можете построить до 3 кварталов за этот ход."
            8 -> "Вы можете разрушить один любой квартал в городе другого игрока, заплатив в банк \nего стоимость минус 1. Получите по 1 золотому за каждый военный (красный) квартал в вашем городе"
            else -> ""
        }

        private const val DRAFT_COMMANDS = "draft <rank>, end, stats"
        private const val TURN_COMMANDS = "gold, draw, keep <index>, income, build <type>,\n " +
                "assassin <rank>, thief <rank>, " +
                "magician {swap <name> | discard <type>...}, \n" +
                "warlord <rank> <type>, architect <type>..., " +
                "smithy, lab <type>, end, stats, exit"
    }

    private fun <T> Iterable<T>.joinToStringIndexed(transform: (Int, T) -> String): String {
        return this.mapIndexed { index, item -> transform(index, item) }.joinToString(", ")
    }
}
