package de.stefanbissell.bots.creepy

import com.github.ocraft.s2client.protocol.data.Abilities
import com.github.ocraft.s2client.protocol.data.Ability
import com.github.ocraft.s2client.protocol.data.UnitType
import com.github.ocraft.s2client.protocol.data.Units
import com.github.ocraft.s2client.protocol.spatial.Point
import com.github.ocraft.s2client.protocol.unit.Unit

open class ZergBot : CommonBot() {

    private val workerTypes = listOf(
        Units.ZERG_DRONE,
        Units.ZERG_DRONE_BURROWED
    )

    private val buildingAbilities = mapOf(
        Units.ZERG_SPAWNING_POOL to Abilities.BUILD_SPAWNING_POOL
    )

    private val trainingAbilities = mapOf(
        Units.ZERG_DRONE to Abilities.TRAIN_DRONE,
        Units.ZERG_OVERLORD to Abilities.TRAIN_OVERLORD,
        Units.ZERG_ZERGLING to Abilities.TRAIN_ZERGLING
    )

    private val workers
        get() = ownUnits
            .filter {
                it.type in workerTypes
            }

    private val idleLarva
        get() = ownUnits
            .filter {
                it.type == Units.ZERG_LARVA &&
                    it.orders.isEmpty()
            }

    fun pendingCount(type: UnitType): Int {
        return trainingAbilities[type]
            ?.let { ability ->
                ownUnits
                    .count { unit ->
                        unit.type == Units.ZERG_EGG &&
                            unit.orders.any {
                                it.ability.abilityId == ability.abilityId
                            }
                    }
                    .let {
                        if (type == Units.ZERG_ZERGLING) it * 2 else it
                    }
            }
            ?: 0
    }

    fun readyCount(type: UnitType): Int {
        return ownUnits.count { it.type == type }
    }

    fun totalCount(type: UnitType): Int {
        return readyCount(type) + pendingCount(type)
    }

    fun trainUnit(type: UnitType) {
        idleLarva
            .randomOrNull()
            ?.also { larva ->
                trainingAbilities[type]
                    ?.takeIf {
                        larva.canCast(it, false)
                    }
                    ?.also {
                        actions()
                            .unitCommand(larva, it, false)
                    }
            }
    }

    fun tryBuildStructure(building: Units, position: Point) {
        if (!canAfford(building)) {
            return
        }
        val ability = buildingAbilities[building] ?: return
        val builder = workers.randomOrNull() ?: return
        actions()
            .unitCommand(
                builder,
                ability,
                position.toPoint2d(),
                false
            )
    }

    private fun canAfford(unitType: UnitType) = canAfford(cost(unitType))

    private fun canAfford(cost: Cost?): Boolean {
        return cost != null &&
            cost.supply <= supplyLeft &&
            cost.minerals <= observation().minerals &&
            cost.vespene <= observation().vespene
    }

    private fun cost(unitType: UnitType) =
        observation().getUnitTypeData(false)[unitType]
            ?.let {
                Cost(
                    supply = it.foodRequired.orElse(0f),
                    minerals = it.mineralCost.orElse(0),
                    vespene = it.vespeneCost.orElse(0)
                )
            }
}

data class Cost(
    val supply: Float,
    val minerals: Int,
    val vespene: Int
)