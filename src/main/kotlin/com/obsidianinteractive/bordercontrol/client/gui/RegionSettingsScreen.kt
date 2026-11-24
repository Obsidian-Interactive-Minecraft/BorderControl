
package com.obsidianinteractive.bordercontrol.client.gui

import com.obsidianinteractive.bordercontrol.network.BorderNetwork
import com.obsidianinteractive.bordercontrol.network.OpenRegionPacket
import com.obsidianinteractive.bordercontrol.network.RegionSettingsResultPacket
import com.obsidianinteractive.bordercontrol.network.UpdateRegionPacket
import com.obsidianinteractive.bordercontrol.region.AccessMode
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component

class RegionSettingsScreen(
    private val draft: RegionDraft
) : Screen(Component.literal("BorderControl")) {

    constructor(packet: OpenRegionPacket) :
            this(RegionDraft(packet))

    private lateinit var saveButton: Button
    private lateinit var cancelButton: Button
    private lateinit var allowlistButton: Button
    private lateinit var blocklistButton: Button

    private var teamButton: Button? = null

    private val modeButtons =
        mutableMapOf<AccessMode, Button>()

    private var saving = false
    private var status = ""

    override fun init() {
        super.init()

        modeButtons.clear()
        teamButton = null

        val centerX = width / 2
        val startY = 64

        AccessMode.entries.forEachIndexed { index, mode ->

            val button = Button.builder(
                Component.literal(modeLabel(mode))
            ) {
                draft.mode = mode
                refreshButtons()
            }
                .bounds(
                    centerX - 120 + index * 80,
                    startY,
                    78,
                    20
                )
                .build()

            modeButtons[mode] =
                addRenderableWidget(button)
        }

        allowlistButton = addRenderableWidget(
            Button.builder(
                Component.literal("Allowlist")
            ) {
                minecraft?.setScreen(
                    PlayerListScreen(
                        this,
                        draft,
                        false
                    )
                )
            }
                .bounds(
                    centerX - 120,
                    startY + 30,
                    118,
                    20
                )
                .build()
        )

        blocklistButton = addRenderableWidget(
            Button.builder(
                Component.literal("Blocklist")
            ) {
                minecraft?.setScreen(
                    PlayerListScreen(
                        this,
                        draft,
                        true
                    )
                )
            }
                .bounds(
                    centerX + 2,
                    startY + 30,
                    118,
                    20
                )
                .build()
        )

        if (draft.teamsEnabled) {

            teamButton = addRenderableWidget(
                Button.builder(
                    Component.literal("Team Permissions")
                ) {
                    minecraft?.setScreen(
                        TeamPermissionsScreen(
                            this,
                            draft
                        )
                    )
                }
                    .bounds(
                        centerX - 120,
                        startY + 54,
                        240,
                        20
                    )
                    .build()
            )
        }

        saveButton = addRenderableWidget(
            Button.builder(
                Component.literal("Save Changes")
            ) {
                saveChanges()
            }
                .bounds(
                    centerX - 120,
                    height - 28,
                    118,
                    20
                )
                .build()
        )

        cancelButton = addRenderableWidget(
            Button.builder(
                Component.literal("Cancel")
            ) {
                onClose()
            }
                .bounds(
                    centerX + 2,
                    height - 28,
                    118,
                    20
                )
                .build()
        )

        refreshButtons()
    }

    private fun modeLabel(mode: AccessMode): String {

        val name = when (mode) {
            AccessMode.NOBODY -> "Nobody"
            AccessMode.PERMISSION -> "Permission"
            AccessMode.PUBLIC -> "Public"
        }

        return if (draft.mode == mode) {
            "> $name <"
        } else {
            name
        }
    }

    private fun refreshButtons() {

        for ((mode, button) in modeButtons) {

            button.message =
                Component.literal(modeLabel(mode))

            button.active = !saving
        }

        allowlistButton.message =
            Component.literal(
                "Allowlist (${draft.allowlist.size})"
            )

        blocklistButton.message =
            Component.literal(
                "Blocklist (${draft.blocklist.size})"
            )

        allowlistButton.active =
            !saving &&
                    draft.mode == AccessMode.PERMISSION

        blocklistButton.active =
            !saving &&
                    draft.mode != AccessMode.NOBODY

        teamButton?.active =
            !saving &&
                    draft.mode != AccessMode.NOBODY

        saveButton.active =
            !saving && draft.hasChanges()

        cancelButton.active = !saving
    }

    private fun saveChanges() {

        if (saving || !draft.hasChanges()) return

        saving = true
        status = "Saving region settings..."

        refreshButtons()

        BorderNetwork.CHANNEL.sendToServer(
            UpdateRegionPacket(
                owner = draft.original.owner,
                id = draft.original.id,
                mode = draft.mode,
                allowlist = draft.allowlist.toList(),
                blocklist = draft.blocklist.toList(),
                teamPermissions =
                    draft.teamPermissions.toMap()
            )
        )
    }

    fun handleResult(
        packet: RegionSettingsResultPacket
    ) {

        saving = false

        if (packet.success) {

            minecraft?.player?.sendSystemMessage(
                Component.literal(packet.message)
            )

            minecraft?.setScreen(null)

        } else {

            status = packet.message
            refreshButtons()
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

        graphics.drawCenteredString(
            font,
            "BorderControl",
            centerX,
            10,
            0xFFFFFF
        )

        graphics.drawCenteredString(
            font,
            "Region #${draft.original.id}",
            centerX,
            24,
            0x55FF55
        )

        graphics.drawCenteredString(
            font,
            "Owner: ${draft.original.owner}",
            centerX,
            36,
            0xAAAAAA
        )

        graphics.drawCenteredString(
            font,
            "Size: ${draft.original.width} x ${draft.original.length}",
            centerX,
            48,
            0xAAAAAA
        )

        if (status.isNotEmpty()) {

            graphics.drawCenteredString(
                font,
                status,
                centerX,
                height - 42,
                if (saving) 0xFFFF55 else 0xFF5555
            )
        }

        super.render(
            graphics,
            mouseX,
            mouseY,
            partialTick
        )
    }

    override fun onClose() {
        if (!saving) {
            minecraft?.setScreen(null)
        }
    }

    override fun isPauseScreen(): Boolean = false
}