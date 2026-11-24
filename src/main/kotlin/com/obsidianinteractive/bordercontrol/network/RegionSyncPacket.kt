
package com.obsidianinteractive.bordercontrol.network

import com.obsidianinteractive.bordercontrol.border.BorderAccess
import com.obsidianinteractive.bordercontrol.border.BorderBounds
import com.obsidianinteractive.bordercontrol.client.ClientRegions
import com.obsidianinteractive.bordercontrol.region.RegionConfig
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.server.level.ServerPlayer
import net.minecraftforge.network.NetworkEvent
import java.util.function.Supplier

data class RegionSyncPacket(
    val regions: List<ClientRegions.RenderRegion>
) {

    fun encode(buffer: FriendlyByteBuf) {
        buffer.writeVarInt(regions.size)

        for (region in regions) {
            buffer.writeUtf(region.owner)
            buffer.writeVarInt(region.id)

            val bounds = region.bounds

            buffer.writeDouble(bounds.minX)
            buffer.writeDouble(bounds.minZ)
            buffer.writeDouble(bounds.maxX)
            buffer.writeDouble(bounds.maxZ)
            buffer.writeUtf(bounds.dimension)

            buffer.writeBoolean(region.allowed)
        }
    }

    fun handle(
        supplier: Supplier<NetworkEvent.Context>
    ) {
        val context = supplier.get()

        ClientRegions.update(regions)

        context.packetHandled = true
    }

    companion object {

        fun decode(
            buffer: FriendlyByteBuf
        ): RegionSyncPacket {

            val count = buffer.readVarInt()

            require(count >= 0) {
                "Invalid region count"
            }

            val regions = ArrayList<ClientRegions.RenderRegion>(
                count.coerceAtMost(1024)
            )

            repeat(count) {
                val owner = buffer.readUtf()
                val id = buffer.readVarInt()

                val bounds = BorderBounds(
                    minX = buffer.readDouble(),
                    minZ = buffer.readDouble(),
                    maxX = buffer.readDouble(),
                    maxZ = buffer.readDouble(),
                    dimension = buffer.readUtf()
                )

                val allowed = buffer.readBoolean()

                regions.add(
                    ClientRegions.RenderRegion(
                        owner,
                        id,
                        bounds,
                        allowed
                    )
                )
            }

            return RegionSyncPacket(regions)
        }

        fun fromPlayer(
            player: ServerPlayer
        ): RegionSyncPacket {

            val dimension = player.serverLevel()
                .dimension().location().toString()

            val regions = RegionConfig.getRegions()
                .asSequence()
                .filter {
                    it.first.dimension == dimension
                }
                .map {
                    ClientRegions.RenderRegion(
                        owner = it.owner,
                        id = it.id,
                        bounds = BorderBounds.fromRegion(it),
                        allowed = BorderAccess.canCross(
                            it,
                            player
                        )
                    )
                }
                .toList()

            return RegionSyncPacket(regions)
        }
    }
}