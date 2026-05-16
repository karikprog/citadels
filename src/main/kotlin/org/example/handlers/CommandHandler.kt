package org.example.cli

import org.example.engine.*
import java.lang.IllegalArgumentException

class CommandHandler(private val state: GameState) {

    fun parse(input: String): GameAction? {
        val tokens = input.trim().split(Regex("\\s+"))
        if (tokens.isEmpty() || tokens[0].isBlank()) return null

        val command = tokens[0].lowercase()
        val args = tokens.drop(1)
        val player = state.activePlayer ?: return null

        return when (state.gamePhase) {
            is DraftPhase -> handleDraftCommands(command, args, player)
            is TurnPhase -> handleTurnCommands(command, args, player)
            else -> null
        }
    }

    private fun handleDraftCommands(command: String, args: List<String>, player: Player): GameAction? {
        return when (command) {
            "draft" -> {
                val rank = args.getOrNull(0)?.toIntOrNull()
                if (rank != null) SelectCharacterAction(rank, player)
                else { println("Укажите ранг: draft <rank>"); null }
            }
            "end" -> EndDraftAction(player)
            else -> {
                println("Доступные команды: draft <rank>, end")
                null
            }
        }
    }

    private fun handleTurnCommands(command: String, args: List<String>, player: Player): GameAction? {
        val rank = player.character

        return when (command) {
            "gold" -> CollectGoldAction(rank)
            "draw" -> DrowCardAction(rank)
            "keep" -> {
                val index = args.getOrNull(0)?.toIntOrNull()
                if (index != null) CollectCardAction(rank, index)
                else { println("Укажите индекс карты (0 или 1): keep <index>"); null }
            }
            "income" -> PassiveTakeGoldAction(rank)
            "build" -> {
                val type = parseDistrictType(args.getOrNull(0))
                if (type != null) BuildDistrictAction(rank, type) else null
            }
            "assassin" -> {
                val victimRank = args.getOrNull(0)?.toIntOrNull()
                if (victimRank != null) UseAssassinAction(rank, victimRank)
                else { println("Укажите цель: assassin <rank>"); null }
            }
            "thief" -> {
                val victimRank = args.getOrNull(0)?.toIntOrNull()
                if (victimRank != null) UseThiefAction(rank, victimRank)
                else { println("Укажите цель: thief <rank>"); null }
            }
            "magician" -> handleMagician(args, player, rank)
            "warlord" -> {
                val victimRank = args.getOrNull(0)?.toIntOrNull()
                val type = parseDistrictType(args.getOrNull(1))
                if (victimRank != null && type != null) {
                    val victim = state.players.find { it.character == victimRank }
                    val card = victim?.city?.find { it.type == type }
                    if (card != null) UseWarlordAction(rank, victimRank, card)
                    else { println("У игрока $victimRank нет здания $type"); null }
                } else { println("Использование: warlord <victimRank> <districtType>"); null }
            }
            "architect" -> {
                val cards = args.mapNotNull { parseDistrictType(it) }.mapNotNull { type ->
                    player.hand.find { it.type == type }
                }
                if (cards.isNotEmpty()) UseArchitectBuildAction(rank, cards)
                else { println("Укажите карты из руки: architect <type1> <type2>..."); null }
            }
            "smithy" -> UseSmithyCardAction(rank)
            "lab" -> {
                val type = parseDistrictType(args.getOrNull(0))
                val card = player.hand.find { it.type == type }
                if (card != null) UseLaboratoryCardAction(rank, card)
                else { println("Укажите карту из руки: lab <type>"); null }
            }
            "end" -> EndTurnAction(rank)
            else -> {
                println("Неизвестная команда. Введите help для списка (пока не реализовано).")
                null
            }
        }
    }

    private fun handleMagician(args: List<String>, player: Player, rank: Int): GameAction? {
        val subCommand = args.getOrNull(0)?.lowercase()
        return when (subCommand) {
            "swap" -> {
                val targetName = args.getOrNull(1)
                if (targetName != null) UseSwapOtherPlayerMagicianAction(rank, targetName)
                else { println("Укажите имя игрока: magician swap <name>"); null }
            }
            "discard" -> {
                val types = args.drop(1).mapNotNull { parseDistrictType(it) }
                val cardsToDiscard = types.mapNotNull { type -> player.hand.find { it.type == type } }
                if (cardsToDiscard.size == types.size && types.isNotEmpty()) {
                    UseSwapDeckMagicianAction(rank, cardsToDiscard)
                } else {
                    println("Не все указанные карты найдены в руке. Использование: magician discard <type1> <type2>"); null
                }
            }
            else -> {
                println("Использование: magician swap <name> ИЛИ magician discard <type1> <type2>..."); null
            }
        }
    }

    private fun parseDistrictType(input: String?): DistrictType? {
        if (input == null) return null
        return try {
            DistrictType.valueOf(input.uppercase())
        } catch (e: IllegalArgumentException) {
            println("Квартал '$input' не существует.")
            null
        }
    }
}