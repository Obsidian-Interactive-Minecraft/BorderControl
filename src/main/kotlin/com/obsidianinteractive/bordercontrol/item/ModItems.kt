
package com.obsidianinteractive.bordercontrol.item

import com.obsidianinteractive.bordercontrol.BorderControl
import com.obsidianinteractive.bordercontrol.block.ModBlocks
import net.minecraft.world.item.BlockItem
import net.minecraft.world.item.Item
import net.minecraftforge.registries.DeferredRegister
import net.minecraftforge.registries.ForgeRegistries

object ModItems {

    val ITEMS = DeferredRegister.create(
        ForgeRegistries.ITEMS,
        BorderControl.MOD_ID
    )

    val BORDER_BLOCK = ITEMS.register("border_block") {
        BlockItem(
            ModBlocks.BORDER_BLOCK.get(),
            Item.Properties()
        )
    }
}