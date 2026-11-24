
package com.obsidianinteractive.bordercontrol.client.gui

import com.obsidianinteractive.bordercontrol.region.PlayerAccess
import com.obsidianinteractive.bordercontrol.region.RegionSettings
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.components.EditBox
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component

class PlayerListScreen(
    private val parent: RegionSettingsScreen,
    private val draft: RegionDraft,
    private val isBlocklist: Boolean
) : Screen(
    Component.literal(
        if (isBlocklist) {
            "Player Blocklist"
        } else {
            "Player Allowlist"
        }
    )
) {

    private lateinit var usernameBox: EditBox

    private var page = 0
    private var status = ""
    private var inputText = ""

    private val players: MutableList<PlayerAccess>
        get() = if (isBlocklist) {
            draft.blocklist
        } else {
            draft.allowlist
        }

    private val oppositeList: MutableList<PlayerAccess>
        get() = if (isBlocklist) {
            draft.allowlist
        } else {
            draft.blocklist
        }

    private val rowHeight: Int
        get() = if (isBlocklist) 28 else 48

    private val pageSize: Int
        get() = maxOf(
            1,
            (height - 132) / rowHeight
        )

    private val panelWidth: Int
        get() = minOf(300, width - 16)

    private val panelLeft: Int
        get() = (width - panelWidth) / 2

    private val panelRight: Int
        get() = panelLeft + panelWidth

    override fun init() {

        super.init()

        val centerX = width / 2

        val maxPage = maxOf(
            0,
            (players.size - 1) / pageSize
        )

        page = page.coerceIn(0, maxPage)

        usernameBox = EditBox(
            font,
            panelLeft,
            32,
            panelWidth - 62,
            20,
            Component.literal("Username")
        )

        usernameBox.setMaxLength(16)
        usernameBox.value = inputText

        usernameBox.setResponder {
            inputText = it
        }

        addRenderableWidget(usernameBox)

        addRenderableWidget(
            Button.builder(
                Component.literal("Add")
            ) {
                addPlayer()
            }
                .bounds(
                    panelRight - 58,
                    32,
                    58,
                    20
                )
                .build()
        )

        val start = page * pageSize

        val end = minOf(
            start + pageSize,
            players.size
        )

        for (index in start until end) {

            createPlayerRow(
                index,
                76 + (index - start) * rowHeight
            )
        }

        addRenderableWidget(
            Button.builder(
                Component.literal("<")
            ) {

                page--

                rebuildWidgets()
            }
                .bounds(
                    centerX - 100,
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
                    centerX + 60,
                    height - 52,
                    40,
                    20
                )
                .build()
        ).active = page < maxPage

        addRenderableWidget(
            Button.builder(
                Component.literal("Done")
            ) {
                onClose()
            }
                .bounds(
                    centerX - 100,
                    height - 26,
                    200,
                    20
                )
                .build()
        )
    }

    private fun createPlayerRow(
        index: Int,
        y: Int
    ) {

        addRenderableWidget(
            Button.builder(
                Component.literal("Remove")
            ) {

                players.removeAt(index)

                rebuildWidgets()
            }
                .bounds(
                    panelRight - 60,
                    y - 5,
                    60,
                    20
                )
                .build()
        )

        // Blocklisted players are denied all permissions.
        // Individual permission buttons are unnecessary.
        if (isBlocklist) return

        val buttonWidth =
            (panelWidth - 6) / 3

        addRenderableWidget(
            Button.builder(
                Component.literal(
                    "Build: ${permissionLabel(players[index].build)}"
                )
            ) { button ->

                val entry = players[index]

                val next = cycle(entry.build)

                players[index] =
                    entry.copy(build = next)

                button.message = Component.literal(
                    "Build: ${permissionLabel(next)}"
                )
            }
                .bounds(
                    panelLeft,
                    y + 17,
                    buttonWidth,
                    20
                )
                .build()
        )

        addRenderableWidget(
            Button.builder(
                Component.literal(
                    "Break: ${permissionLabel(players[index].breakBlocks)}"
                )
            ) { button ->

                val entry = players[index]

                val next = cycle(entry.breakBlocks)

                players[index] =
                    entry.copy(breakBlocks = next)

                button.message = Component.literal(
                    "Break: ${permissionLabel(next)}"
                )
            }
                .bounds(
                    panelLeft + buttonWidth + 3,
                    y + 17,
                    buttonWidth,
                    20
                )
                .build()
        )

        addRenderableWidget(
            Button.builder(
                Component.literal(
                    "Interact: ${permissionLabel(players[index].interact)}"
                )
            ) { button ->

                val entry = players[index]

                val next = cycle(entry.interact)

                players[index] =
                    entry.copy(interact = next)

                button.message = Component.literal(
                    "Interact: ${permissionLabel(next)}"
                )
            }
                .bounds(
                    panelLeft + (buttonWidth + 3) * 2,
                    y + 17,
                    buttonWidth,
                    20
                )
                .build()
        )
    }

    private fun addPlayer() {

        val username =
            usernameBox.value.trim()

        if (username.isEmpty()) {

            status = "Enter a username!"

            return
        }

        val connection =
            minecraft?.connection ?: return

        val profile = connection.onlinePlayers
            .firstOrNull {

                it.profile.name.equals(
                    username,
                    ignoreCase = true
                )
            }
            ?.profile

        if (profile == null) {

            status = "Player must be online!"

            return
        }

        if (profile.id == minecraft?.player?.uuid) {

            status = "You cannot add yourself!"

            return
        }

        val uuid = profile.id.toString()

        if (players.any {

                it.uuid.equals(
                    uuid,
                    ignoreCase = true
                )
            }
        ) {

            status = "Player is already listed!"

            return
        }

        if (oppositeList.any {

                it.uuid.equals(
                    uuid,
                    ignoreCase = true
                )
            }
        ) {

            status = if (isBlocklist) {
                "Remove player from allowlist first!"
            } else {
                "Remove player from blocklist first!"
            }

            return
        }

        if (
            players.size >= RegionSettings.MAX_PLAYERS
        ) {

            status = "Player list is full!"

            return
        }

        players.add(
            PlayerAccess(
                username = profile.name,
                uuid = uuid,
                build = null,
                breakBlocks = null,
                interact = null
            )
        )

        inputText = ""

        status = if (isBlocklist) {
            "Player blocklisted!"
        } else {
            "Player allowlisted!"
        }

        page = (players.size - 1) / pageSize

        rebuildWidgets()
    }

    private fun cycle(
        value: Boolean?
    ): Boolean? {

        return when (value) {

            null -> true

            true -> false

            false -> null
        }
    }

    private fun permissionLabel(
        value: Boolean?
    ): String {

        return when (value) {

            null -> "§7Inherit"

            true -> "§aAllow"

            false -> "§cDeny"
        }
    }

    override fun render(
        graphics: GuiGraphics,
        mouseX: Int,
        mouseY: Int,
        partialTick: Float
    ) {

        renderBackground(graphics)

        val centerX = width / 2

        val title = if (isBlocklist) {
            "Player Blocklist"
        } else {
            "Player Allowlist"
        }

        graphics.drawCenteredString(
            font,
            title,
            centerX,
            10,
            0xFFFFFF
        )

        graphics.drawCenteredString(
            font,
            "${players.size} players",
            centerX,
            21,
            0xAAAAAA
        )

        val start = page * pageSize

        val end = minOf(
            start + pageSize,
            players.size
        )

        for (index in start until end) {

            val y =
                76 + (index - start) * rowHeight

            graphics.fill(
                panelLeft - 4,
                y - 8,
                panelRight + 4,
                y + rowHeight - 9,
                0x55000000
            )

            graphics.drawString(
                font,
                players[index].username,
                panelLeft,
                y,
                if (isBlocklist) {
                    0xFF5555
                } else {
                    0x55FF55
                }
            )
        }

        if (players.isEmpty()) {

            graphics.drawCenteredString(
                font,
                "No players listed",
                centerX,
                height / 2,
                0xAAAAAA
            )
        }

        val totalPages = maxOf(
            1,
            (players.size + pageSize - 1) / pageSize
        )

        graphics.drawCenteredString(
            font,
            "${page + 1} / $totalPages",
            centerX,
            height - 46,
            0xFFFFFF
        )

        val description = if (status.isNotEmpty()) {

            status

        } else if (isBlocklist) {

            "Blocked players have no access."

        } else {

            "Click permissions to change them."
        }

        graphics.drawCenteredString(
            font,
            description,
            centerX,
            57,
            if (status.isNotEmpty()) {
                0xFFFF55
            } else {
                0xAAAAAA
            }
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