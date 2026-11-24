
package com.obsidianinteractive.bordercontrol.border

import com.obsidianinteractive.bordercontrol.integration.ftb.FTBCompat
import com.obsidianinteractive.bordercontrol.region.AccessMode
import com.obsidianinteractive.bordercontrol.region.BorderRegion
import com.obsidianinteractive.bordercontrol.region.PlayerAccess
import com.obsidianinteractive.bordercontrol.region.TeamAccess
import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.InteractionHand
import net.minecraft.world.entity.Entity

object BorderAccess {

    fun isOwner(
        region: BorderRegion,
        player: ServerPlayer
    ): Boolean {
        return region.owner.equals(
            player.gameProfile.name,
            ignoreCase = true
        )
    }

    fun isOperator(player: ServerPlayer): Boolean {
        return player.server.playerList.isOp(
            player.gameProfile
        )
    }

    fun isAllowlisted(
        region: BorderRegion,
        player: ServerPlayer
    ): Boolean {
        return region.allowlist.any {
            it.uuid.equals(
                player.uuid.toString(),
                ignoreCase = true
            )
        }
    }

    fun isBlocklisted(
        region: BorderRegion,
        player: ServerPlayer
    ): Boolean {
        return region.blocklist.any {
            it.uuid.equals(
                player.uuid.toString(),
                ignoreCase = true
            )
        }
    }

    private fun getTeamAccess(
        region: BorderRegion,
        player: ServerPlayer
    ): TeamAccess? {

        val role = FTBCompat.getTeamRole(
            region,
            player
        ) ?: return null

        return region.teamPermissions[role]
            ?: TeamAccess.defaults()[role]
    }

    private fun getPlayerAccess(
        region: BorderRegion,
        player: ServerPlayer
    ): PlayerAccess? {

        if (region.mode != AccessMode.PERMISSION) {
            return null
        }

        return region.allowlist.firstOrNull {
            it.uuid.equals(
                player.uuid.toString(),
                ignoreCase = true
            )
        }
    }

    fun canCross(
        region: BorderRegion,
        player: ServerPlayer
    ): Boolean {

        if (isOwner(region, player)) return true

        if (region.mode == AccessMode.NOBODY) {
            return false
        }

        if (isBlocklisted(region, player)) {
            return false
        }

        if (isOperator(player)) return true

        if (
            region.mode == AccessMode.PERMISSION &&
            isAllowlisted(region, player)
        ) {
            return true
        }

        val teamPermission =
            getTeamAccess(region, player)?.cross

        if (teamPermission != null) {
            return teamPermission
        }

        return region.mode == AccessMode.PUBLIC
    }

    fun getBorderColor(
        region: BorderRegion,
        player: ServerPlayer
    ): Int {
        return if (canCross(region, player)) {
            0x00FF00
        } else {
            0xFF0000
        }
    }

    private fun canPerform(
        region: BorderRegion,
        player: ServerPlayer,
        playerPermission: (PlayerAccess) -> Boolean?,
        teamPermission: (TeamAccess) -> Boolean?,
        externalPermission: () -> Boolean?
    ): Boolean {

        if (isOwner(region, player)) return true

        if (region.mode == AccessMode.NOBODY) {
            return false
        }

        if (isBlocklisted(region, player)) {
            return false
        }

        // Individual permission overrides.
        val individual = getPlayerAccess(
            region,
            player
        )?.let(playerPermission)

        if (individual != null) {
            return individual
        }

        if (isOperator(player)) return true

        // FTB Teams role permissions.
        val team = getTeamAccess(
            region,
            player
        )?.let(teamPermission)

        if (team != null) {
            return team
        }

        // FTB Chunks and future claim integrations.
        val inherited = externalPermission()

        if (inherited != null) {
            return inherited
        }

        // Standard region access.
        return canCross(region, player)
    }

    fun canBuild(
        region: BorderRegion,
        player: ServerPlayer,
        pos: BlockPos
    ): Boolean {
        return canPerform(
            region,
            player,
            playerPermission = { it.build },
            teamPermission = { it.build },
            externalPermission = {
                FTBCompat.canBuild(player, pos)
            }
        )
    }

    fun canBreak(
        region: BorderRegion,
        player: ServerPlayer,
        pos: BlockPos
    ): Boolean {
        return canPerform(
            region,
            player,
            playerPermission = { it.breakBlocks },
            teamPermission = { it.breakBlocks },
            externalPermission = {
                FTBCompat.canBreak(player, pos)
            }
        )
    }

    fun canInteract(
        region: BorderRegion,
        player: ServerPlayer,
        pos: BlockPos,
        hand: InteractionHand = InteractionHand.MAIN_HAND
    ): Boolean {
        return canPerform(
            region,
            player,
            playerPermission = { it.interact },
            teamPermission = { it.interact },
            externalPermission = {
                FTBCompat.canInteract(
                    player,
                    pos,
                    hand
                )
            }
        )
    }

    fun canInteractEntity(
        region: BorderRegion,
        player: ServerPlayer,
        entity: Entity,
        hand: InteractionHand = InteractionHand.MAIN_HAND
    ): Boolean {
        return canPerform(
            region,
            player,
            playerPermission = { it.interact },
            teamPermission = { it.interact },
            externalPermission = {
                FTBCompat.canInteractEntity(
                    player,
                    entity,
                    hand
                )
            }
        )
    }
}