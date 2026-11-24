package com.obsidianinteractive.bordercontrol.item

import com.obsidianinteractive.bordercontrol.BorderControl
import com.obsidianinteractive.bordercontrol.item.ModItems
import net.minecraft.core.registries.Registries
import net.minecraft.network.chat.Component
import net.minecraft.world.item.CreativeModeTab
import net.minecraft.world.item.ItemStack
import net.minecraftforge.eventbus.api.IEventBus
import net.minecraftforge.registries.DeferredRegister
import net.minecraftforge.registries.RegistryObject

object ModCreativeTabs {
    val CREATIVE_MODE_TABS: DeferredRegister<CreativeModeTab> = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, BorderControl.MOD_ID)

    val BORDERCONTROL_CREATIVETAB: RegistryObject<CreativeModeTab> = CREATIVE_MODE_TABS.register("bordercontrol_creativetab") {
        CreativeModeTab.builder()
            .icon {
                ItemStack(ModItems.BORDER_BLOCK.get())
            }
            .title(Component.translatable("bordercontrol.creativetab.title"))
            .displayItems {_, output ->
                output.accept(ModItems.BORDER_BLOCK.get())
            }
            .build()
    }

    fun register(eventBus: IEventBus) {CREATIVE_MODE_TABS.register(eventBus)}
}