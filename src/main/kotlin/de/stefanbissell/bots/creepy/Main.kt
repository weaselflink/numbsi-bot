package de.stefanbissell.bots.creepy

import com.github.ocraft.s2client.bot.S2Coordinator
import com.github.ocraft.s2client.protocol.game.Difficulty
import com.github.ocraft.s2client.protocol.game.LocalMap
import com.github.ocraft.s2client.protocol.game.Race
import kotlin.io.path.Path

fun main(args: Array<String>) {
    val bot = CreepyBot()
    val s2Coordinator = S2Coordinator.setup()
        .setRealtime(false)
        .loadSettings(args)
        .setParticipants(
            S2Coordinator.createParticipant(Race.ZERG, bot),
            S2Coordinator.createComputer(Race.TERRAN, Difficulty.MEDIUM)
        )
        .launchStarcraft()
        .startGame(LocalMap.of(Path("EphemeronLE.SC2Map")))

    while (s2Coordinator.update()) {
        // nothing
    }

    s2Coordinator.quit()
}
