
package com.obsidianinteractive.bordercontrol.region

data class TeamAccess(
    val cross: Boolean? = null,
    val build: Boolean? = null,
    val breakBlocks: Boolean? = null,
    val interact: Boolean? = null
) {
    companion object {

        fun defaults(): Map<TeamRole, TeamAccess> {
            return TeamRole.entries.associateWith { role ->

                when (role) {

                    TeamRole.OWNER,
                    TeamRole.OFFICER,
                    TeamRole.MEMBER -> TeamAccess(
                        cross = true,
                        build = true,
                        breakBlocks = true,
                        interact = true
                    )

                    else -> TeamAccess()
                }
            }
        }
    }
}