package com.obsidianinteractive.bordercontrol.client

import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.client.event.RenderLevelStageEvent
import net.minecraftforge.client.event.ClientPlayerNetworkEvent
import org.apache.logging.log4j.LogManager

object ClientSetup {

    private val LOGGER =
        LogManager.getLogger("BorderControl/ClientSetup")

    private var registered = false

    fun register() {
        if (registered) return

        MinecraftForge.EVENT_BUS.addListener<RenderLevelStageEvent> { event ->
            BorderRenderer.onRender(event)
        }

        MinecraftForge.EVENT_BUS.addListener<ClientPlayerNetworkEvent.LoggingOut> { event ->
            BorderRenderer.onDisconnect(event)
        }

        registered = true

        LOGGER.info("BorderRenderer listeners registered explicitly!")
    }
}