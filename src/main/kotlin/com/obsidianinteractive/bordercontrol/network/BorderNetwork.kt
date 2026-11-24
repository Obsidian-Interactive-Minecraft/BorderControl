
package com.obsidianinteractive.bordercontrol.network

import com.obsidianinteractive.bordercontrol.BorderControl
import com.obsidianinteractive.bordercontrol.client.ClientPacketHandler
import com.obsidianinteractive.bordercontrol.region.BorderRegion
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerPlayer
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.fml.DistExecutor
import net.minecraftforge.network.NetworkRegistry
import net.minecraftforge.network.NetworkDirection
import net.minecraftforge.network.PacketDistributor

object BorderNetwork {

    private const val PROTOCOL_VERSION = "3"

    val CHANNEL = NetworkRegistry.newSimpleChannel(
        ResourceLocation(
            BorderControl.MOD_ID,
            "network"
        ),
        { PROTOCOL_VERSION },
        { it == PROTOCOL_VERSION },
        { it == PROTOCOL_VERSION }
    )

    fun register() {

        CHANNEL.messageBuilder(
            RegionSyncPacket::class.java,
            0,
            NetworkDirection.PLAY_TO_CLIENT
        )
            .encoder(RegionSyncPacket::encode)
            .decoder(RegionSyncPacket::decode)
            .consumerMainThread(RegionSyncPacket::handle)
            .add()

        CHANNEL.messageBuilder(
            OpenRegionPacket::class.java,
            1,
            NetworkDirection.PLAY_TO_CLIENT
        )
            .encoder(OpenRegionPacket::encode)
            .decoder(OpenRegionPacket::decode)
            .consumerMainThread { packet, supplier ->

                DistExecutor.unsafeRunWhenOn(Dist.CLIENT) {
                    Runnable {
                        ClientPacketHandler.openRegion(packet)
                    }
                }

                supplier.get().packetHandled = true
            }
            .add()

        CHANNEL.messageBuilder(
            UpdateRegionPacket::class.java,
            2,
            NetworkDirection.PLAY_TO_SERVER
        )
            .encoder(UpdateRegionPacket::encode)
            .decoder(UpdateRegionPacket::decode)
            .consumerMainThread(UpdateRegionPacket::handle)
            .add()

        CHANNEL.messageBuilder(
            RegionSettingsResultPacket::class.java,
            3,
            NetworkDirection.PLAY_TO_CLIENT
        )
            .encoder(RegionSettingsResultPacket::encode)
            .decoder(RegionSettingsResultPacket::decode)
            .consumerMainThread { packet, supplier ->

                DistExecutor.unsafeRunWhenOn(Dist.CLIENT) {
                    Runnable {
                        ClientPacketHandler.handleResult(packet)
                    }
                }

                supplier.get().packetHandled = true
            }
            .add()
    }

    fun sync(player: ServerPlayer) {
        CHANNEL.send(
            PacketDistributor.PLAYER.with { player },
            RegionSyncPacket.fromPlayer(player)
        )
    }

    fun openRegion(
        player: ServerPlayer,
        region: BorderRegion
    ) {
        CHANNEL.send(
            PacketDistributor.PLAYER.with { player },
            OpenRegionPacket.fromRegion(region)
        )
    }
}