package de.stefanbissell.bots.numbsi

import com.github.ocraft.s2client.protocol.data.Abilities
import com.github.ocraft.s2client.protocol.debug.Color
import com.github.ocraft.s2client.protocol.unit.Unit as S2Unit

class WorkerManager(
    private val zergBot: ZergBot,
    private val bases: Bases
) : BotComponent {

    private val prioritizeGas = true
    private var lastRebalance = 0.0

    override fun onStep() {
        sendIdleToWork()
        debugWorkerJobs()
        val seconds = zergBot.gameTime.exactSeconds
        if (seconds - lastRebalance > 1) {
            lastRebalance = seconds
            rebalanceWorkers()
        }
    }

    private fun rebalanceWorkers() {
        val basesWithSurplus = bases.currentBases
            .filter {
                it.isReady &&
                    it.workerCount > it.optimalWorkerCount + 1
            }
        val basesWithNeed = bases.currentBases
            .filter {
                it.isReady &&
                    it.workerCount < it.optimalWorkerCount
            }

        if (basesWithNeed.isNotEmpty() && basesWithSurplus.isNotEmpty()) {
            val worker = basesWithSurplus
                .random()
                .surplusWorker
            if (worker != null) {
                val extractors = basesWithNeed
                    .flatMap {
                        it.underSaturatedExtractors
                    }
                if (extractors.isNotEmpty()) {
                    extractors
                        .closestTo(worker)
                        ?.also {
                            zergBot.actions()
                                .unitCommand(worker, Abilities.HARVEST_GATHER_DRONE, it, false)
                        }
                } else {
                    basesWithNeed
                        .flatMap {
                            it.mineralFields
                        }
                        .closestTo(worker)
                        ?.also {
                            zergBot.actions()
                                .unitCommand(worker, Abilities.HARVEST_GATHER_DRONE, it, false)
                        }
                }
            }
        } else {
            bases.currentBases
                .forEach { base ->
                    val underSaturatedExtractors = base.underSaturatedExtractors
                    val mineralWorkers = base.mineralWorkers
                    if (underSaturatedExtractors.isNotEmpty() && (prioritizeGas || mineralWorkers.size > base.mineralFields.size * 2)) {
                        mineralWorkers
                            .randomOrNull()
                            ?.also { worker ->
                                underSaturatedExtractors
                                    .closestTo(worker)
                                    ?.also {
                                        zergBot.actions()
                                            .unitCommand(worker, Abilities.HARVEST_GATHER_DRONE, it, false)
                                    }

                            }
                    }
                }
        }
    }

    private fun sendIdleToWork() {
        zergBot.workers
            .idle
            .forEach {
                it.backToWork()
            }
    }

    private fun debugWorkerJobs() {
        zergBot.workers
            .forEach {
                when {
                    zergBot.isHarvestingMinerals(it) -> {
                        debugText(it, "minerals")
                    }
                    zergBot.isHarvestingVespene(it) -> {
                        debugText(it, "vespene")
                    }
                    zergBot.isBuilding(it) -> {
                        debugText(it, "building", Color.GREEN)
                    }
                    else -> {
                        debugText(it, "unknown", Color.RED)
                    }
                }
            }
    }

    private fun debugText(
        worker: S2Unit,
        text: String,
        color: Color = Color.WHITE
    ) {
        zergBot.debug()
            .debugTextOut(text, worker.position, color, 12)
    }

    private fun S2Unit.backToWork() {
        val closestMinerals = closestMineralsNearBase() ?: closestMinerals()
        closestMinerals
            ?.also {
                zergBot.actions()
                    .unitCommand(this, Abilities.HARVEST_GATHER_DRONE, it, false)
            }
    }

    private fun S2Unit.closestMinerals() =
        zergBot.mineralFields
            .closestTo(this)

    private fun S2Unit.closestMineralsNearBase() =
        bases.currentBases
            .flatMap {
                it.mineralFields
            }
            .closestTo(this)
}