
package com.obsidianinteractive.bordercontrol.events

import com.obsidianinteractive.bordercontrol.blockentity.BorderBlockEntity
import com.obsidianinteractive.bordercontrol.region.BorderPoint
import com.obsidianinteractive.bordercontrol.region.RegionManager
import net.minecraft.core.BlockPos
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerPlayer
import net.minecraftforge.event.ServerChatEvent
import net.minecraftforge.eventbus.api.SubscribeEvent
import org.apache.logging.log4j.LogManager
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

object BorderChatHandler {

    private val LOGGER =
        LogManager.getLogger("BorderControl/ChatHandler")

    private val CODE_PATTERN =
        Regex("[A-Za-z0-9_-]{1,32}")

    private data class PendingInput(
        val pos: BlockPos,
        val dimension: String
    )

    private data class LinkKey(
        val owner: UUID,
        val dimension: String,
        val code: String
    )

    private val pendingInputs =
        ConcurrentHashMap<UUID, PendingInput>()

    private val waitingBlocks =
        HashMap<LinkKey, BlockPos>()

    private val blockKeys =
        HashMap<Pair<UUID, BorderPoint>, LinkKey>()

    init {
        LOGGER.info("BorderChatHandler initialized")
    }

    fun requestCode(
        player: ServerPlayer,
        pos: BlockPos
    ) {
        val dimension = player.serverLevel()
            .dimension().location().toString()

        pendingInputs[player.uuid] = PendingInput(
            pos.immutable(),
            dimension
        )

        LOGGER.info(
            "Linking requested by {} at {} in {}",
            player.gameProfile.name,
            pos,
            dimension
        )

        message(
            player,
            "Enter a linking code in chat, or type cancel."
        )
    }

    @SubscribeEvent
    fun onChat(event: ServerChatEvent) {
        val player = event.player

        val pending = pendingInputs.remove(player.uuid)
            ?: return

        event.isCanceled = true

        val code = event.rawText.trim()

        LOGGER.info(
            "Linking input received on thread {}",
            Thread.currentThread().name
        )

        player.server.execute {
            processCode(player, pending, code)
        }
    }


    private fun processCode(
        player: ServerPlayer,
        pending: PendingInput,
        code: String
    ) {
        LOGGER.info(
            "Processing linking input on thread {}",
            Thread.currentThread().name
        )

        if (code.equals("cancel", ignoreCase = true)) {
            message(player, "Border linking cancelled.")
            return
        }

        if (!CODE_PATTERN.matches(code)) {
            reject(player, "Invalid linking code!")
            return
        }

        val level = player.serverLevel()
        val dimension = level.dimension()
            .location().toString()

        if (dimension != pending.dimension) {
            reject(player, "You changed dimensions!")
            return
        }

        if (!level.hasChunkAt(pending.pos)) {
            reject(player, "Border block chunk is not loaded!")
            return
        }

        val entity = level.getBlockEntity(pending.pos)
                as? BorderBlockEntity

        if (entity == null) {
            reject(player, "Border Block Entity not found!")
            return
        }

        if (!isAvailable(entity, player.uuid)) {
            reject(
                player,
                "Border block is already linked or not yours!"
            )
            return
        }

        val currentPoint = toPoint(pending.pos, dimension)
        val currentIdentity = player.uuid to currentPoint

        // Remove the previous code associated with this block.
        blockKeys.remove(currentIdentity)?.let { oldKey ->
            if (waitingBlocks[oldKey] == pending.pos) {
                waitingBlocks.remove(oldKey)
            }
        }

        entity.setCode(code)

        val key = LinkKey(
            player.uuid,
            dimension,
            code
        )

        val firstPos = waitingBlocks[key]

        if (firstPos == null) {
            registerWaiting(key, pending.pos)

            message(
                player,
                "Code saved! Configure the second Border Block."
            )

            return
        }

        LOGGER.info(
            "Matching blocks found: {} and {}",
            firstPos,
            pending.pos
        )

        if (!level.hasChunkAt(firstPos)) {
            reject(
                player,
                "The first Border Block must be loaded!"
            )
            return
        }

        val firstEntity = level.getBlockEntity(firstPos)
                as? BorderBlockEntity

        if (firstEntity == null ||
            !isAvailable(firstEntity, player.uuid) ||
            firstEntity.linkCode != code
        ) {
            blockKeys.remove(
                player.uuid to toPoint(firstPos, dimension)
            )

            registerWaiting(key, pending.pos)

            message(
                player,
                "Previous block unavailable. New linking point saved."
            )

            return
        }

        try {
            LOGGER.info("Creating region...")

            val region = RegionManager.create(
                player,
                toPoint(firstPos, dimension),
                currentPoint
            )

            firstEntity.linkRegion(region.id)
            entity.linkRegion(region.id)

            // Remove completed linking entries.
            waitingBlocks.remove(key)

            blockKeys.remove(
                player.uuid to toPoint(firstPos, dimension)
            )

            blockKeys.remove(currentIdentity)

            LOGGER.info(
                "Region #{} created successfully for {}",
                region.id,
                region.owner
            )

            message(
                player,
                "Successfully created region #${region.id}!"
            )

        } catch (exception: Exception) {
            LOGGER.error("Region creation failed", exception)

            message(
                player,
                "Region creation failed: ${exception.message}"
            )

            return
        }

        // Synchronize the newly created region with clients.
        try {
            LOGGER.info("Synchronizing regions with clients...")

            RegionSyncHandler.syncAll(player.server)

            LOGGER.info("Region synchronization complete!")

        } catch (exception: Exception) {
            LOGGER.error("Region synchronization failed", exception)

            message(
                player,
                "Region created, but border synchronization failed!"
            )
        }
    }

    private fun registerWaiting(
        key: LinkKey,
        pos: BlockPos
    ) {
        waitingBlocks[key] = pos.immutable()

        blockKeys[
            key.owner to toPoint(pos, key.dimension)
        ] = key

        LOGGER.info(
            "Waiting block registered at {}. Total: {}",
            pos,
            waitingBlocks.size
        )
    }

    private fun isAvailable(
        entity: BorderBlockEntity,
        owner: UUID
    ): Boolean {
        return entity.ownerUUID == owner &&
                entity.regionId == null
    }

    private fun toPoint(
        pos: BlockPos,
        dimension: String
    ): BorderPoint {
        return BorderPoint(
            pos.x,
            pos.y,
            pos.z,
            dimension
        )
    }

    private fun message(
        player: ServerPlayer,
        text: String
    ) {
        player.sendSystemMessage(Component.literal(text))
    }

    private fun reject(
        player: ServerPlayer,
        reason: String
    ) {
        LOGGER.warn("Linking rejected: {}", reason)
        message(player, reason)
    }
}