
package com.obsidianinteractive.bordercontrol.events

import com.obsidianinteractive.bordercontrol.border.BorderAccess
import com.obsidianinteractive.bordercontrol.border.BorderBounds
import com.obsidianinteractive.bordercontrol.border.BorderCollision
import com.obsidianinteractive.bordercontrol.region.RegionConfig
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.phys.Vec3
import net.minecraftforge.event.TickEvent
import net.minecraftforge.event.entity.player.PlayerEvent
import net.minecraftforge.event.server.ServerStoppedEvent
import net.minecraftforge.eventbus.api.SubscribeEvent
import java.util.UUID

object BorderMovementHandler {

    private data class PlayerPosition(
        val dimension: String,
        val position: Vec3
    )

    private val previousPositions =
        HashMap<UUID, PlayerPosition>()

    @SubscribeEvent
    fun onPlayerTick(event: TickEvent.PlayerTickEvent) {

        if (event.phase != TickEvent.Phase.END) return

        val player = event.player as? ServerPlayer
            ?: return

        if (!player.isAlive) {
            previousPositions.remove(player.uuid)
            return
        }

        val dimension = player.serverLevel()
            .dimension().location().toString()

        val current = player.position()

        val previous = previousPositions[player.uuid]

        if (previous == null ||
            previous.dimension != dimension
        ) {
            previousPositions[player.uuid] =
                PlayerPosition(dimension, current)

            return
        }

        val from = previous.position

        // Vertical-only movement cannot cross
        // an X/Z region boundary.
        if (from.x == current.x &&
            from.z == current.z
        ) {
            previousPositions[player.uuid] =
                PlayerPosition(dimension, current)

            return
        }

        for (region in RegionConfig.getRegions()) {

            if (region.first.dimension != dimension) {
                continue
            }

            val bounds = BorderBounds.fromRegion(region)

            if (!BorderCollision.crosses(
                    bounds,
                    from,
                    current
                )
            ) {
                continue
            }

            if (BorderAccess.canCross(region, player)) {
                continue
            }

            // Restore the last accepted position.
            player.connection.teleport(
                from.x,
                from.y,
                from.z,
                player.yRot,
                player.xRot
            )

            player.deltaMovement = Vec3.ZERO
            player.hurtMarked = true

            return
        }

        previousPositions[player.uuid] =
            PlayerPosition(dimension, current)
    }

    @SubscribeEvent
    fun onLogout(event: PlayerEvent.PlayerLoggedOutEvent) {
        previousPositions.remove(event.entity.uuid)
    }

    @SubscribeEvent
    fun onRespawn(event: PlayerEvent.PlayerRespawnEvent) {
        previousPositions.remove(event.entity.uuid)
    }

    @SubscribeEvent
    fun onDimensionChange(
        event: PlayerEvent.PlayerChangedDimensionEvent
    ) {
        previousPositions.remove(event.entity.uuid)
    }

    @SubscribeEvent
    fun onServerStopped(event: ServerStoppedEvent) {
        previousPositions.clear()
    }
}