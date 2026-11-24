
package com.obsidianinteractive.bordercontrol.network

import com.obsidianinteractive.bordercontrol.border.BorderBounds
import com.obsidianinteractive.bordercontrol.integration.ftb.FTBCompat
import com.obsidianinteractive.bordercontrol.region.AccessMode
import com.obsidianinteractive.bordercontrol.region.BorderRegion
import com.obsidianinteractive.bordercontrol.region.PlayerAccess
import com.obsidianinteractive.bordercontrol.region.TeamAccess
import com.obsidianinteractive.bordercontrol.region.TeamRole
import net.minecraft.network.FriendlyByteBuf

data class OpenRegionPacket(
    val owner: String,
    val id: Int,
    val width: Int,
    val length: Int,
    val dimension: String,
    val mode: AccessMode,
    val allowlist: List<PlayerAccess>,
    val blocklist: List<PlayerAccess>,
    val teamPermissions: Map<TeamRole, TeamAccess>,
    val teamsEnabled: Boolean
) {

    fun encode(buffer: FriendlyByteBuf) {

        buffer.writeUtf(owner, 16)
        buffer.writeVarInt(id)
        buffer.writeVarInt(width)
        buffer.writeVarInt(length)
        buffer.writeUtf(dimension)

        RegionPacketCodec.writeMode(
            buffer,
            mode
        )

        RegionPacketCodec.writePlayers(
            buffer,
            allowlist
        )

        RegionPacketCodec.writePlayers(
            buffer,
            blocklist
        )

        RegionPacketCodec.writeTeamPermissions(
            buffer,
            teamPermissions
        )

        buffer.writeBoolean(teamsEnabled)
    }

    companion object {

        fun decode(
            buffer: FriendlyByteBuf
        ): OpenRegionPacket {

            return OpenRegionPacket(
                owner = buffer.readUtf(16),
                id = buffer.readVarInt(),
                width = buffer.readVarInt(),
                length = buffer.readVarInt(),
                dimension = buffer.readUtf(),

                mode =
                    RegionPacketCodec.readMode(buffer),

                allowlist =
                    RegionPacketCodec.readPlayers(buffer),

                blocklist =
                    RegionPacketCodec.readPlayers(buffer),

                teamPermissions =
                    RegionPacketCodec.readTeamPermissions(
                        buffer
                    ),

                teamsEnabled = buffer.readBoolean()
            )
        }

        fun fromRegion(
            region: BorderRegion
        ): OpenRegionPacket {

            val bounds =
                BorderBounds.fromRegion(region)

            return OpenRegionPacket(
                owner = region.owner,
                id = region.id,
                width = bounds.width.toInt(),
                length = bounds.length.toInt(),
                dimension = bounds.dimension,
                mode = region.mode,
                allowlist = region.allowlist.toList(),
                blocklist = region.blocklist.toList(),
                teamPermissions =
                    region.teamPermissions.toMap(),
                teamsEnabled =
                    FTBCompat.isTeamsLoaded()
            )
        }
    }
}