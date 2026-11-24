
package com.obsidianinteractive.bordercontrol.integration.ftb

import com.obsidianinteractive.bordercontrol.region.BorderRegion
import com.obsidianinteractive.bordercontrol.region.TeamRole
import dev.ftb.mods.ftbteams.api.FTBTeamsAPI
import dev.ftb.mods.ftbteams.api.TeamRank
import net.minecraft.server.level.ServerPlayer

object FTBTeamsIntegration {

    fun isReady(): Boolean {
        return FTBTeamsAPI.api().isManagerLoaded
    }

    fun getRole(
        region: BorderRegion,
        player: ServerPlayer
    ): TeamRole? {

        if (!isReady()) return null

        val server = player.server

        val ownerUUID =
            server.playerList
                .getPlayerByName(region.owner)
                ?.uuid
                ?: server.profileCache
                    ?.get(region.owner)
                    ?.orElse(null)
                    ?.id
                ?: return null

        val manager = FTBTeamsAPI.api().manager

        val team = manager
            .getTeamForPlayerID(ownerUUID)
            .orElse(null)
            ?: return null

        if (!team.isValid) return null

        return when (team.getRankForPlayer(player.uuid)) {

            TeamRank.OWNER -> TeamRole.OWNER

            TeamRank.OFFICER -> TeamRole.OFFICER

            TeamRank.MEMBER -> TeamRole.MEMBER

            TeamRank.INVITED -> TeamRole.INVITED

            TeamRank.ALLY -> TeamRole.ALLY

            TeamRank.NONE -> TeamRole.NONE

            TeamRank.ENEMY -> TeamRole.ENEMY
        }
    }
}