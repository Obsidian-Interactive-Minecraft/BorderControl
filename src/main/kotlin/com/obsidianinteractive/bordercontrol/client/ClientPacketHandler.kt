
package com.obsidianinteractive.bordercontrol.client

import com.obsidianinteractive.bordercontrol.client.gui.RegionSettingsScreen
import com.obsidianinteractive.bordercontrol.network.OpenRegionPacket
import com.obsidianinteractive.bordercontrol.network.RegionSettingsResultPacket
import net.minecraft.client.Minecraft

object ClientPacketHandler {

    fun openRegion(packet: OpenRegionPacket) {
        Minecraft.getInstance().setScreen(
            RegionSettingsScreen(packet)
        )
    }

    fun handleResult(
        packet: RegionSettingsResultPacket
    ) {
        val minecraft = Minecraft.getInstance()

        val screen = minecraft.screen

        if (screen is RegionSettingsScreen) {
            screen.handleResult(packet)
        } else {
            minecraft.player?.sendSystemMessage(
                net.minecraft.network.chat.Component.literal(
                    packet.message
                )
            )
        }
    }
}