
package com.obsidianinteractive.bordercontrol.network

import com.obsidianinteractive.bordercontrol.region.AccessMode
import com.obsidianinteractive.bordercontrol.region.PlayerAccess
import com.obsidianinteractive.bordercontrol.region.RegionSettings
import com.obsidianinteractive.bordercontrol.region.TeamAccess
import com.obsidianinteractive.bordercontrol.region.TeamRole
import com.obsidianinteractive.bordercontrol.events.RegionSyncHandler
import net.minecraft.network.FriendlyByteBuf
import net.minecraftforge.network.NetworkEvent
import net.minecraftforge.network.PacketDistributor
import org.apache.logging.log4j.LogManager
import java.util.function.Supplier

data class UpdateRegionPacket(
    val owner: String,
    val id: Int,
    val mode: AccessMode,
    val allowlist: List<PlayerAccess>,
    val blocklist: List<PlayerAccess>,
    val teamPermissions: Map<TeamRole, TeamAccess>
) {

    fun encode(buffer: FriendlyByteBuf) {

        buffer.writeUtf(owner, 16)
        buffer.writeVarInt(id)

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
    }

    fun handle(
        supplier: Supplier<NetworkEvent.Context>
    ) {

        val context = supplier.get()
        val player = context.sender

        context.packetHandled = true

        if (player == null) return

        val result = try {

            RegionSettings.update(
                player = player,
                owner = owner,
                id = id,
                mode = mode,
                allowlist = allowlist,
                blocklist = blocklist,
                teamPermissions = teamPermissions
            )

            LOGGER.info(
                "Region {}/{} updated by {}",
                owner,
                id,
                player.gameProfile.name
            )

            RegionSettingsResultPacket(
                true,
                "Region settings saved!"
            )

        } catch (exception: Exception) {

            LOGGER.warn(
                "Region settings update rejected",
                exception
            )

            RegionSettingsResultPacket(
                false,
                "Unable to save region settings."
            )
        }

        BorderNetwork.CHANNEL.send(
            PacketDistributor.PLAYER.with {
                player
            },
            result
        )

        if (result.success) {

            RegionSyncHandler.syncAll(
                player.server
            )
        }
    }

    companion object {

        private val LOGGER =
            LogManager.getLogger(
                "BorderControl/RegionSettings"
            )

        fun decode(
            buffer: FriendlyByteBuf
        ): UpdateRegionPacket {

            return UpdateRegionPacket(
                owner = buffer.readUtf(16),
                id = buffer.readVarInt(),

                mode =
                    RegionPacketCodec.readMode(buffer),

                allowlist =
                    RegionPacketCodec.readPlayers(buffer),

                blocklist =
                    RegionPacketCodec.readPlayers(buffer),

                teamPermissions =
                    RegionPacketCodec.readTeamPermissions(
                        buffer
                    )
            )
        }
    }
}