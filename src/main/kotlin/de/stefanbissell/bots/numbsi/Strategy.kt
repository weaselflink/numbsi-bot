package de.stefanbissell.bots.numbsi

import com.github.ocraft.s2client.protocol.data.UnitType
import com.github.ocraft.s2client.protocol.data.Units
import com.github.ocraft.s2client.protocol.data.Upgrade
import com.github.ocraft.s2client.protocol.data.Upgrades
import kotlin.math.ceil
import kotlin.math.max

class Strategy(
    private val gameMap: GameMap,
    private val buildOrder: BuildOrder,
    private val upgradeTacker: UpgradeTacker
) : BotComponent(11) {

    private val priorities = listOf(
        Units.ZERG_LAIR,
        Upgrades.ZERG_MELEE_WEAPONS_LEVEL1,
        Units.ZERG_SPIRE,
        Upgrades.ZERG_FLYER_ARMORS_LEVEL1,
        Upgrades.ZERG_GROUND_ARMORS_LEVEL2,
        Upgrades.ZERG_FLYER_WEAPONS_LEVEL1,
        Upgrades.ZERG_MELEE_WEAPONS_LEVEL2,
        Units.ZERG_INFESTATION_PIT,
        Units.ZERG_HIVE,
        Upgrades.ZERGLING_ATTACK_SPEED,
        Upgrades.ZERG_GROUND_ARMORS_LEVEL3,
        Upgrades.ZERG_MELEE_WEAPONS_LEVEL3,
        Units.ZERG_GREATER_SPIRE,
        Upgrades.ZERG_FLYER_ARMORS_LEVEL2,
        Upgrades.ZERG_FLYER_WEAPONS_LEVEL2,
    )

    override fun onStep(zergBot: ZergBot) {
        if (buildOrder.finished) {
            expandWhenReady(zergBot)
            droneUp(zergBot)
            buildExtractors(zergBot)
            buildQueens(zergBot)
            ensurePriorities(zergBot)
            trainTroops(zergBot)
            keepSupplied(zergBot)
        }
    }

    private fun droneUp(zergBot: ZergBot) {
        if (zergBot.totalCount(Units.ZERG_DRONE) >= 80) {
            return
        }
        if (zergBot.bases.any { it.workersNeeded > 0 }) {
            if (zergBot.canAfford(Units.ZERG_DRONE, 2)) {
                zergBot.trainUnit(Units.ZERG_DRONE)
            }
        }
    }

    private fun buildExtractors(zergBot: ZergBot) {
        if (zergBot.observation().vespene < 200 &&
            zergBot.pendingCount(Units.ZERG_EXTRACTOR) < 1 &&
            zergBot.bases.isNotEmpty() &&
            zergBot.bases.maxOf { it.workersNeeded } < 4
        ) {
            zergBot.tryBuildStructure(gameMap, Units.ZERG_EXTRACTOR)
        }
    }

    private fun buildQueens(zergBot: ZergBot) {
        if (zergBot.baseBuildings.ready.count() >= zergBot.totalCount(Units.ZERG_QUEEN)) {
            zergBot.trainUnit(Units.ZERG_QUEEN)
        }
    }

    private fun trainTroops(zergBot: ZergBot) {
        if (hasReserves(zergBot)) {
            if (zergBot.canAfford(Units.ZERG_MUTALISK)) {
                zergBot.trainUnit(Units.ZERG_MUTALISK)
            }
            if (zergBot.canAfford(Units.ZERG_ZERGLING, 4)) {
                zergBot.trainUnit(Units.ZERG_ZERGLING)
                zergBot.trainUnit(Units.ZERG_ZERGLING)
            }
        }
    }

    private fun keepSupplied(zergBot: ZergBot) {
        if (needSupply(zergBot)) {
            val targetOverlordCount = ceil((zergBot.supplyCap * 0.2) / 8).toInt()
            if (zergBot.pendingCount(Units.ZERG_OVERLORD) < targetOverlordCount) {
                zergBot.trainUnit(Units.ZERG_OVERLORD)
            }
        }
    }

    private fun needSupply(zergBot: ZergBot): Boolean {
        if (zergBot.supplyCap >= 200) {
            return false
        }
        return zergBot.supplyLeft < (zergBot.supplyCap / 5)
    }

    private fun expandWhenReady(zergBot: ZergBot) {
        if (((zergBot.minerals > 400 && !expansionInProgress(zergBot)) || zergBot.minerals > 800) &&
            goodSaturation(zergBot)
        ) {
            expand(zergBot)
        }
    }

    private fun goodSaturation(zergBot: ZergBot) =
        zergBot.bases
            .sumOf { it.workersNeeded } < max(8, zergBot.gameTime.fullMinutes)

    private fun expansionInProgress(zergBot: ZergBot) =
        zergBot.pendingCount(Units.ZERG_HATCHERY) > 0 ||
            zergBot.baseBuildings.inProgress.isNotEmpty()

    private fun expand(zergBot: ZergBot) {
        zergBot.tryBuildStructure(gameMap, Units.ZERG_HATCHERY)
    }

    private fun ensurePriorities(zergBot: ZergBot) {
        priorities
            .forEach {
                ensure(zergBot, it)
            }
    }

    private fun ensure(zergBot: ZergBot, what: Any) {
        if (what is UnitType) {
            ensureBuilding(zergBot, what)
        }
        if (what is Upgrade) {
            ensureUpgrade(zergBot, what)
        }
    }

    private fun ensureBuilding(zergBot: ZergBot, unitType: UnitType) {
        if (zergBot.totalCount(unitType) < 1) {
            zergBot.tryBuildStructure(gameMap, unitType)
        }
    }

    private fun ensureUpgrade(zergBot: ZergBot, upgrade: Upgrade) {
        if (!upgradeTacker.isCompletedOrPending(zergBot, upgrade)) {
            zergBot.tryResearchUpgrade(upgrade)
        }
    }

    private fun hasReserves(zergBot: ZergBot) =
        zergBot.gameTime.fullMinutes < 8 ||
            (zergBot.gameTime.fullMinutes < 12 && zergBot.minerals > 300 && zergBot.vespene > 200) ||
            (zergBot.minerals > 400 && zergBot.vespene > 300)
}
