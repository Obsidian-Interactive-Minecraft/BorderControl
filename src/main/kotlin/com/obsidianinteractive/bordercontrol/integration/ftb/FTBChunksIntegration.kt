
package com.obsidianinteractive.bordercontrol.integration.ftb

import dev.ftb.mods.ftbchunks.api.FTBChunksAPI
import dev.ftb.mods.ftbchunks.api.Protection
import dev.ftb.mods.ftblibrary.math.ChunkDimPos
import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.InteractionHand
import net.minecraft.world.entity.Entity
import net.minecraft.world.level.ChunkPos

object FTBChunksIntegration {

    fun isReady(): Boolean {
        return FTBChunksAPI.api().isManagerLoaded
    }

    fun isClaimed(
        player: ServerPlayer,
        pos: BlockPos
    ): Boolean {
        if (!isReady()) return false

        val manager = FTBChunksAPI.api().manager

        val chunkPos = ChunkDimPos(
            player.level().dimension(),
            ChunkPos(pos)
        )

        return manager.getChunk(chunkPos) != null
    }

    private fun checkProtection(
        player: ServerPlayer,
        pos: BlockPos,
        protection: Protection,
        hand: InteractionHand = InteractionHand.MAIN_HAND,
        target: Entity? = null
    ): Boolean? {
        if (!isReady()) return null
        if (!isClaimed(player, pos)) return null

        val manager = FTBChunksAPI.api().manager

        return !manager.shouldPreventInteraction(
            player,
            hand,
            pos,
            protection,
            target
        )
    }

    fun canBuild(
        player: ServerPlayer,
        pos: BlockPos
    ): Boolean? {
        return checkProtection(
            player,
            pos,
            Protection.EDIT_BLOCK
        )
    }

    fun canBreak(
        player: ServerPlayer,
        pos: BlockPos
    ): Boolean? {
        return checkProtection(
            player,
            pos,
            Protection.EDIT_BLOCK
        )
    }

    fun canInteract(
        player: ServerPlayer,
        pos: BlockPos,
        hand: InteractionHand = InteractionHand.MAIN_HAND
    ): Boolean? {
        return checkProtection(
            player,
            pos,
            Protection.INTERACT_BLOCK,
            hand
        )
    }

    fun canInteractEntity(
        player: ServerPlayer,
        entity: Entity,
        hand: InteractionHand = InteractionHand.MAIN_HAND
    ): Boolean? {
        return checkProtection(
            player,
            entity.blockPosition(),
            Protection.INTERACT_ENTITY,
            hand,
            entity
        )
    }
}