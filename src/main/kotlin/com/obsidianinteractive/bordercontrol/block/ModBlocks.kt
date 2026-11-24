
package com.obsidianinteractive.bordercontrol.block

import com.obsidianinteractive.bordercontrol.BorderControl
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.SoundType
import net.minecraft.world.level.block.state.BlockBehaviour
import net.minecraft.world.level.material.PushReaction
import net.minecraftforge.registries.DeferredRegister
import net.minecraftforge.registries.ForgeRegistries

object ModBlocks {

    val BLOCKS = DeferredRegister.create(
        ForgeRegistries.BLOCKS,
        BorderControl.MOD_ID
    )

    val BORDER_BLOCK = BLOCKS.register("border_block") {
        BorderBlock(
            BlockBehaviour.Properties.of()
                .strength(5.0f, 3600000.0f)
                .sound(SoundType.METAL)
                .pushReaction(PushReaction.BLOCK)
                .requiresCorrectToolForDrops()
        )
    }
}