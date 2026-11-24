
package com.obsidianinteractive.bordercontrol.block

import com.obsidianinteractive.bordercontrol.blockentity.BorderBlockEntity
import com.obsidianinteractive.bordercontrol.events.BorderChatHandler
import net.minecraft.core.BlockPos
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.EntityBlock
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockBehaviour
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.BlockHitResult
import com.obsidianinteractive.bordercontrol.region.RegionManager
import com.obsidianinteractive.bordercontrol.events.RegionSyncHandler
import net.minecraft.server.level.ServerLevel
import com.obsidianinteractive.bordercontrol.network.BorderNetwork

class BorderBlock(
    properties: BlockBehaviour.Properties
) : Block(properties), EntityBlock {

    override fun newBlockEntity(
        pos: BlockPos,
        state: BlockState
    ): BlockEntity {
        return BorderBlockEntity(pos, state)
    }

    override fun setPlacedBy(
        level: Level,
        pos: BlockPos,
        state: BlockState,
        placer: LivingEntity?,
        stack: ItemStack
    ) {
        super.setPlacedBy(level, pos, state, placer, stack)

        if (!level.isClientSide && placer is ServerPlayer) {
            val entity = level.getBlockEntity(pos)
                    as? BorderBlockEntity ?: return

            entity.setOwner(placer.uuid)
        }
    }

    override fun use(
        state: BlockState,
        level: Level,
        pos: BlockPos,
        player: Player,
        hand: InteractionHand,
        hit: BlockHitResult
    ): InteractionResult {

        if (level.isClientSide) {
            return InteractionResult.SUCCESS
        }

        if (player !is ServerPlayer) {
            return InteractionResult.PASS
        }

        val entity = level.getBlockEntity(pos)
                as? BorderBlockEntity
            ?: return InteractionResult.FAIL

        if (entity.ownerUUID != player.uuid) {
            player.sendSystemMessage(
                Component.literal(
                    "You do not own this Border Block!"
                )
            )

            return InteractionResult.FAIL
        }


        val regionId = entity.regionId

        if (regionId != null) {

            val region = RegionManager.getRegion(
                player.gameProfile.name,
                regionId
            )

            if (region == null) {
                player.sendSystemMessage(
                    Component.literal(
                        "Unable to find linked region!"
                    )
                )

                return InteractionResult.FAIL
            }

            BORDER_LOGGER.info(
                "Opening settings for region #{}",
                regionId
            )

            BorderNetwork.openRegion(player, region)

            return InteractionResult.CONSUME
        }

        BorderChatHandler.requestCode(player, pos)

        return InteractionResult.CONSUME
    }


    override fun onRemove(
        state: BlockState,
        level: Level,
        pos: BlockPos,
        newState: BlockState,
        isMoving: Boolean
    ) {
        if (!state.`is`(newState.block) && level is ServerLevel) {
            BORDER_LOGGER.info("Border Block removed at {}", pos)

            val dimension = level.dimension()
                .location().toString()

            val region = RegionManager.getRegionAt(
                pos,
                dimension
            )

            if (region != null) {
                val first = BlockPos(
                    region.first.x,
                    region.first.y,
                    region.first.z
                )

                val second = BlockPos(
                    region.second.x,
                    region.second.y,
                    region.second.z
                )

                if (pos == first || pos == second) {
                    val other = if (pos == first) second else first

                    BORDER_LOGGER.info(
                        "Deleting region #{} owned by {}",
                        region.id,
                        region.owner
                    )

                    // Delete the region before removing the other
                    // block to prevent recursive deletion.
                    RegionManager.delete(region.owner, region.id)

                    if (level.hasChunkAt(other)) {
                        val otherEntity = level.getBlockEntity(other)
                                as? BorderBlockEntity

                        val removedEntity = level.getBlockEntity(pos)
                                as? BorderBlockEntity

                        if (otherEntity != null &&
                            removedEntity != null &&
                            otherEntity.regionId == region.id &&
                            otherEntity.ownerUUID == removedEntity.ownerUUID
                        ) {
                            level.removeBlock(other, false)

                            BORDER_LOGGER.info(
                                "Removed matching Border Block at {}",
                                other
                            )
                        }
                    } else {
                        BORDER_LOGGER.warn(
                            "Other Border Block is in an unloaded chunk"
                        )
                    }

                    RegionSyncHandler.syncAll(level.server)

                    BORDER_LOGGER.info("Region deletion complete!")
                }
            }
        }

        super.onRemove(
            state,
            level,
            pos,
            newState,
            isMoving
        )
    }

    companion object {
        private val BORDER_LOGGER =
            org.apache.logging.log4j.LogManager.getLogger(
                "BorderControl/BorderBlock"
            )
    }
}