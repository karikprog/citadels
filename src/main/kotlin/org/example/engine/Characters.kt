package org.example.engine

enum class Color {
    GREEN, BLUE, RED, YELLOW, LILAC, GREY
}

abstract class GameCharacter(
    val rank: Int,
    val name: String,
    val color: Color
) {
}

class Assassin : GameCharacter(1, "Assassin", Color.GREY)

class Thief : GameCharacter(2, "Thief", Color.GREY)

class Magician : GameCharacter(3, "Magician", Color.GREY)

class Warlord : GameCharacter(8, "Warlord", Color.RED)

class Bishop : GameCharacter(5, "Bishop", Color.BLUE)

class Merchant : GameCharacter(6, "Merchant", Color.GREEN)

class King : GameCharacter(4, "King", Color.YELLOW)

class Architect : GameCharacter(7, "Architect", Color.GREY)


