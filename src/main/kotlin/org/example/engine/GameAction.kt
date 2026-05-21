package org.example.engine

import java.util.UUID

abstract class GameAction {
    abstract fun execute(state: GameState)
}

class SelectCharacterAction(val selectedCharacter: Int, val player: Player) : GameAction() {
    override fun execute(state: GameState) {

        val character = state.availableCharacter.find { it.rank == selectedCharacter } ?: return

        player.setCharacter(character)
        state.selectCharacter(character)
    }
}

class CollectGoldAction(
    val rank: Int,
) : GameAction() {

    override fun execute(state: GameState) {
        val player = state.activePlayer ?: return

        player.addGold(2)
        player.takeResourcesFlag()
    }
}

class DrowCardAction(val rank: Int) : GameAction() {
    override fun execute(state: GameState) {
        val player = state.activePlayer ?: return

        var cardsCount = if (player.containsCity(DistrictType.OBSERVATORY)) {
            3
        } else {
            2
        }
        for (i in 1..cardsCount) {
            player.temporaryHand.addLast(state.selectDistrict())
        }
        player.takeResourcesFlag()
    }
}

class CollectCardAction(val rank: Int, val cardInx: Int) : GameAction() {
    override fun execute(state: GameState) {
        val player = state.activePlayer ?: return
        if (player.containsCity(DistrictType.LIBRARY)) {
            for (card in player.temporaryHand) {
                player.addHand(card)
            }
        } else {
            player.addHand(player.temporaryHand[cardInx])
            for (card in player.temporaryHand) {
                if (card != player.temporaryHand[cardInx]) {
                    state.addDistrict(card)
                }
            }
        }
        player.temporaryHand.clear()
        player.takeResourcesFlag()
    }
}

class UseAssassinAction(val rank: Int, val victimRank: Int) : GameAction() {
    override fun execute(state: GameState) {
        val player = state.activePlayer ?: return
        val victim: Player? = state.players.find { it.character == victimRank }
        victim?.assassinated()
    }
}

class UseThiefAction(val rank: Int, val robbedRank: Int) : GameAction() {
    override fun execute(state: GameState) {
        val player = state.activePlayer ?: return

        val robbedPlayer = state.players.find { it.character == robbedRank }
        //Переход денег по правилам происходит во время вызова жертвы
        robbedPlayer?.robbedFlag()
    }
}

class UseSwapOtherPlayerMagicianAction(val rank: Int, val otherName: String) : GameAction() {
    override fun execute(state: GameState) {
        val player = state.activePlayer ?: return

        val otherPlayer = state.players.find { it.name == otherName } ?: return
        player.swapHandCards(otherPlayer)
    }
}

class UseSwapDeckMagicianAction(val rank: Int, val cards: List<District>) : GameAction() {
    override fun execute(state: GameState) {
        val player = state.activePlayer ?: return
        for (card in cards) {
            player.deleteCardFromHand(card)
            state.addDistrict(card)
            player.addHand(state.selectDistrict())
        }
    }
}

class BuildDistrictAction(val rank: Int, val districtType: DistrictType) : GameAction() {
    override fun execute(state: GameState) {
        val player = state.activePlayer ?: return
        player.buildDistrict(districtType)
    }
}

class UseWarlordAction(
    val rank: Int,
    val rankVictim: Int,
    val card: District,
) : GameAction() {
    override fun execute(state: GameState) {
        val player = state.activePlayer ?: return

        val victimPlayer = state.players.find { it.character == rankVictim } ?: return

        var coins = 0

        coins += if (victimPlayer.containsCity(DistrictType.GREAT_WALL)) {
            card.cost
        } else {
            card.cost - 1
        }
        player.spendGold(coins)
        victimPlayer.deleteCardFromCity(card)
        victimPlayer.addDestroyedDistrict(card)
    }
}

class UseArchitectBuildAction(val rank: Int, val cards: List<District>) : GameAction() {
    override fun execute(state: GameState) {
        val player = state.activePlayer ?: return


        for (card in cards) {
            player.buildDistrict(card.type)
        }
    }
}

class UseSmithyCardAction(val rank: Int) : GameAction() {
    override fun execute(state: GameState) {
        val player = state.activePlayer ?: return

        player.spendGold(2)
        for (i in 1..3) {
            player.addHand(state.selectDistrict())
        }
    }
}

class UseLaboratoryCardAction(val rank: Int, val card: District) : GameAction() {
    override fun execute(state: GameState) {
        val player = state.activePlayer ?: return

        player.deleteCardFromHand(card)
        player.addGold(1)
    }
}

class EndTurnAction(val rank: Int) : GameAction() {
    override fun execute(state: GameState) {
        val player = state.activePlayer ?: return

        player.resetTurnFlags()
    }
}

class EndDraftAction(val player: Player) : GameAction() {
    override fun execute(state: GameState) {
    }
}

class PassiveTakeGoldAction(val rank: Int) : GameAction() {
    override fun execute(state: GameState) {
        val player = state.activePlayer ?: return

        player.passiveTakeGold()
    }
}



