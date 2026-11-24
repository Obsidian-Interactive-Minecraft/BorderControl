
package com.obsidianinteractive.bordercontrol.events

import com.obsidianinteractive.bordercontrol.blockentity.BorderBlockEntity
import com.obsidianinteractive.bordercontrol.block.ModBlocks
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerPlayer
import net.minecraftforge.event.level.BlockEvent
import net.minecraftforge.eventbus.api.SubscribeEvent
import org.apache.logging.log4j.LogManager

object BorderBreakHandler {

    private val LOGGER =
        LogManager.getLogger("BorderControl/BreakHandler")

    @SubscribeEvent
    fun onBlockBreak(event: BlockEvent.BreakEvent) {
        if (!event.state.`is`(ModBlocks.BORDER_BLOCK.get())) {
            return
        }

        val player = event.player as? ServerPlayer ?: return

        val entity = event.level.getBlockEntity(event.pos)
                as? BorderBlockEntity

        val allowed = player.isCreative ||
                entity?.ownerUUID == player.uuid

        if (!allowed) {
            event.isCanceled = true

            player.sendSystemMessage(
                Component.literal(
                    "You do not own this Border Block!"
                )
            )

            LOGGER.info(
                "Unauthorized Border Block removal prevented"
            )
        }
    }
}