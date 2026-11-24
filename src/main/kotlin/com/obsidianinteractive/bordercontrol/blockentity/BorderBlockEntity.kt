
package com.obsidianinteractive.bordercontrol.blockentity

import net.minecraft.core.BlockPos
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState
import java.util.UUID

class BorderBlockEntity(
    pos: BlockPos,
    state: BlockState
) : BlockEntity(ModBlockEntities.BORDER_BLOCK.get(), pos, state) {

    var ownerUUID: UUID? = null
        private set

    var linkCode: String? = null
        private set

    var regionId: Int? = null
        private set

    fun setOwner(uuid: UUID) {
        ownerUUID = uuid
        setChanged()
    }

    fun setCode(code: String) {
        linkCode = code
        setChanged()
    }

    fun linkRegion(id: Int) {
        regionId = id
        setChanged()
    }

    override fun saveAdditional(tag: CompoundTag) {
        super.saveAdditional(tag)

        ownerUUID?.let {
            tag.putUUID("OwnerUUID", it)
        }

        linkCode?.let {
            tag.putString("LinkCode", it)
        }

        regionId?.let {
            tag.putInt("RegionID", it)
        }
    }

    override fun load(tag: CompoundTag) {
        super.load(tag)

        ownerUUID = if (tag.hasUUID("OwnerUUID"))
            tag.getUUID("OwnerUUID")
        else null

        linkCode = if (tag.contains("LinkCode"))
            tag.getString("LinkCode")
        else null

        regionId = if (tag.contains("RegionID"))
            tag.getInt("RegionID")
        else null
    }
}