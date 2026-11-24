
package com.obsidianinteractive.bordercontrol.integration.ftb

import com.obsidianinteractive.bordercontrol.integration.IntegrationManager
import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.InteractionHand
import net.minecraft.world.entity.Entity
import com.obsidianinteractive.bordercontrol.region.BorderRegion
import com.obsidianinteractive.bordercontrol.region.TeamRole

object FTBCompat {

    fun isChunksLoaded(): Boolean {
        return IntegrationManager.isEnabled(
            IntegrationManager.Integration.FTB_CHUNKS
        )
    }

    fun canBuild(
        player: ServerPlayer,
        pos: BlockPos
    ): Boolean? {
        if (!isChunksLoaded()) return null

        return FTBChunksIntegration.canBuild(player, pos)
    }

    fun canBreak(
        player: ServerPlayer,
        pos: BlockPos
    ): Boolean? {
        if (!isChunksLoaded()) return null

        return FTBChunksIntegration.canBreak(player, pos)
    }

    fun canInteract(
        player: ServerPlayer,
        pos: BlockPos,
        hand: InteractionHand = InteractionHand.MAIN_HAND
    ): Boolean? {
        if (!isChunksLoaded()) return null

        return FTBChunksIntegration.canInteract(
            player,
            pos,
            hand
        )
    }

    fun canInteractEntity(
        player: ServerPlayer,
        entity: Entity,
        hand: InteractionHand = InteractionHand.MAIN_HAND
    ): Boolean? {
        if (!isChunksLoaded()) return null

        return FTBChunksIntegration.canInteractEntity(
            player,
            entity,
            hand
        )
    }

    fun isTeamsLoaded(): Boolean {
        return IntegrationManager.isEnabled(
            IntegrationManager.Integration.FTB_TEAMS
        )
    }

    fun getTeamRole(
        region: BorderRegion,
        player: ServerPlayer
    ): TeamRole? {

        if (!isTeamsLoaded()) return null

        return FTBTeamsIntegration.getRole(
            region,
            player
        )
    }
}