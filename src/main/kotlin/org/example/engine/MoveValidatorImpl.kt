package org.example.engine


class ClassicMoveValidator : MoveValidator {

    override fun canExecute(action: GameAction, state: GameState): ValidationResult {
        val player = state.activePlayer ?: return ValidationResult.Invalid("Нет активного игрока")

        if (state.gamePhase is DraftPhase) {
            return ValidationResult.Invalid("Сейчас фаза выбора персонажей (Draft Phase)")
        }

        val actionRank = getActionRank(action)
        if (actionRank != null && player.character != actionRank) {
            return ValidationResult.Invalid("Ранг действия ($actionRank) не совпадает с вашим рангом (${player.character})")
        }

        if (player.isAssassinated) {
            return ValidationResult.Invalid("Вы были убиты Ассасином и пропускаете этот ход")
        }

        return when (action) {
            is CollectGoldAction -> canCollectResources(player)
            is DrowCardAction -> canCollectResources(player)

            is CollectCardAction -> canKeepCard(player, action.cardInx)

            is BuildDistrictAction -> canBuildDistrict(player, action.districtType)

            is UseAssassinAction -> canAssassinate(player, action.victimRank, state)
            is UseThiefAction -> canStealFrom(player, action.robbedRank, state)
            is UseWarlordAction -> canDestroyDistrict(player, action.rankVictim, action.card, state)

            is UseSwapOtherPlayerMagicianAction -> canSwapHandsWithPlayer(player, action.otherName, state)
            is UseSwapDeckMagicianAction -> canSwapCardsWithDeck(player, action.cards)

            is UseArchitectBuildAction -> canBuildMultiple(player, action.cards)
            is UseSmithyCardAction -> canUseSmithy(player)
            is UseLaboratoryCardAction -> canUseLaboratory(player, action.card)

            else -> ValidationResult.Valid
        }
    }


    private fun canCollectResources(player: Player): ValidationResult {
        if (player.hasTakenResources) {
            return ValidationResult.Invalid("Вы уже взяли ресурсы в этом ходу")
        }
        return ValidationResult.Valid
    }

    private fun canKeepCard(player: Player, cardIndex: Int): ValidationResult {
        if (player.temporaryHand.isEmpty()) {
            return ValidationResult.Invalid("У вас нет карт для выбора. Сначала нужно потянуть карты из колоды")
        }
        if (cardIndex !in player.temporaryHand.indices) {
            return ValidationResult.Invalid("Некорректный индекс карты: $cardIndex")
        }
        return ValidationResult.Valid
    }

    private fun canBuildDistrict(player: Player, districtType: DistrictType): ValidationResult {
        if (!player.hasTakenResources) {
            return ValidationResult.Invalid("Сначала нужно взять ресурсы (золото или карты)")
        }

        if (player.hasBuildThisTurn && player.character != 7) {
            return ValidationResult.Invalid("Вы уже построили здание в этом ходу")
        }

        val cardToBuild = player.hand.find { it.type == districtType }
            ?: return ValidationResult.Invalid("У вас в руке нет карты этого квартала")

        if (player.city.any { it.type == districtType }) {
            return ValidationResult.Invalid("В вашем городе уже есть квартал: ${cardToBuild.name}")
        }

        if (player.gold < cardToBuild.cost) {
            return ValidationResult.Invalid("Недостаточно золота (нужно ${cardToBuild.cost}, у вас ${player.gold})")
        }

        return ValidationResult.Valid
    }

    private fun canAssassinate(player: Player, victimRank: Int, state: GameState): ValidationResult {
        if (victimRank == 1) return ValidationResult.Invalid("Ассасин не может убить самого себя")
        if (victimRank !in 2..8) return ValidationResult.Invalid("Некорректный ранг для убийства")
        return ValidationResult.Valid
    }

    private fun canStealFrom(player: Player, robbedRank: Int, state: GameState): ValidationResult {
        if (robbedRank == 1 || robbedRank == 2) {
            return ValidationResult.Invalid("Нельзя воровать у Ассасина или Вора")
        }
        if (robbedRank == player.character) {
            return ValidationResult.Invalid("Нельзя воровать у самого себя")
        }

        val victim = state.players.find { it.character == robbedRank }
        if (victim?.isAssassinated == true) {
            return ValidationResult.Invalid("Нельзя воровать у убитого персонажа")
        }
        return ValidationResult.Valid
    }

