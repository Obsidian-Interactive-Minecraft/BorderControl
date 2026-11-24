
package com.obsidianinteractive.bordercontrol.region

data class BorderRegion(
    val owner: String,
    val id: Int,
    val first: BorderPoint,
    val second: BorderPoint,
    val mode: AccessMode,
    val allowlist: List<PlayerAccess> = emptyList(),
    val blocklist: List<PlayerAccess> = emptyList(),
    val teamPermissions: Map<TeamRole, TeamAccess> =
        TeamAccess.defaults()
)