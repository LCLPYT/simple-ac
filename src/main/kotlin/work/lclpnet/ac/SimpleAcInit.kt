package work.lclpnet.ac

import net.fabricmc.api.ModInitializer
import net.minecraft.resources.Identifier
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class SimpleAcInit : ModInitializer {

    override fun onInitialize() {
        LOGGER.info("Initialized.")
    }

    companion object {
        const val MOD_ID = "simple-ac"

        @JvmField
        val LOGGER: Logger = LoggerFactory.getLogger(MOD_ID)

        @JvmStatic
        fun identifier(path: String): Identifier =
            Identifier.fromNamespaceAndPath(MOD_ID, path)
    }
}