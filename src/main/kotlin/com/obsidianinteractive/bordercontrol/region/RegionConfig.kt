
package com.obsidianinteractive.bordercontrol.region

import net.minecraft.server.MinecraftServer
import net.minecraft.world.level.storage.LevelResource
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.nio.file.AtomicMoveNotSupportedException
import java.util.UUID

object RegionConfig {

    @Volatile
    private var activePath: Path? = null

    @Volatile
    private var activeRegions: List<BorderRegion> =
        emptyList()

    private val path: Path
        get() = activePath
            ?: error("No active BorderControl world")

    @Synchronized
    fun initialize(
        server: MinecraftServer
    ): List<BorderRegion> {

        activeRegions = emptyList()

        activePath = server
            .getWorldPath(LevelResource.ROOT)
            .resolve("bordercontrol")
            .resolve("regions.j4s")

        return load()
    }

    @Synchronized
    fun shutdown() {

        activeRegions = emptyList()
        activePath = null
    }

    fun isInitialized(): Boolean {
        return activePath != null
    }

    fun getRegions(): List<BorderRegion> {
        return activeRegions.toList()
    }

    @Synchronized
    fun load(): List<BorderRegion> {

        Files.createDirectories(path.parent)

        if (Files.notExists(path)) {
            Files.writeString(path, "")
        }

        val source = Files.readString(path)

        val document = J4S.parse(source)

        val parsed = RegionCodec.decode(document)

        // Validate before replacing active regions.
        validate(parsed)

        activeRegions = parsed.toList()

        return getRegions()
    }

    @Synchronized
    fun add(region: BorderRegion) {

        val updated = activeRegions + region

        save(updated)
    }

    @Synchronized
    fun update(region: BorderRegion) {

        val index = activeRegions.indexOfFirst {

            it.owner == region.owner &&
                    it.id == region.id
        }

        require(index >= 0) {
            "Region ${region.owner}/${region.id} does not exist"
        }

        val updated =
            activeRegions.toMutableList()

        updated[index] = region

        save(updated)
    }

    @Synchronized
    fun remove(
        owner: String,
        id: Int
    ): Boolean {

        val updated = activeRegions.filterNot {

            it.owner == owner &&
                    it.id == id
        }

        if (updated.size == activeRegions.size) {
            return false
        }

        save(updated)

        return true
    }

    @Synchronized
    fun save(
        regions: List<BorderRegion> = activeRegions
    ) {

        validate(regions)

        Files.createDirectories(path.parent)

        val document = RegionCodec.encode(regions)

        val source = J4S.write(document)

        val temporary = Files.createTempFile(
            path.parent,
            "regions-",
            ".tmp"
        )

        try {

            Files.writeString(
                temporary,
                source
            )

            try {

                Files.move(
                    temporary,
                    path,
                    StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.ATOMIC_MOVE
                )

            } catch (
                _: AtomicMoveNotSupportedException
            ) {

                Files.move(
                    temporary,
                    path,
                    StandardCopyOption.REPLACE_EXISTING
                )
            }

            // Only publish after a successful save.
            activeRegions = regions.toList()

        } finally {

            Files.deleteIfExists(temporary)
        }
    }

    private fun validatePlayers(
        region: BorderRegion
    ) {

        val allowed = region.allowlist.map {
            UUID.fromString(it.uuid)
        }

        val blocked = region.blocklist.map {
            UUID.fromString(it.uuid)
        }

        require(
            allowed.size <= RegionSettings.MAX_PLAYERS
        ) {
            "Allowlist exceeds maximum player count"
        }

        require(
            blocked.size <= RegionSettings.MAX_PLAYERS
        ) {
            "Blocklist exceeds maximum player count"
        }

        require(
            allowed.size == allowed.toSet().size
        ) {
            "Duplicate allowlist entry"
        }

        require(
            blocked.size == blocked.toSet().size
        ) {
            "Duplicate blocklist entry"
        }

        require(
            allowed.toSet()
                .intersect(blocked.toSet())
                .isEmpty()
        ) {
            "A player cannot appear in both lists"
        }
    }

    private fun validate(
        regions: List<BorderRegion>
    ) {

        val identifiers =
            mutableSetOf<Pair<String, Int>>()

        for ((index, region) in regions.withIndex()) {

            require(region.id > 0) {
                "Region ID must be positive"
            }

            require(
                identifiers.add(
                    region.owner.lowercase() to region.id
                )
            ) {
                "Duplicate region ${region.owner}/${region.id}"
            }

            require(
                region.first.dimension ==
                        region.second.dimension
            ) {
                "Border blocks must be in the same dimension"
            }

            require(region.first != region.second) {
                "Region requires two different border blocks"
            }

            val width = kotlin.math.abs(
                region.first.x.toLong() -
                        region.second.x
            ) + 1L

            val length = kotlin.math.abs(
                region.first.z.toLong() -
                        region.second.z
            ) + 1L

            require(width > 0 && length > 0) {
                "Invalid region dimensions"
            }

            validatePlayers(region)

            require(
                region.teamPermissions.keys ==
                        TeamRole.entries.toSet()
            ) {
                "Incomplete team permissions"
            }

            // Check each region pair only once.
            for (otherIndex in index + 1 until regions.size) {

                val other = regions[otherIndex]

                require(
                    !RegionManager.overlaps(
                        region,
                        other
                    )
                ) {
                    "Regions ${region.owner}/${region.id} and " +
                            "${other.owner}/${other.id} overlap"
                }
            }
        }
    }
}