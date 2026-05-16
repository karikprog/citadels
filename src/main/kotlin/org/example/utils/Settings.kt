package org.example.utils

import org.example.engine.Architect
import org.example.engine.Assassin
import org.example.engine.Bishop
import org.example.engine.Color
import org.example.engine.District
import org.example.engine.DistrictType
import org.example.engine.GameCharacter
import org.example.engine.King
import org.example.engine.Magician
import org.example.engine.Merchant
import org.example.engine.SpecialDistrict
import org.example.engine.StandardDistrict
import org.example.engine.Thief
import org.example.engine.Warlord


class Settings {
    val playerCount = 6
    val districtCardCount = 65

    fun generateCharacters(): MutableList<GameCharacter> {
        val characters = mutableListOf<GameCharacter>()
            characters.add(Assassin())
            characters.add(Thief())
            characters.add(Magician())
            characters.add(King())
            characters.add(Warlord())
            characters.add(Bishop())
            characters.add(Architect())
            characters.add(Merchant())
        characters.shuffle()
        return characters
    }

    fun generateCitadelsDeck(): List<District> {
        val deck = mutableListOf<District>()

        fun addStandard(count: Int, type: DistrictType, name: String, cost: Int, color: Color) {
            repeat(count) {
                deck.add(StandardDistrict(type, name, cost, color))
            }
        }

        addStandard(3, DistrictType.TEMPLE, "Храм", 1, Color.BLUE)
        addStandard(3, DistrictType.CHURCH, "Церковь", 2, Color.BLUE)
        addStandard(3, DistrictType.MONASTERY, "Монастырь", 3, Color.BLUE)
        addStandard(2, DistrictType.CATHEDRAL, "Собор", 5, Color.BLUE)

        addStandard(5, DistrictType.MANOR, "Поместье", 3, Color.YELLOW)
        addStandard(4, DistrictType.CASTLE, "Замок", 4, Color.YELLOW)
        addStandard(3, DistrictType.PALACE, "Палаццо", 5, Color.YELLOW)

        addStandard(3, DistrictType.WATCHTOWER, "Дозорная Башня", 1, Color.RED)
        addStandard(3, DistrictType.PRISON, "Тюрьма", 2, Color.RED)
        addStandard(3, DistrictType.BATTLEGROUND, "Марсово Поле", 3, Color.RED)
        addStandard(2, DistrictType.FORTRESS, "Крепость", 5, Color.RED)

        addStandard(5, DistrictType.TAVERN, "Таверна", 1, Color.GREEN)
        addStandard(4, DistrictType.MARKET, "Рынок", 2, Color.GREEN)
        addStandard(3, DistrictType.TRADING_POST, "Лавка", 2, Color.GREEN)
        addStandard(3, DistrictType.DOCKS, "Порт", 3, Color.GREEN)
        addStandard(3, DistrictType.HARBOR, "Гавань", 4, Color.GREEN)
        addStandard(2, DistrictType.TOWN_HALL, "Ратуша", 5, Color.GREEN)

        deck.add(SpecialDistrict(
            DistrictType.HAUNTED_QUARTER, "Город Призраков", 2, Color.LILAC,
            "Считается кварталом любого цвета при подсчёте очков (кроме последнего тура)."
        ))
        repeat(2) {
            deck.add(SpecialDistrict(
                DistrictType.KEEP, "Форт", 3, Color.LILAC,
                "Кондотьер не может уничтожить Форт."
            ))
        }
        deck.add(SpecialDistrict(
            DistrictType.SMITHY, "Кузня", 5, Color.LILAC,
            "Раз в ход можно заплатить 2 золотых, чтобы вытянуть 3 карты."
        ))
        //Будет в дальнейшем реализована
        //deck.add(SpecialDistrict(
        //    DistrictType.GRAVEYARD, "Кладбище", 5, Color.LILAC,
        //    "При разрушении квартала можно заплатить 1 золотой, чтобы забрать его в руку."
        //))
        deck.add(SpecialDistrict(
            DistrictType.OBSERVATORY, "Обсерватория", 5, Color.LILAC,
            "При взятии карт: тяни 3, оставь 1, остальные под низ колоды."
        ))
        deck.add(SpecialDistrict(
            DistrictType.LABORATORY, "Лаборатория", 5, Color.LILAC,
            "Раз в ход можно сбросить карту с руки и получить 1 золотой."
        ))
        deck.add(SpecialDistrict(
            DistrictType.LIBRARY, "Библиотека", 6, Color.LILAC,
            "При взятии карт оставляй обе вытянутые карты на руке."
        ))
        deck.add(SpecialDistrict(
            DistrictType.SCHOOL_OF_MAGIC, "Школа Магии", 6, Color.LILAC,
            "При расчёте дохода считается кварталом любого цвета (на выбор)."
        ))
        deck.add(SpecialDistrict(
            DistrictType.GREAT_WALL, "Великая Стена", 6, Color.LILAC,
            "Кондотьер платит на 1 золотой больше за разрушение твоих кварталов."
        ))
        deck.add(SpecialDistrict(
            DistrictType.DRAGON_GATE, "Врата Дракона", 6, Color.LILAC,
            "Стоит 6 золотых, приносит 8 очков в конце игры."
        ))
        deck.add(SpecialDistrict(
            DistrictType.UNIVERSITY, "Университет", 6, Color.LILAC,
            "Стоит 6 золотых, приносит 8 очков в конце игры."
        ))
        deck.shuffle()
        return deck
    }
}