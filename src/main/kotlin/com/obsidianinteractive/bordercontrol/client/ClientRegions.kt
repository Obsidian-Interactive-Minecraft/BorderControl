
package com.obsidianinteractive.bordercontrol.client

import com.obsidianinteractive.bordercontrol.border.BorderBounds
import org.apache.logging.log4j.LogManager

object ClientRegions {

    private val LOGGER =
        LogManager.getLogger("BorderControl/ClientRegions")

    data class RenderRegion(
        val owner: String,
        val id: Int,
        val bounds: BorderBounds,
        val allowed: Boolean
    ) {
        val color: Int
            get() = if (allowed) {
                0x00FF00
            } else {
                0xFF0000
            }
    }

    @Volatile
    private var regions: List<RenderRegion> =
        emptyList()

    fun update(newRegions: List<RenderRegion>) {
        regions = newRegions.toList()

        LOGGER.info(
            "Received {} client regions",
            regions.size
        )
    }

    fun getRegions(): List<RenderRegion> {
        return regions
    }

    fun getNearbyRegions(
        dimension: String,
        x: Double,
        z: Double,
        renderDistance: Double
    ): List<RenderRegion> {

        val maxDistanceSquared =
            renderDistance * renderDistance

        return regions.filter {
            it.bounds.dimension == dimension &&
                    it.bounds.distanceSquared(x, z) <=
                    maxDistanceSquared
        }
    }

    fun clear() {
        regions = emptyList()

        LOGGER.info("Client region cache cleared")
    }
}