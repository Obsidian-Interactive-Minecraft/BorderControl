
package com.obsidianinteractive.bordercontrol.events

import com.obsidianinteractive.bordercontrol.network.BorderNetwork
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerPlayer
import net.minecraftforge.event.entity.player.PlayerEvent
import net.minecraftforge.eventbus.api.SubscribeEvent
import org.apache.logging.log4j.LogManager

object RegionSyncHandler {

    private val LOGGER =
        LogManager.getLogger("BorderControl/RegionSync")

    @SubscribeEvent
    fun onPlayerJoin(
        event: PlayerEvent.PlayerLoggedInEvent
    ) {
        syncPlayer(event.entity as? ServerPlayer ?: return)
    }

    @SubscribeEvent
    fun onDimensionChange(
        event: PlayerEvent.PlayerChangedDimensionEvent
    ) {
        syncPlayer(event.entity as? ServerPlayer ?: return)
    }

    @SubscribeEvent
    fun onRespawn(
        event: PlayerEvent.PlayerRespawnEvent
    ) {
        syncPlayer(event.entity as? ServerPlayer ?: return)
    }

    fun syncPlayer(player: ServerPlayer) {
        BorderNetwork.sync(player)

        LOGGER.info(
            "Synchronized regions for {}",
            player.gameProfile.name
        )
    }

    fun syncAll(server: MinecraftServer) {
        val players = server.playerList.players

        for (player in players) {
            BorderNetwork.sync(player)
        }

        LOGGER.info(
            "Synchronized regions for {} players",
            players.size
        )
    }
}