
package com.obsidianinteractive.bordercontrol.events

import com.obsidianinteractive.bordercontrol.region.RegionConfig
import net.minecraftforge.event.server.ServerAboutToStartEvent
import net.minecraftforge.event.server.ServerStoppedEvent
import net.minecraftforge.eventbus.api.SubscribeEvent
import org.apache.logging.log4j.LogManager

object RegionLifecycleHandler {

    private val LOGGER =
        LogManager.getLogger(
            "BorderControl/Lifecycle"
        )

    @SubscribeEvent
    fun onServerStarting(
        event: ServerAboutToStartEvent
    ) {

        val regions = RegionConfig.initialize(
            event.server
        )

        LOGGER.info(
            "Loaded {} regions for the active world",
            regions.size
        )
    }

    @SubscribeEvent
    fun onServerStopped(
        event: ServerStoppedEvent
    ) {

        RegionConfig.shutdown()

        LOGGER.info(
            "Cleared active world regions"
        )
    }
}