package org.example.cli

import org.example.engine.*
import org.example.repository.MatchRepository
import org.example.repository.MatchSummary
import java.util.UUID

sealed class ParseResult {
    data class Success(val action: GameAction) : ParseResult()
    data class Display(val message: String) : ParseResult()
    data class Error(val message: String) : ParseResult()
    data object Exit : ParseResult()
}

class CommandHandler(
    private val _state: GameState,
    private val _repo: MatchRepository? = null
) {
    fun parse(input: String): ParseResult {
        val tokens = input.trim().split(Regex("\\s+"))
        if (tokens.isEmpty() || tokens[0].isBlank()) return ParseResult.Error("Введите команду")

        val command = tokens[0].lowercase()
        val args = tokens.drop(1)

        if (command == "stats") return ParseResult.Display(buildGameStats())
        if (command == "exit") return ParseResult.Exit

        val player = _state.activePlayer ?: return ParseResult.Error("Нет активного игрока")

        return when (_state.gamePhase) {
            is DraftPhase -> handleDraftCommands(command, args, player)
            is TurnPhase -> handleTurnCommands(command, args, player)
            else -> ParseResult.Error("Неизвестная фаза игры")
        }
    }

    fun saveFinishedMatch(): MatchSummary? {
        val repo = this._repo ?: return null
        if (!_state.gameOver) return null

        val scores = _state.players.associate { it.id to it.getScore() }
        val winner = _state.players.maxBy { it.getScore() }
        val summary = MatchSummary(
            matchId = UUID.randomUUID(),
            winnerUserId = winner.id,
            finalScore = scores,
            playerNames = _state.players.associate { it.id to it.name }
        )
        repo.saveCompletedMatch(summary.matchId, summary)
        repo.updateRatingsAfterMatch(summary)
        return summary
    }

    private fun buildGameStats(): String {
        val sb = StringBuilder()
        sb.appendLine("=== СТАТИСТИКА ИГРЫ ===")
        val phaseName = _state.gamePhase?.javaClass?.simpleName ?: "Неизвестна"
        sb.appendLine("Фаза: $phaseName")
        sb.appendLine()

        val header = "%-12s %-18s %6s  %-30s %5s".format("Игрок", "Персонаж", "Золото", "Город (кварталы)", "Очки")
        sb.appendLine(header)
        sb.appendLine("-".repeat(header.length))

        for (p in _state.players) {
            val charName = characterName(p.character)
            val charStr = if (p.character != 0) "$charName (${p.character})" else "—"
            val goldStr = p.gold.toString()
            val cityStr = if (p.city.isEmpty()) "—"
            else p.city.joinToString(", ") { it.type.name }
            val score = p.getScore()
            sb.appendLine("%-12s %-18s %6s  %-30s %5s".format(p.name, charStr, goldStr, cityStr, score))
        }
        return sb.toString()
    }

    private fun characterName(rank: Int): String = when (rank) {
        1 -> "Убийца"
        2 -> "Вор"
        3 -> "Маг"
        4 -> "Король"
        5 -> "Епископ"
        6 -> "Купец"
        7 -> "Зодчий"
        8 -> "Властелин"
        else -> ""
    }

    private fun handleDraftCommands(command: String, args: List<String>, player: Player): ParseResult {
        return when (command) {
            "draft" -> {
                val rank = args.getOrNull(0)?.toIntOrNull()
                if (rank != null) ParseResult.Success(SelectCharacterAction(rank, player))
                else ParseResult.Error("Укажите ранг: draft <rank>")
            }
            "end" -> ParseResult.Success(EndDraftAction(player))
            else -> ParseResult.Error("Доступные команды: draft <rank>, end")
        }
    }

    private fun handleTurnCommands(command: String, args: List<String>, player: Player): ParseResult {
        val rank = player.character

        return when (command) {
            "gold" -> ParseResult.Success(CollectGoldAction(rank))
            "draw" -> ParseResult.Success(DrowCardAction(rank))
            "keep" -> {
                val index = args.getOrNull(0)?.toIntOrNull()
                if (index != null) ParseResult.Success(CollectCardAction(rank, index))
                else ParseResult.Error("Укажите индекс карты (0 или 1): keep <index>")
            }
            "income" -> ParseResult.Success(PassiveTakeGoldAction(rank))
            "build" -> {
                val type = parseDistrictType(args.getOrNull(0))
                if (type != null) ParseResult.Success(BuildDistrictAction(rank, type))
                else ParseResult.Error("Квартал '${args.getOrNull(0)}' не существует. Использование: build <type>")
            }
            "assassin" -> {
                val victimRank = args.getOrNull(0)?.toIntOrNull()
                if (victimRank != null) ParseResult.Success(UseAssassinAction(rank, victimRank))
                else ParseResult.Error("Укажите цель: assassin <rank>")
            }
            "thief" -> {
                val victimRank = args.getOrNull(0)?.toIntOrNull()
                if (victimRank != null) ParseResult.Success(UseThiefAction(rank, victimRank))
                else ParseResult.Error("Укажите цель: thief <rank>")
            }
            "magician" -> handleMagician(args, player, rank)
            "warlord" -> {
                val victimRank = args.getOrNull(0)?.toIntOrNull()
                val type = parseDistrictType(args.getOrNull(1))
                if (victimRank != null && type != null) {
                    val victim = _state.players.find { it.character == victimRank }
                    val card = victim?.city?.find { it.type == type }
                    if (card != null) ParseResult.Success(UseWarlordAction(rank, victimRank, card))
                    else ParseResult.Error("У игрока $victimRank нет здания ${args.getOrNull(1)}")
                } else ParseResult.Error("Использование: warlord <victimRank> <districtType>")
            }
            "architect" -> {
                val cards = args.mapNotNull { parseDistrictType(it) }.mapNotNull { type ->
                    player.hand.find { it.type == type }
                }
                if (cards.isNotEmpty()) ParseResult.Success(UseArchitectBuildAction(rank, cards))
                else ParseResult.Error("Укажите карты из руки: architect <type1> <type2>...")
            }
            "smithy" -> ParseResult.Success(UseSmithyCardAction(rank))
            "lab" -> {
                val type = parseDistrictType(args.getOrNull(0))
                val card = player.hand.find { it.type == type }
                if (card != null) ParseResult.Success(UseLaboratoryCardAction(rank, card))
                else ParseResult.Error("Укажите карту из руки: lab <type>")
            }
            "end" -> ParseResult.Success(EndTurnAction(rank))
            else -> ParseResult.Error("Неизвестная команда. Введите help для списка (пока не реализовано).")
        }
    }

    private fun handleMagician(args: List<String>, player: Player, rank: Int): ParseResult {
        val subCommand = args.getOrNull(0)?.lowercase()
        return when (subCommand) {
            "swap" -> {
                val targetName = args.getOrNull(1)
                if (targetName != null) ParseResult.Success(UseSwapOtherPlayerMagicianAction(rank, targetName))
                else ParseResult.Error("Укажите имя игрока: magician swap <name>")
            }
            "discard" -> {
                val types = args.drop(1).mapNotNull { parseDistrictType(it) }
                val cardsToDiscard = types.mapNotNull { type -> player.hand.find { it.type == type } }
                if (cardsToDiscard.size == types.size && types.isNotEmpty()) {
                    ParseResult.Success(UseSwapDeckMagicianAction(rank, cardsToDiscard))
                } else {
                    ParseResult.Error("Не все указанные карты найдены в руке. Использование: magician discard <type1> <type2>")
                }
            }
            else -> ParseResult.Error("Использование: magician swap <name> ИЛИ magician discard <type1> <type2>...")
        }
    }

    private fun parseDistrictType(input: String?): DistrictType? {
        if (input == null) return null
        return try {
            DistrictType.valueOf(input.uppercase())
        } catch (e: IllegalArgumentException) {
            null
        }
    }
}
