package work.lclpnet.ac.anti_auto_clicker

import net.fabricmc.fabric.api.event.player.AttackEntityCallback
import net.fabricmc.fabric.api.networking.v1.PlayerLookup
import net.minecraft.ChatFormatting
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.InteractionResult
import org.slf4j.Logger
import work.lclpnet.ac.SimpleAcInit
import work.lclpnet.kibu.hook.HookContainer
import work.lclpnet.kibu.hook.entity.PlayerInteractionHooks
import work.lclpnet.kibu.hook.player.PlayerConnectionHooks
import work.lclpnet.kibu.translate.Translations
import work.lclpnet.kibu.translate.text.FormatWrapper
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.pow
import kotlin.math.sqrt

class AntiAutoClicker(
    val translations: Translations,
    val logger: Logger,
    val flagWindowMs: Int = 20_000,
    val maxFlags: Int = 5,
) {

    private val hooks = HookContainer()
    private val detector = AutoClickDetector(logger = logger)
    private val flagged = ConcurrentHashMap<UUID, MutableList<Long>>()

    fun deactivate() {
        hooks.unload()
        detector.clear()
        flagged.clear()
    }

    fun activate() {
        deactivate()

        hooks.registerHook(PlayerInteractionHooks.ATTACK_ENTITY, AttackEntityCallback { player, _, _, _, _ ->
            if (player is ServerPlayer) {
                onInteraction(player)
            }

            InteractionResult.PASS
        })

        hooks.registerHook(PlayerConnectionHooks.QUIT, PlayerConnectionHooks.ServerPlayerAction { player ->
            detector.clean(player)
            flagged.remove(player.uuid)
        })
    }

    fun onInteraction(player: ServerPlayer) {
        if (!detector.input(player)) return

        // flagged for using an auto clicker
        logger.debug("Flagged {}: auto clicker", player.plainTextName)

        val now = System.currentTimeMillis()
        val flags = flagged.getOrPut(player.uuid) { mutableListOf() }

        flags.add(now)

        if (flags.size <= maxFlags) return
        if (flags.size > maxFlags + 1) flags.removeFirst()

        val minTimestampMs = now - flagWindowMs
        val flagsInWindow = flags.count { it >= minTimestampMs }

        if (flagsInWindow <= maxFlags) return

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

}