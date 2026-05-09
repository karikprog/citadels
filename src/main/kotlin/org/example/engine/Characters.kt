package org.example.engine

enum class Color {
    GREEN, BLUE, RED, YELLOW, LILAC, GREY
}

abstract class Character(
    val rank: Int,
    val name: String,
    val color: Color
) {
}

class Assassin : Character(1, "Assassin", Color.GREY)

class Thief : Character(2, "Thief", Color.GREY)

class Magician : Character(3, "Magician", Color.GREY)

class Warlord : Character(8, "Warlord", Color.RED)

class Bishop : Character(5, "Bishop", Color.BLUE)

class Merchant : Character(6, "Merchant", Color.GREEN)

class King : Character(4, "King", Color.YELLOW)

class Architect : Character(7, "Architect", Color.GREY)


