
package com.obsidianinteractive.bordercontrol

import com.obsidianinteractive.bordercontrol.config.Config
import com.obsidianinteractive.bordercontrol.block.ModBlocks
import com.obsidianinteractive.bordercontrol.item.ModItems
import com.obsidianinteractive.bordercontrol.item.ModCreativeTabs
import com.obsidianinteractive.bordercontrol.blockentity.ModBlockEntities
import com.obsidianinteractive.bordercontrol.events.BorderChatHandler
import com.obsidianinteractive.bordercontrol.events.BorderBreakHandler
import com.obsidianinteractive.bordercontrol.events.BorderMovementHandler
import com.obsidianinteractive.bordercontrol.events.RegionProtectionHandler
import com.obsidianinteractive.bordercontrol.events.RegionSyncHandler
import com.obsidianinteractive.bordercontrol.events.RegionReloadHandler
import com.obsidianinteractive.bordercontrol.events.RegionLifecycleHandler
import com.obsidianinteractive.bordercontrol.events.TeamSyncHandler
import com.obsidianinteractive.bordercontrol.integration.IntegrationManager
import com.obsidianinteractive.bordercontrol.network.BorderNetwork
import com.obsidianinteractive.bordercontrol.client.ClientSetup
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.fml.common.Mod
import net.minecraftforge.fml.loading.FMLEnvironment
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent
import org.apache.logging.log4j.LogManager
import thedarkcolour.kotlinforforge.forge.MOD_BUS

@Mod(BorderControl.MOD_ID)
class BorderControl {

    companion object {

        const val MOD_ID = "bordercontrol"

        private val LOGGER =
            LogManager.getLogger()
    }

    init {
        LOGGER.info("BorderControl is initializing!")
        Config.load()
        ModBlocks.BLOCKS.register(MOD_BUS)
        ModItems.ITEMS.register(MOD_BUS)
        ModCreativeTabs.register(MOD_BUS)
        ModBlockEntities.BLOCK_ENTITIES.register(MOD_BUS)
        BorderNetwork.register()
        val eventBus = MinecraftForge.EVENT_BUS
        eventBus.register(BorderChatHandler)
        eventBus.register(BorderBreakHandler)
        eventBus.register(BorderMovementHandler)
        eventBus.register(RegionProtectionHandler)
        eventBus.register(RegionSyncHandler)
        eventBus.register(RegionReloadHandler)
        eventBus.register(RegionLifecycleHandler)
        eventBus.register(TeamSyncHandler)
        MOD_BUS.addListener<FMLCommonSetupEvent> { event -> event.enqueueWork { IntegrationManager.initialize() }
        }

        if (FMLEnvironment.dist == Dist.CLIENT) { ClientSetup.register() }

        LOGGER.info("BorderControl configuration loaded! Max border size: {}x{}", Config.maxBorderSize, Config.maxBorderSize)
    }
}