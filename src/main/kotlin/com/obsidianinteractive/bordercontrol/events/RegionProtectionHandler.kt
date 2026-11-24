
package com.obsidianinteractive.bordercontrol.events

import com.obsidianinteractive.bordercontrol.border.BorderAccess
import com.obsidianinteractive.bordercontrol.region.BorderRegion
import com.obsidianinteractive.bordercontrol.region.RegionManager
import net.minecraft.core.BlockPos
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerPlayer
import net.minecraftforge.event.level.BlockEvent
import net.minecraftforge.event.entity.player.PlayerInteractEvent
import net.minecraftforge.eventbus.api.Event
import net.minecraftforge.eventbus.api.EventPriority
import net.minecraftforge.eventbus.api.SubscribeEvent
import org.apache.logging.log4j.LogManager

object RegionProtectionHandler {

    private val LOGGER =
        LogManager.getLogger("BorderControl/Protection")

    private fun getRegion(
        player: ServerPlayer,
        pos: BlockPos
    ): BorderRegion? {

        val dimension = player.serverLevel()
            .dimension().location().toString()

        return RegionManager.getRegionAt(
            pos,
            dimension
        )
    }

    private fun deny(
        player: ServerPlayer,
        region: BorderRegion,
        action: String,
        pos: BlockPos
    ) {
        player.displayClientMessage(
            Component.literal(
                "You cannot $action in this region!"
            ),
            true
        )

        LOGGER.debug(
            "{} denied {} at {} in region {}/{}",
            player.gameProfile.name,
            action,
            pos,
            region.owner,
            region.id
        )
    }

    // Block breaking
    @SubscribeEvent(priority = EventPriority.LOWEST)
    fun onBlockBreak(event: BlockEvent.BreakEvent) {

        if (event.isCanceled) return

        val player = event.player as? ServerPlayer
            ?: return

        val region = getRegion(player, event.pos)
            ?: return

        if (!BorderAccess.canBreak(
                region,
                player,
                event.pos
            )
        ) {
            event.isCanceled = true

            deny(
                player,
                region,
                "break blocks",
                event.pos
            )
        }
    }

    // Block placement, including multi-block placement
    @SubscribeEvent(priority = EventPriority.LOWEST)
    fun onBlockPlace(event: BlockEvent.EntityPlaceEvent) {

        if (event.isCanceled) return

        val player = event.entity as? ServerPlayer
            ?: return

        val positions = if (
            event is BlockEvent.EntityMultiPlaceEvent
        ) {
            event.replacedBlockSnapshots.map { it.pos }
        } else {
            listOf(event.pos)
        }

        for (pos in positions) {

            val region = getRegion(player, pos)
                ?: continue

            if (!BorderAccess.canBuild(
                    region,
                    player,
                    pos
                )
            ) {
                event.isCanceled = true

                deny(
                    player,
                    region,
                    "place blocks",
                    pos
                )

                return
            }
        }
    }

    // Chests, doors, levers, buttons, machines, etc.
    @SubscribeEvent(priority = EventPriority.LOWEST)
    fun onBlockInteract(
        event: PlayerInteractEvent.RightClickBlock
    ) {
        if (event.isCanceled) return

        // Respect existing protection from other mods.
        if (event.useBlock == Event.Result.DENY) return

        val player = event.entity as? ServerPlayer
            ?: return

        val region = getRegion(player, event.pos)
            ?: return

        if (!BorderAccess.canInteract(
                region,
                player,
                event.pos,
                event.hand
            )
        ) {
            // Deny block interaction without automatically
            // denying item placement.
            event.setUseBlock(Event.Result.DENY)

            deny(
                player,
                region,
                "interact with blocks",
                event.pos
            )
        }
    }

    // Interacting with entities
    @SubscribeEvent(priority = EventPriority.LOWEST)
    fun onEntityInteract(
        event: PlayerInteractEvent.EntityInteract
    ) {
        if (event.isCanceled) return

        val player = event.entity as? ServerPlayer
            ?: return

        val pos = event.target.blockPosition()

        val region = getRegion(player, pos)
            ?: return

        if (!BorderAccess.canInteractEntity(
                region,
                player,
                event.target,
                event.hand
            )
        ) {
            event.isCanceled = true

            deny(
                player,
                region,
                "interact with entities",
                pos
            )
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    fun onEntityInteractSpecific(
        event: PlayerInteractEvent.EntityInteractSpecific
    ) {
        if (event.isCanceled) return

        val player = event.entity as? ServerPlayer
            ?: return

        val pos = event.target.blockPosition()

        val region = getRegion(player, pos)
            ?: return

        if (!BorderAccess.canInteractEntity(
                region,
                player,
                event.target,
                event.hand
            )
        ) {
            event.isCanceled = true

            deny(
                player,
                region,
                "interact with entities",
                pos
            )
        }
    }
}