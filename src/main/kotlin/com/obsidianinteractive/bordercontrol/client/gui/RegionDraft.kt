
package com.obsidianinteractive.bordercontrol.client.gui

import com.obsidianinteractive.bordercontrol.network.OpenRegionPacket
import com.obsidianinteractive.bordercontrol.region.AccessMode

class RegionDraft(
    val original: OpenRegionPacket
) {

    var mode: AccessMode = original.mode

    val allowlist =
        original.allowlist.toMutableList()

    val blocklist =
        original.blocklist.toMutableList()

    val teamPermissions =
        original.teamPermissions.toMutableMap()

    val teamsEnabled: Boolean
        get() = original.teamsEnabled

    fun hasChanges(): Boolean {

        return mode != original.mode ||
                allowlist != original.allowlist ||
                blocklist != original.blocklist ||
                teamPermissions != original.teamPermissions
    }
}