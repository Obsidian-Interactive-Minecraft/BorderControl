
package com.obsidianinteractive.bordercontrol.network

import com.obsidianinteractive.bordercontrol.region.AccessMode
import com.obsidianinteractive.bordercontrol.region.PlayerAccess
import com.obsidianinteractive.bordercontrol.region.RegionSettings
import com.obsidianinteractive.bordercontrol.region.TeamAccess
import com.obsidianinteractive.bordercontrol.region.TeamRole
import net.minecraft.network.FriendlyByteBuf

object RegionPacketCodec {

    fun writeMode(
        buffer: FriendlyByteBuf,
        mode: AccessMode
    ) {
        buffer.writeEnum(mode)
    }

    fun readMode(
        buffer: FriendlyByteBuf
    ): AccessMode {
        return buffer.readEnum(
            AccessMode::class.java
        )
    }

    fun writePlayers(
        buffer: FriendlyByteBuf,
        players: List<PlayerAccess>
    ) {

        require(
            players.size <= RegionSettings.MAX_PLAYERS
        )

        buffer.writeVarInt(players.size)

        for (player in players) {

            buffer.writeUtf(player.username, 16)
            buffer.writeUtf(player.uuid, 36)

            writePermission(buffer, player.build)
            writePermission(buffer, player.breakBlocks)
            writePermission(buffer, player.interact)
        }
    }

    fun readPlayers(
        buffer: FriendlyByteBuf
    ): List<PlayerAccess> {

        val count = buffer.readVarInt()

        require(
            count in 0..RegionSettings.MAX_PLAYERS
        ) {
            "Invalid player count"
        }

        return List(count) {

            PlayerAccess(
                username = buffer.readUtf(16),
                uuid = buffer.readUtf(36),
                build = readPermission(buffer),
                breakBlocks = readPermission(buffer),
                interact = readPermission(buffer)
            )
        }
    }

    fun writeTeamPermissions(
        buffer: FriendlyByteBuf,
        permissions: Map<TeamRole, TeamAccess>
    ) {

        val defaults = TeamAccess.defaults()

        for (role in TeamRole.entries) {

            val access = permissions[role]
                ?: defaults.getValue(role)

            writePermission(buffer, access.cross)
            writePermission(buffer, access.build)
            writePermission(buffer, access.breakBlocks)
            writePermission(buffer, access.interact)
        }
    }

    fun readTeamPermissions(
        buffer: FriendlyByteBuf
    ): Map<TeamRole, TeamAccess> {

        return TeamRole.entries.associateWith {

            TeamAccess(
                cross = readPermission(buffer),
                build = readPermission(buffer),
                breakBlocks = readPermission(buffer),
                interact = readPermission(buffer)
            )
        }
    }

    private fun writePermission(
        buffer: FriendlyByteBuf,
        permission: Boolean?
    ) {

        buffer.writeByte(
            when (permission) {
                null -> 0
                true -> 1
                false -> 2
            }
        )
    }

    private fun readPermission(
        buffer: FriendlyByteBuf
    ): Boolean? {

        return when (
            buffer.readUnsignedByte().toInt()
        ) {
            0 -> null
            1 -> true
            2 -> false

            else ->
                error("Invalid permission value")
        }
    }
}