package de.stefanbissell.bots.creepy

import com.github.ocraft.s2client.protocol.data.Abilities
import com.github.ocraft.s2client.protocol.data.Buffs
import com.github.ocraft.s2client.protocol.data.Units
import com.github.ocraft.s2client.protocol.spatial.Point

class Bases(
    private val zergBot: ZergBot
) : BotComponent {

    val currentBases: MutableList<Base> = mutableListOf()

    override fun onStep() {
        currentBases.removeIf { base ->
            zergBot.observation()
                .units
                .none { it.tag.value == base.buildingId }
        }
        zergBot.baseBuildings
            .filter { building ->
                currentBases
                    .none { it.buildingId == building.tag.value }
            }
            .forEach {
                currentBases += Base(
                    zergBot = zergBot,
                    buildingId = it.tag.value,
                    position = it.position
                )
            }
        tryInjectLarva()
    }

    private fun tryInjectLarva() {
        zergBot.ownUnits
            .ofType(Units.ZERG_QUEEN)
            .idle
            .mapNotNull { queen ->
                zergBot.baseBuildings
                    .firstOrNull { it.position.distance(queen.position) < 9 }
                    ?.let {
                        queen to it
                    }
            }
            .filter { (queen, base) ->
                zergBot.canCast(queen, Abilities.EFFECT_INJECT_LARVA) &&
                    base.buffs.none { it.buffId == Buffs.QUEEN_SPAWN_LARVA_TIMER.buffId }
            }
            .randomOrNull()
            ?.also { (queen, base) ->
                zergBot.actions()
                    .unitCommand(queen, Abilities.EFFECT_INJECT_LARVA, base, false)
            }
    }
}

class Base(
    private val zergBot: ZergBot,
    val buildingId: Long,
    val position: Point
) {

    private val building
        get() = zergBot
            .ownUnits
            .firstOrNull {
                it.tag.value == buildingId
            }

    val mineralFields
        get() = building
            ?.let { b ->
                zergBot
                    .mineralFields
                    .filter {
                        it.position.distance(b.position) < 9f
                    }
            }
            ?: emptyList()

    private val geysers
        get() = building
            ?.let { b ->
                zergBot
                    .vespeneGeysers
                    .filter {
                        it.position.distance(b.position) < 9f
                    }
            }
            ?: emptyList()

    val emptyGeysers
        get() = geysers
            .filter { geyser ->
                zergBot.ownUnits
                    .ofTypes(
                        Units.ZERG_EXTRACTOR,
                        Units.ZERG_EXTRACTOR_RICH
                    )
                    .none {
                        it.position.distance(geyser.position) < 1
                     }
            }
}
