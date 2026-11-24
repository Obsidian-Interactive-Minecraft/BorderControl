
package com.obsidianinteractive.bordercontrol.client.gui

import com.obsidianinteractive.bordercontrol.region.TeamAccess
import com.obsidianinteractive.bordercontrol.region.TeamRole
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component

class TeamPermissionsScreen(
    private val parent: RegionSettingsScreen,
    private val draft: RegionDraft
) : Screen(Component.literal("Team Permissions")) {

    private var page = 0

    private val rolesPerPage: Int
        get() = maxOf(1, (height - 100) / 24)

    private val actions = listOf(
        "Cross",
        "Build",
        "Break",
        "Interact"
    )

    override fun init() {
        super.init()

        val centerX = width / 2
        val roles = TeamRole.entries

        val maxPage =
            (roles.size - 1) / rolesPerPage

        page = page.coerceIn(0, maxPage)

        val start = page * rolesPerPage

        val end = minOf(
            start + rolesPerPage,
            roles.size
        )

        for (index in start until end) {

            val role = roles[index]
            val y = 46 + (index - start) * 24

            actions.forEachIndexed { action, _ ->

                val access = getAccess(role)
                val value = getPermission(access, action)

                addRenderableWidget(
                    Button.builder(
                        permissionLabel(value)
                    ) { button ->

                        val current = getAccess(role)

                        val next = cycle(
                            getPermission(
                                current,
                                action
                            )
                        )

                        draft.teamPermissions[role] =
                            setPermission(
                                current,
                                action,
                                next
                            )

                        button.message =
                            permissionLabel(next)
                    }
                        .bounds(
                            centerX - 86 + action * 60,
                            y,
                            58,
                            20
                        )
                        .build()
                )
            }
        }

        addRenderableWidget(
            Button.builder(
                Component.literal("<")
            ) {
                page--
                rebuildWidgets()
            }
                .bounds(
                    centerX - 150,
                    height - 52,
                    40,
                    20
                )
                .build()
        ).active = page > 0

        addRenderableWidget(
            Button.builder(
                Component.literal(">")
            ) {
                page++
                rebuildWidgets()
            }
                .bounds(
                    centerX + 110,
                    height - 52,
                    40,
                    20
                )
                .build()
        ).active = page < maxPage

        addRenderableWidget(
            Button.builder(
                Component.literal("Reset Defaults")
            ) {

                draft.teamPermissions.clear()

                draft.teamPermissions.putAll(
                    TeamAccess.defaults()
                )

                rebuildWidgets()
            }
                .bounds(
                    centerX - 150,
                    height - 26,
                    148,
                    20
                )
                .build()
        )

        addRenderableWidget(
            Button.builder(
                Component.literal("Done")
            ) {
                onClose()
            }
                .bounds(
                    centerX + 2,
                    height - 26,
                    148,
                    20
                )
                .build()
        )
    }

    private fun getAccess(role: TeamRole): TeamAccess {

        return draft.teamPermissions[role]
            ?: TeamAccess.defaults().getValue(role)
    }

    private fun getPermission(
        access: TeamAccess,
        action: Int
    ): Boolean? {

        return when (action) {
            0 -> access.cross
            1 -> access.build
            2 -> access.breakBlocks
            3 -> access.interact
            else -> error("Unknown permission")
        }
    }

    private fun setPermission(
        access: TeamAccess,
        action: Int,
        value: Boolean?
    ): TeamAccess {

        return when (action) {
            0 -> access.copy(cross = value)
            1 -> access.copy(build = value)
            2 -> access.copy(breakBlocks = value)
            3 -> access.copy(interact = value)
            else -> error("Unknown permission")
        }
    }

    private fun cycle(value: Boolean?): Boolean? {

        return when (value) {
            null -> true
            true -> false
            false -> null
        }
    }

    private fun permissionLabel(
        value: Boolean?
    ): Component {

        return Component.literal(
            when (value) {
                null -> "§7Inherit"
                true -> "§aAllow"
                false -> "§cDeny"
            }
        )
    }

    override fun render(
        graphics: GuiGraphics,
        mouseX: Int,
        mouseY: Int,
        partialTick: Float
    ) {

        renderBackground(graphics)

        val centerX = width / 2
        val roles = TeamRole.entries

        graphics.drawCenteredString(
            font,
            "FTB Team Permissions",
            centerX,
            12,
            0xFFFFFF
        )

        actions.forEachIndexed { index, action ->

            graphics.drawCenteredString(
                font,
                action,
                centerX - 57 + index * 60,
                32,
                0xAAAAAA
            )
        }

        val start = page * rolesPerPage

        val end = minOf(
            start + rolesPerPage,
            roles.size
        )

        for (index in start until end) {

            val role = roles[index]

            val y = 46 + (index - start) * 24

            val name = role.name
                .lowercase()
                .replaceFirstChar {
                    it.uppercase()
                }

            graphics.drawString(
                font,
                name,
                centerX - 150,
                y + 6,
                0xFFFFFF
            )
        }

        val totalPages =
            (roles.size + rolesPerPage - 1) /
                    rolesPerPage

        graphics.drawCenteredString(
            font,
            "${page + 1} / $totalPages",
            centerX,
            height - 46,
            0xFFFFFF
        )

        super.render(
            graphics,
            mouseX,
            mouseY,
            partialTick
        )
    }

    override fun onClose() {
        minecraft?.setScreen(parent)
    }

    override fun isPauseScreen(): Boolean = false
}