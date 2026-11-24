
package com.obsidianinteractive.bordercontrol.blockentity

import com.obsidianinteractive.bordercontrol.BorderControl
import com.obsidianinteractive.bordercontrol.block.ModBlocks
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraftforge.registries.DeferredRegister
import net.minecraftforge.registries.ForgeRegistries

object ModBlockEntities {

    val BLOCK_ENTITIES = DeferredRegister.create(
        ForgeRegistries.BLOCK_ENTITY_TYPES,
        BorderControl.MOD_ID
    )

    val BORDER_BLOCK = BLOCK_ENTITIES.register("border_block") {
        BlockEntityType.Builder.of(
            ::BorderBlockEntity,
            ModBlocks.BORDER_BLOCK.get()
        ).build(null)
    }
}