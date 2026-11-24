
package com.obsidianinteractive.bordercontrol.network

import net.minecraft.network.FriendlyByteBuf

data class RegionSettingsResultPacket(
    val success: Boolean,
    val message: String
) {

    fun encode(buffer: FriendlyByteBuf) {
        buffer.writeBoolean(success)
        buffer.writeUtf(message, 256)
    }

    companion object {

        fun decode(
            buffer: FriendlyByteBuf
        ): RegionSettingsResultPacket {

            return RegionSettingsResultPacket(
                success = buffer.readBoolean(),
                message = buffer.readUtf(256)
            )
        }
    }
}