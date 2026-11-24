
package com.obsidianinteractive.bordercontrol.region

import com.obsidianinteractive.bordercontrol.config.Config
import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerPlayer
import kotlin.math.abs

object RegionManager {

    fun create(
        player: ServerPlayer,
        first: BorderPoint,
        second: BorderPoint
    ): BorderRegion {

        require(first.dimension == second.dimension) {
            "Border blocks must be in the same dimension!"
        }

        require(first != second) {
            "Two different border blocks are required!"
        }

        val width = abs(first.x.toLong() - second.x) + 1
        val length = abs(first.z.toLong() - second.z) + 1

        val maxSize = Config.maxBorderSize.toLong()

        require(width <= maxSize && length <= maxSize) {
            "Region exceeds maximum size of ${maxSize}x${maxSize}!"
        }

        val owner = player.gameProfile.name

        // Read the latest region database.
        RegionConfig.load()

        val regions = RegionConfig.getRegions()

        val id = (regions
            .filter { it.owner == owner }
            .maxOfOrNull { it.id } ?: 0) + 1

        val region = BorderRegion(
            owner = owner,
            id = id,
            first = first,
            second = second,
            mode = AccessMode.NOBODY,
            allowlist = emptyList(),
            blocklist = emptyList()
        )

        require(regions.none { overlaps(it, region) }) {
            "This region overlaps an existing region!"
        }

        RegionConfig.add(region)
        RegionConfig.load()

        return region
    }

    fun overlaps(
        first: BorderRegion,
        second: BorderRegion
    ): Boolean {

        if (first.first.dimension != second.first.dimension) {
            return false
        }

        return minOf(first.first.x, first.second.x) <=
                maxOf(second.first.x, second.second.x) &&

                maxOf(first.first.x, first.second.x) >=
                minOf(second.first.x, second.second.x) &&

                minOf(first.first.z, first.second.z) <=
                maxOf(second.first.z, second.second.z) &&

                maxOf(first.first.z, first.second.z) >=
                minOf(second.first.z, second.second.z)
    }

    fun contains(
        region: BorderRegion,
        pos: BlockPos,
        dimension: String
    ): Boolean {

        if (region.first.dimension != dimension) {
            return false
        }

        return pos.x in
                minOf(region.first.x, region.second.x)..
                maxOf(region.first.x, region.second.x) &&

                pos.z in
                minOf(region.first.z, region.second.z)..
                maxOf(region.first.z, region.second.z)
    }

    fun getRegionAt(
        pos: BlockPos,
        dimension: String
    ): BorderRegion? {

        return RegionConfig.getRegions().firstOrNull {
            contains(it, pos, dimension)
        }
    }

    fun getRegionsByOwner(owner: String): List<BorderRegion> {
        return RegionConfig.getRegions().filter {
            it.owner == owner
        }
    }

    fun getRegion(owner: String, id: Int): BorderRegion? {
        return RegionConfig.getRegions().firstOrNull {
            it.owner == owner && it.id == id
        }
    }

    fun delete(owner: String, id: Int): Boolean {
        return RegionConfig.remove(owner, id)
    }
}