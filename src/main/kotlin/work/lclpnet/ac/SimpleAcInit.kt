package work.lclpnet.ac

import net.fabricmc.api.ModInitializer
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.MutableComponent
import net.minecraft.resources.Identifier
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import work.lclpnet.ac.anti_auto_clicker.AntiAutoClicker
import work.lclpnet.kibu.translate.util.ModTranslations

class SimpleAcInit : ModInitializer {

    override fun onInitialize() {
        val translations = ModTranslations.fromAssets(MOD_ID, LOGGER).translations

        AntiAutoClicker(translations, LOGGER).activate()

        LOGGER.info("Initialized.")
    }

    companion object {
        const val MOD_ID = "simple-ac"

        @JvmField
        val LOGGER: Logger = LoggerFactory.getLogger(MOD_ID)

        val PREFIX: MutableComponent = Component.literal("Anti-Cheat> ").withStyle(ChatFormatting.BLUE)

        @JvmStatic
        fun identifier(path: String): Identifier =
            Identifier.fromNamespaceAndPath(MOD_ID, path)
    }
}