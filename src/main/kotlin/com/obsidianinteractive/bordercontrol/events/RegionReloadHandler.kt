
package com.obsidianinteractive.bordercontrol.events

import com.obsidianinteractive.bordercontrol.region.RegionConfig
import com.mojang.brigadier.Command
import net.minecraft.commands.Commands
import net.minecraft.network.chat.Component
import net.minecraftforge.event.RegisterCommandsEvent
import net.minecraftforge.eventbus.api.SubscribeEvent
import org.apache.logging.log4j.LogManager
import net.minecraft.server.packs.resources.ResourceManagerReloadListener
import net.minecraftforge.event.AddReloadListenerEvent
import net.minecraftforge.server.ServerLifecycleHooks

object RegionReloadHandler {

    private val LOGGER =
        LogManager.getLogger("BorderControl/Reload")

    @SubscribeEvent
    fun onRegisterCommands(event: RegisterCommandsEvent) {

        event.dispatcher.register(
            Commands.literal("bordercontrol")
                .requires { it.hasPermission(2) }
                .then(
                    Commands.literal("reload")
                        .then(
                            Commands.literal("regions")
                                .executes { context ->

                                    try {

                                        val regions =
                                            RegionConfig.load()

                                        RegionSyncHandler.syncAll(
                                            context.source.server
                                        )

                                        context.source.sendSuccess(
                                            {
                                                Component.literal(
                                                    "Reloaded ${regions.size} regions!"
                                                )
                                            },
                                            true
                                        )

                                        Command.SINGLE_SUCCESS

                                    } catch (exception: Exception) {

                                        LOGGER.error(
                                            "Failed to reload regions",
                                            exception
                                        )

                                        context.source.sendFailure(
                                            Component.literal(
                                                "Failed to reload regions. Check the server log."
                                            )
                                        )

                                        0
                                    }
                                }
                        )
                )
        )
    }

    @SubscribeEvent
    fun onAddReloadListener(
        event: AddReloadListenerEvent
    ) {

        event.addListener(
            ResourceManagerReloadListener {

                if (RegionConfig.isInitialized()) {

                    val regions = RegionConfig.load()

                    LOGGER.info(
                        "Reloaded {} regions during resource reload",
                        regions.size
                    )

                    val server =
                        ServerLifecycleHooks.getCurrentServer()

                    server?.execute {

                        if (RegionConfig.isInitialized()) {

                            RegionSyncHandler.syncAll(server)
                        }
                    }
                }
            }
        )
    }
}