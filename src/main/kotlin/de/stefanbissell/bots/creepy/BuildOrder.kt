package de.stefanbissell.bots.creepy

import com.github.ocraft.s2client.protocol.data.UnitType
import com.github.ocraft.s2client.protocol.data.Units
import com.github.ocraft.s2client.protocol.spatial.Point
import kotlin.random.Random

class BuildOrder(
    val zergBot: ZergBot,
    private val gameMap: GameMap,
    private val bases: Bases
) : BotComponent {

    private val order = listOf(
        DroneUp(14),
        TrainUnit(Units.ZERG_OVERLORD, 2),
        BuildStructure(Units.ZERG_SPAWNING_POOL),
        DroneUp(16),
        TrainUnit(Units.ZERG_OVERLORD, 3),
        BuildStructure(Units.ZERG_EXTRACTOR),
        TrainUnit(Units.ZERG_QUEEN, 1),
        DroneUp(20),
        BuildStructure(Units.ZERG_HATCHERY, 2),
        KeepTraining(Units.ZERG_ZERGLING),
        KeepSupplied()
    )

    override fun onStep() {
        order
            .firstOrNull {
                !it.tryExecute(this)
            }
    }

    fun tryBuildStructure(type: UnitType) =
        when (type) {
            Units.ZERG_EXTRACTOR -> {
                bases
                    .currentBases
                    .flatMap {
                        it.geysers
                    }
                    .randomOrNull()
                    ?.also {
                        zergBot.tryBuildStructure(type, it)
                    }
            }
            Units.ZERG_HATCHERY -> {
                gameMap
                    .expansions
                    .filter { expansion ->
                        zergBot.baseBuildings.none { it.position.distance(expansion) < 4 }
                    }
                    .minByOrNull {
                        it.toPoint2d().distance(gameMap.ownStart)
                    }
                    ?.also {
                        zergBot.tryBuildStructure(type, it)
                    }
            }
            else -> {
                bases
                    .currentBases
                    .firstOrNull()
                    ?.position
                    ?.towards(gameMap.center, 6f)
                    ?.add(Point.of(getRandomScalar(), getRandomScalar()).mul(4.0f))
                    ?.let {
                        gameMap.clampToMap(it)
                    }
                    ?.also {
                        zergBot.tryBuildStructure(type, it)
                    }
            }
        }

    private fun getRandomScalar(): Float {
        return Random.nextFloat() * 2 - 1
    }
}

sealed class BuildOrderStep {

    abstract fun tryExecute(buildOrder: BuildOrder): Boolean
}

data class DroneUp(val needed: Int) : BuildOrderStep() {
    override fun tryExecute(buildOrder: BuildOrder): Boolean {
        val count = buildOrder.zergBot.totalCount(Units.ZERG_DRONE)
        if (count < needed) {
            buildOrder.zergBot.trainUnit(Units.ZERG_DRONE)
            return false
        }
        return true
    }
}

data class TrainUnit(
    val type: UnitType,
    val needed: Int
) : BuildOrderStep() {
    override fun tryExecute(buildOrder: BuildOrder): Boolean {
        val count = buildOrder.zergBot.totalCount(type)
        if (count < needed) {
            buildOrder.zergBot.trainUnit(type)
            return false
        }
        return true
    }
}

data class BuildStructure(
    val type: UnitType,
    val needed: Int = 1
) : BuildOrderStep() {
    override fun tryExecute(buildOrder: BuildOrder): Boolean {
        val count = buildOrder.zergBot.totalCount(type)
        if (count < needed) {
            buildOrder.tryBuildStructure(type)
            return false
        }
        return true
    }
}

data class KeepTraining(
    val type: UnitType
) : BuildOrderStep() {
    override fun tryExecute(buildOrder: BuildOrder): Boolean {
        buildOrder.zergBot.trainUnit(type)
        return true
    }
}

data class KeepSupplied(val minOverlords: Int = 3) : BuildOrderStep() {
    override fun tryExecute(buildOrder: BuildOrder): Boolean {
        if (buildOrder.zergBot.totalCount(Units.ZERG_OVERLORD) >= minOverlords &&
            buildOrder.zergBot.supplyLeft < 4 &&
            buildOrder.zergBot.pendingCount(Units.ZERG_OVERLORD) == 0
        ) {
            buildOrder.zergBot.trainUnit(Units.ZERG_OVERLORD)
        }
        return true
    }
}
