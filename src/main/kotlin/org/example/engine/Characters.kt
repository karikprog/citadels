package org.example.engine

enum class Color {
    GREEN, BLUE, RED, YELLOW, LILAC, GREY
}

sealed class GameCharacter(
    val rank: Int,
    val name: String,
    val color: Color
) {
}

object Assassin : GameCharacter(1, "Assassin", Color.GREY)

object Thief : GameCharacter(2, "Thief", Color.GREY)

object Magician : GameCharacter(3, "Magician", Color.GREY)

object Warlord : GameCharacter(8, "Warlord", Color.RED)

object Bishop : GameCharacter(5, "Bishop", Color.BLUE)

object Merchant : GameCharacter(6, "Merchant", Color.GREEN)

object King : GameCharacter(4, "King", Color.YELLOW)

object Architect : GameCharacter(7, "Architect", Color.GREY)


