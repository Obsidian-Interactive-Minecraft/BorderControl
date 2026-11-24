
package com.obsidianinteractive.bordercontrol.region

import com.obsidianinteractive.bordercontrol.border.BorderAccess
import net.minecraft.server.level.ServerPlayer
import java.util.UUID
import com.obsidianinteractive.bordercontrol.integration.ftb.FTBCompat

object RegionSettings {

    const val MAX_PLAYERS = 256

    private val usernamePattern =
        Regex("[A-Za-z0-9_]{1,16}")

    private fun validatePlayers(
        player: ServerPlayer,
        region: BorderRegion,
        entries: List<PlayerAccess>,
        listName: String
    ): List<PlayerAccess> {

        require(entries.size <= MAX_PLAYERS) {
            "Too many entries in $listName!"
        }

        val existingPlayers =
            region.allowlist + region.blocklist

        val validated = entries.map { entry ->

            val uuid = UUID.fromString(entry.uuid)

            require(uuid != player.uuid) {
                "You cannot add yourself to $listName!"
            }

            require(entry.username.matches(usernamePattern)) {
                "Invalid Minecraft username!"
            }

            val onlinePlayer =
                player.server.playerList.getPlayer(uuid)

            val existingPlayer =
                existingPlayers.firstOrNull {
                    it.uuid.equals(
                        uuid.toString(),
                        ignoreCase = true
                    )
                }

            val username =
                onlinePlayer?.gameProfile?.name
                    ?: existingPlayer?.username
                    ?: error("New players must be online!")

            entry.copy(
                username = username,
                uuid = uuid.toString()
            )
        }

        require(
            validated.map { it.uuid }.distinct().size ==
                    validated.size
        ) {
            "Duplicate entries in $listName!"
        }

        return validated
    }

    fun update(
        player: ServerPlayer,
        owner: String,
        id: Int,
        mode: AccessMode,
        allowlist: List<PlayerAccess>,
        blocklist: List<PlayerAccess>,
        teamPermissions: Map<TeamRole, TeamAccess>
    ): BorderRegion {

        val region = RegionManager.getRegion(owner, id)
            ?: error("Region does not exist!")

        require(BorderAccess.isOwner(region, player)) {
            "You do not own this region!"
        }

        val validatedAllowlist = validatePlayers(
            player,
            region,
            allowlist,
            "allowlist"
        )

        val validatedBlocklist = validatePlayers(
            player,
            region,
            blocklist,
            "blocklist"
        )

        val allowedUUIDs =
            validatedAllowlist.map { it.uuid }.toSet()

        val blockedUUIDs =
            validatedBlocklist.map { it.uuid }.toSet()

        require(
            allowedUUIDs.intersect(blockedUUIDs).isEmpty()
        ) {
            "A player cannot appear in both lists!"
        }

        val validatedTeamPermissions =
            if (FTBCompat.isTeamsLoaded()) {

                require(
                    teamPermissions.keys ==
                            TeamRole.entries.toSet()
                ) {
                    "Incomplete team permissions!"
                }

                teamPermissions.toMap()

            } else {

                // Preserve stored settings when FTB Teams
                // is absent or disabled.
                region.teamPermissions
            }

        val updated = region.copy(
            mode = mode,
            allowlist = validatedAllowlist,
            blocklist = validatedBlocklist,
            teamPermissions = validatedTeamPermissions
        )

        RegionConfig.update(updated)

        return updated
    }
}