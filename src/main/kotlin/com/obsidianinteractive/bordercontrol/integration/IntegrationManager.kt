
package com.obsidianinteractive.bordercontrol.integration

import com.mojang.logging.LogUtils
import com.obsidianinteractive.bordercontrol.config.Config
import com.obsidianinteractive.bordercontrol.config.Config.IntegrationMode
import net.minecraftforge.fml.ModList

object IntegrationManager {

    private val LOGGER = LogUtils.getLogger()

    enum class Integration(
        val modId: String,
        val displayName: String,
        val configKey: String? = null
    ) {

        FTB_CHUNKS(
            "ftbchunks",
            "FTB Chunks",
            "ftb-chunks"
        ),

        FTB_TEAMS(
            "ftbteams",
            "FTB Teams",
            "ftb-teams"
        ),

        FTB_RANKS(
            "ftbranks",
            "FTB Ranks",
            "ftb-ranks"
        ),

        FTB_ESSENTIALS(
            "ftbessentials",
            "FTB Essentials",
            "ftb-essentials"
        ),

        LUCKPERMS(
            "luckperms",
            "LuckPerms",
            "luckperms"
        ),

        IMMERSIVE_VEHICLES(
            "mts",
            "Immersive Vehicles",
            "immersive-vehicles"
        ),

        CREATE(
            "create",
            "Create",
            "create"
        ),

        OPAC(
            "openpartiesandclaims",
            "Open Parties and Claims",
            "open-parties-and-claims"
        ),

        XAERO_MINIMAP(
            "xaerominimap",
            "Xaero's Minimap"
        ),

        XAERO_WORLD_MAP(
            "xaeroworldmap",
            "Xaero's World Map"
        )
    }

    @Volatile
    private var detectedIntegrations: Set<Integration> =
        emptySet()

    fun initialize() {

        LOGGER.info(
            "[BorderControl] Detecting optional integrations..."
        )

        detectedIntegrations = Integration.entries
            .filter { ModList.get().isLoaded(it.modId) }
            .toSet()

        logStatus()
    }

    fun isLoaded(integration: Integration): Boolean {
        return integration in detectedIntegrations
    }

    fun getMode(integration: Integration): IntegrationMode {

        // Integrations without config keys are automatic.
        val key = integration.configKey
            ?: return IntegrationMode.AUTO

        return Config.getIntegrationMode(key)
    }

    fun isConfigured(integration: Integration): Boolean {
        return getMode(integration) != IntegrationMode.DISABLED
    }

    fun isEnabled(integration: Integration): Boolean {

        if (!isLoaded(integration)) {
            return false
        }

        return when (getMode(integration)) {

            IntegrationMode.AUTO -> true

            IntegrationMode.ENABLED -> true

            IntegrationMode.DISABLED -> false
        }
    }

    fun getLoadedIntegrations(): Set<Integration> {
        return detectedIntegrations.toSet()
    }

    fun getEnabledIntegrations(): Set<Integration> {

        return detectedIntegrations
            .filter { isEnabled(it) }
            .toSet()
    }

    fun getVersion(integration: Integration): String? {

        return ModList.get()
            .getModContainerById(integration.modId)
            .map { it.modInfo.version.toString() }
            .orElse(null)
    }

    fun logStatus() {

        Integration.entries.forEach { integration ->

            val loaded = isLoaded(integration)
            val mode = getMode(integration)

            val status = when {

                mode == IntegrationMode.DISABLED ->
                    "DISABLED BY CONFIG"

                !loaded && mode == IntegrationMode.ENABLED ->
                    "DISABLED (MOD MISSING)"

                !loaded ->
                    "DISABLED (AUTO - NOT INSTALLED)"

                integration.configKey == null ->
                    "ENABLED (AUTOMATIC - REQUIRED)"

                mode == IntegrationMode.AUTO ->
                    "ENABLED (AUTO)"

                else ->
                    "ENABLED BY CONFIG"
            }

            if (!loaded && mode == IntegrationMode.ENABLED) {

                LOGGER.warn(
                    "[BorderControl] ${integration.displayName}: $status"
                )

            } else {

                LOGGER.info(
                    "[BorderControl] ${integration.displayName}: $status"
                )
            }
        }

        LOGGER.info(
            "[BorderControl] Detected " +
                    "${detectedIntegrations.size}/${Integration.entries.size} " +
                    "optional integrations."
        )

        LOGGER.info(
            "[BorderControl] Enabled " +
                    "${getEnabledIntegrations().size}/${detectedIntegrations.size} " +
                    "installed integrations."
        )
    }
}