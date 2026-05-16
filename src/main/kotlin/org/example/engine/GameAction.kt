package org.example.engine

import java.util.UUID

abstract class GameAction {
    abstract fun execute(state: GameState)
}

class SelectCharacterAction(val selectedCharacter: Int, val player: Player) : GameAction() {
    override fun execute(state: GameState) {
        require(player == state.activePlayer) {
            "The transferred player and the active player do not match"
        }
        require(player.character == 0) {
            "The player already selected character"
        }
        val character = state.availableCharacter.find { it.rank == selectedCharacter }
        require(character != null) {
            "The character $selectedCharacter is not available"
        }

        player.setCharacter(character)
        state.selectCharacter(character)
    }
}

class CollectGoldAction(
    val rank: Int,
) : GameAction() {

    override fun execute(state: GameState) {
        val player = state.activePlayer ?: return
        require(player.character == rank) { "Players rank ${player.character} does not match rank $rank" }

        player.addGold(2)
        player.takeResources()
    }
}

class DrowCardAction(val rank: Int) : GameAction() {
    override fun execute(state: GameState) {
        val player = state.activePlayer ?: return
        require(player.character == rank) { "Players rank ${player.character} does not match rank $rank" }

        var cardsCount = if (player.containsCity(DistrictType.OBSERVATORY)) {
            3
        } else {
            2
        }
        for (i in 1..cardsCount) {
            player.temporaryHand.addLast(state.selectDistrict())
        }
        player.takeResources()
    }
}

class CollectCardAction(val rank: Int, val cardInx: Int) : GameAction() {
    override fun execute(state: GameState) {
        val player = state.activePlayer ?: return
        require(player.character == rank) { "Players rank ${player.character} does not match rank $rank" }
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
        player.takeResources()
    }
}

class UseAssassinAction(val rank: Int, val victimRank: Int) : GameAction() {
    override fun execute(state: GameState) {
        val player = state.activePlayer ?: return
        require(player.character == rank) { "Players rank ${player.character} does not match rank $rank" }
        val victim: Player? = state.players.find { it.character == victimRank }
        victim?.assassinated()
    }
}

class UseThiefAction(val rank: Int, val robbedRank: Int) : GameAction() {
    override fun execute(state: GameState) {
        val player = state.activePlayer ?: return
        require(player.character == rank) { "Players rank ${player.character} does not match rank $rank" }
        require(robbedRank != rank) { "The player ${player.character} steals from himself" }

        val assassinVictim = state.players.find { it.isAssassinated }
        require(assassinVictim != null && assassinVictim.character != robbedRank) {
            "The player ${player.character} cannot steal from the assassin's victim."
        }
        require(robbedRank != 1) { "The player ${player.character} cannot steal from the assassin" }

        // val robbedPlayer = state.players.find { it.character == robbedRank }
        //   if (robbedPlayer != null) {
        //         player.addGold(robbedPlayer.gold)
        //           robbedPlayer.robbed()
//        }
    }
}

class UseSwapOtherPlayerMagicianAction(val rank: Int, val otherName: String) : GameAction() {
    override fun execute(state: GameState) {
        val player = state.activePlayer ?: return
        require(player.character == rank) { "Players rank ${player.character} does not match rank $rank" }

        val otherPlayer = state.players.find { it.name == otherName }
        require(otherPlayer != null) { "Player $otherName does not exist" }
        player.swapHandCards(otherPlayer)
    }
}

class UseSwapDeckMagicianAction(val rank: Int, val cards: List<District>) : GameAction() {
    override fun execute(state: GameState) {
        val player = state.activePlayer ?: return
        require(player.character == rank) { "Players rank ${player.character} not match rank $rank" }
        require(cards.size <= player.hand.size) { "Amount should not be greater then hand size" }
        val tempHand = player.hand.toMutableList()
        for (card in cards) {
            require(tempHand.remove(card)) { "Card ${card.name} does not exist in your deck" }
        }
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
        require(player.character == rank) { "Players rank ${player.character} does not match rank $rank" }
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
        require(player.character == rank) {
            "Players rank ${player.character} not match rank $rank"
        }
        val victimPlayer = state.players.find { it.character == rankVictim }
        require(victimPlayer != null) { "The bishop's quarter must not be destroyed" }
        require(rankVictim != 5 || (rankVictim == 5 && victimPlayer.isAssassinated)) {
            "The bishop's quarter must not be destroyed"
        }
        require(victimPlayer.city.size < 7) { "You can't destroy a completed city" }
        require(card.type != DistrictType.KEEP) {
            "You can't destroy a Keep card"
        }
        require(card in victimPlayer.city) {
            "Victim haven`t $card"
        }
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
        require(player.character == rank) {
            "Players rank ${player.character} not match rank $rank"
        }
        require(cards.size <= 3 && cards.size <= player.hand.size) {
            "Amount should not be greater then hand size"
        }
        require(cards.distinct().size == cards.size) { "The districts should be different" }
        for (card in cards) {
            player.buildDistrict(card.type)
        }
    }
}

class UseSmithyCardAction(val rank: Int) : GameAction() {
    override fun execute(state: GameState) {
        val player = state.activePlayer ?: return
        require(player.character == rank) { "Players rank ${player.character} does not match rank $rank" }
        require(player.containsCity(DistrictType.SMITHY)) {
            "You don't have a smithy card"
        }
        require(player.gold >= 2) { "Not enough gold" }
        player.spendGold(2)
        for (i in 1..3) {
            player.addHand(state.selectDistrict())
        }
    }
}

class UseLaboratoryCardAction(val rank: Int, val card: District) : GameAction() {
    override fun execute(state: GameState) {
        val player = state.activePlayer ?: return
        require(player.character == rank) {
            "Players rank ${player.character} not match rank $rank"
        }
        require(card in player.hand) { "You don't have a card $card" }
        player.deleteCardFromHand(card)
        player.addGold(1)
    }
}

class EndTurnAction(val rank: Int) : GameAction() {
    override fun execute(state: GameState) {
        val player = state.activePlayer ?: return
        require(player.character == rank) {
            "Players rank ${player.character} not match rank $rank"
        }
        require(player.hasTakenResources) { "During his turn, the player $player must take resources" }
        player.resetTurnFlags()
    }
}

class EndDraftAction(val player: Player) : GameAction() {
    override fun execute(state: GameState) {
        val activePlayer = state.activePlayer ?: return
        require(player == activePlayer) {
            "The active player does not match the transmitted one"
        }

    }
}

class PassiveTakeGoldAction(val rank: Int) : GameAction() {
    override fun execute(state: GameState) {
        val player = state.activePlayer ?: return
        require(player.character == rank) {
            "Players rank ${player.character} not match rank $rank"
        }
        require(player.hasCollectedIncome) {"Player already collected income"}
        player.passiveTakeGold()
    }
}



