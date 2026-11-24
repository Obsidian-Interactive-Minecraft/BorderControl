package com.obsidianinteractive.bordercontrol.region

data class PlayerAccess(
    val username: String,
    val uuid: String,
    val build: Boolean? = null,
    val breakBlocks: Boolean? = null,
    val interact: Boolean? = null
)