    private fun canDestroyDistrict(
        player: Player,
        victimRank: Int,
        card: District,
        state: GameState
    ): ValidationResult {
        val victimPlayer = state.players.find { it.character == victimRank }
            ?: return ValidationResult.Invalid("Игрок не найден")

        if (victimRank == 5 && !victimPlayer.isAssassinated) {
            return ValidationResult.Invalid("Нельзя разрушать кварталы Епископа")
        }

        if (victimPlayer.city.size >= 7) {
            return ValidationResult.Invalid("Нельзя разрушать здания в завершенном городе (7+ кварталов)")
        }

        if (card.type == DistrictType.KEEP) {
            return ValidationResult.Invalid("Форт (Keep) невозможно разрушить")
        }

        if (card !in victimPlayer.city) {
            return ValidationResult.Invalid("Этого квартала нет в городе выбранного игрока")
        }

        val hasGreatWall = victimPlayer.city.any { it.type == DistrictType.GREAT_WALL }
        val destroyCost = if (hasGreatWall) card.cost else card.cost - 1

        if (player.gold < destroyCost) {
            return ValidationResult.Invalid("Недостаточно золота для разрушения (нужно $destroyCost)")
        }

        return ValidationResult.Valid
    }

    private fun canBuildMultiple(player: Player, cards: List<District>): ValidationResult {
        if (cards.size > 3) return ValidationResult.Invalid("Зодчий может построить максимум 3 здания")

        for (card in cards) {
            val buildCheck = canBuildDistrict(player, card.type)
            if (buildCheck is ValidationResult.Invalid) return buildCheck
        }
        return ValidationResult.Valid
    }

    private fun canUseSmithy(player: Player): ValidationResult {
        if (!player.city.any { it.type == DistrictType.SMITHY }) {
            return ValidationResult.Invalid("У вас в городе нет Кузни")
        }
        if (player.gold < 2) {
            return ValidationResult.Invalid("Нужно 2 золотых для активации Кузни")
        }
        return ValidationResult.Valid
    }

    private fun canUseLaboratory(player: Player, card: District): ValidationResult {
        if (!player.city.any { it.type == DistrictType.LABORATORY }) {
            return ValidationResult.Invalid("У вас в городе нет Лаборатории")
        }
        if (card !in player.hand) {
            return ValidationResult.Invalid("Этой карты нет у вас в руке")
        }
        return ValidationResult.Valid
    }

    private fun canSwapHandsWithPlayer(player: Player, otherName: String, state: GameState): ValidationResult {
        val other = state.players.find { it.name == otherName }
        if (other == null) return ValidationResult.Invalid("Игрок с именем $otherName не найден")
        if (other == player) return ValidationResult.Invalid("Нельзя меняться картами с самим собой")
        return ValidationResult.Valid
    }

    private fun canSwapCardsWithDeck(player: Player, cards: List<District>): ValidationResult {
        if (cards.isEmpty()) return ValidationResult.Invalid("Нужно выбрать хотя бы одну карту для обмена")
        for (card in cards) {
            if (card !in player.hand) return ValidationResult.Invalid("Карты ${card.name} нет в руке")
        }
        return ValidationResult.Valid
    }


    private fun getActionRank(action: GameAction): Int? {
        return when (action) {
            is CollectGoldAction -> action.rank
            is DrowCardAction -> action.rank
            is CollectCardAction -> action.rank
            is BuildDistrictAction -> action.rank
            is UseWarlordAction -> action.rank
            is UseSmithyCardAction -> action.rank
            is UseLaboratoryCardAction -> action.rank
            is UseArchitectBuildAction -> action.rank
            is UseAssassinAction -> 1
            is UseThiefAction -> 2
            is UseSwapOtherPlayerMagicianAction -> 3
            is UseSwapDeckMagicianAction -> 3
            else -> null
        }
    }
}
