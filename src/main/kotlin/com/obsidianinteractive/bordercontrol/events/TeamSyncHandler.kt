
package com.obsidianinteractive.bordercontrol.events

import com.obsidianinteractive.bordercontrol.integration.ftb.FTBCompat
import com.obsidianinteractive.bordercontrol.region.RegionConfig
import net.minecraftforge.event.TickEvent
import net.minecraftforge.event.server.ServerStoppedEvent
import net.minecraftforge.eventbus.api.SubscribeEvent

object TeamSyncHandler {

    private const val SYNC_INTERVAL = 40

    private var ticks = 0

    @SubscribeEvent
    fun onServerTick(
        event: TickEvent.ServerTickEvent
    ) {

        if (event.phase != TickEvent.Phase.END) {
            return
        }

        if (!FTBCompat.isTeamsLoaded()) return

        if (!RegionConfig.isInitialized()) return

        ticks++

        if (ticks < SYNC_INTERVAL) return

        ticks = 0

        if (event.server.playerList.players.isEmpty()) {
            return
        }

        if (RegionConfig.getRegions().isEmpty()) {
            return
        }

        RegionSyncHandler.syncAll(event.server)
    }

    @SubscribeEvent
    fun onServerStopped(
        event: ServerStoppedEvent
    ) {
        ticks = 0
    }
}