package work.lclpnet.ac.module

import net.fabricmc.fabric.api.networking.v1.PlayerLookup
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerPlayer
import org.slf4j.Logger
import work.lclpnet.ac.SimpleAcInit
import work.lclpnet.kibu.hook.HookContainer
import work.lclpnet.kibu.hook.player.PlayerConnectionHooks
import work.lclpnet.kibu.hook.player.PlayerSwingHandHook
import work.lclpnet.kibu.translate.Translations
import work.lclpnet.kibu.translate.text.FormatWrapper
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.pow
import kotlin.math.sqrt

class AntiAutoClicker(
    val translations: Translations,
    val logger: Logger,
) {

    private val hooks = HookContainer()
    private val clickData = ConcurrentHashMap<UUID, MutableList<Long>>()
    private val flagged = ConcurrentHashMap<UUID, MutableList<Long>>()

    companion object {
        const val MAX_CPS = 25
        const val SAMPLE_SIZE = 30
        const val FLAG_WINDOW_MS = 20_000
        const val MAX_FLAGS = 5
    }

    fun deactivate() {
        hooks.unload()
        clickData.clear()
        flagged.clear()
    }

    fun activate() {
        deactivate()

        hooks.registerHook(PlayerSwingHandHook.HOOK, PlayerSwingHandHook { player, _ ->
            onInteraction(player)
        })

        hooks.registerHook(PlayerConnectionHooks.QUIT, PlayerConnectionHooks.ServerPlayerAction { player ->
            clickData.remove(player.uuid)
            flagged.remove(player.uuid)
        })
    }

    fun onInteraction(player: ServerPlayer) {
        if (!input(player)) return

        // flagged for using an auto clicker
        logger.debug("Flagged {}: auto clicker", player.plainTextName)

        val now = System.currentTimeMillis()
        val flags = flagged.getOrPut(player.uuid) { mutableListOf() }

        flags.add(now)

        if (flags.size <= MAX_FLAGS) return
        if (flags.size > MAX_FLAGS + 1) flags.removeFirst()

        val minTimestampMs = now - FLAG_WINDOW_MS
        val flagsInWindow = flags.count { it >= minTimestampMs }

        if (flagsInWindow <= MAX_FLAGS) return

        logger.info("Player {} was kicked for using an auto clicker", player.plainTextName)

        // suspected auto-click
        player.connection.disconnect(
            translations.translateText("simple-ac.anti-auto-clicker.not_allowed")
                .formatted(ChatFormatting.RED)
                .translateFor(player)
        )

        translations.translateText(
            "simple-ac.anti-auto-clicker.kicked",
            FormatWrapper.styled(player.plainTextName, ChatFormatting.YELLOW)
        ).formatted(ChatFormatting.RED)
            .prefixed(SimpleAcInit.PREFIX)
            .sendTo(PlayerLookup.all(player.level().server))
    }

    private fun input(player: ServerPlayer): Boolean {
        val now = System.currentTimeMillis()
        val timestamps = clickData.getOrPut(player.uuid) { mutableListOf() }

        timestamps.add(now)

        val oneSecondAgo = now - 1000L
        val clicksInLastSecond = timestamps.count { it >= oneSecondAgo }

        if (clicksInLastSecond > MAX_CPS) {
            return true
        }

        if (timestamps.size > SAMPLE_SIZE) timestamps.removeAt(0)
        if (timestamps.size < SAMPLE_SIZE) return false

        val delays = mutableListOf<Long>()

        for (i in 1 until timestamps.size) {
            delays.add(timestamps[i] - timestamps[i - 1])
        }

        // < 10ms: Packets bunched up due to server ticks or network lag
        // > 250ms: Lag spikes or the player simply stopped clicking
        val validDelays = delays.filter { it in 10..250 }

        // If lag is so severe that half our sample was noise, wait for cleaner data
        if (validDelays.size < SAMPLE_SIZE / 2) {
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