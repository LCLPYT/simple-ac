package work.lclpnet.ac.anti_auto_clicker

import net.minecraft.server.level.ServerPlayer
import org.slf4j.Logger
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.pow
import kotlin.math.sqrt

class AutoClickDetector(
    val maxCps: Int = 20,
    val sampleSize: Int = 30,
    logger: Logger
) {

    init {
        if (sampleSize < maxCps) {
            logger.warn("AutoClickDetector sample size is smaller than maxCps. Max click detections will not work.")
        }
    }

    private val clickData = ConcurrentHashMap<UUID, MutableList<Long>>()

    fun clear() {
        clickData.clear()
    }

    fun clean(player: ServerPlayer) {
        clickData.remove(player.uuid)
    }

    fun input(player: ServerPlayer): Boolean {
        val now = System.currentTimeMillis()
        val timestamps = clickData.getOrPut(player.uuid) { mutableListOf() }

        timestamps.add(now)

        val oneSecondAgo = now - 1000L
        val clicksInLastSecond = timestamps.count { it >= oneSecondAgo }

        if (clicksInLastSecond > maxCps) {
            return true
        }

        if (timestamps.size > sampleSize) timestamps.removeAt(0)
        if (timestamps.size < sampleSize) return false

        val delays = mutableListOf<Long>()

        for (i in 1 until timestamps.size) {
            delays.add(timestamps[i] - timestamps[i - 1])
        }

        // < 10ms: Packets bunched up due to server ticks or network lag
        // > 250ms: Lag spikes or the player simply stopped clicking
        val validDelays = delays.filter { it in 10..250 }

        // If lag is so severe that half our sample was noise, wait for cleaner data
        if (validDelays.size < sampleSize / 2) {
            return false
        }

        val mean = validDelays.average()
        val variance = validDelays.map { (it - mean).pow(2) }.average()
        val stdDev = sqrt(variance)

        val ping = getPlayerLatencyMs(player)

        // Dynamic Threshold: Humans normally have a stdDev > 10ms.
        // We set a strict baseline of 4.0ms. We add +1.0ms leniency for every 50ms of ping.
        // If a high-ping player somehow maintains a rock-solid click rhythm, it's likely a bot.
        val dynamicMinDeviation = 4.0 + (ping / 50.0)

        // Flag if the clicks are unnaturally consistent
        return stdDev < dynamicMinDeviation
    }

    private fun getPlayerLatencyMs(player: ServerPlayer): Long {
        return player.connection.latency().toLong()
    }
}