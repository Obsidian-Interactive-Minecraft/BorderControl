
package com.obsidianinteractive.bordercontrol.border

import com.obsidianinteractive.bordercontrol.region.BorderRegion
import net.minecraft.core.BlockPos
import net.minecraft.world.phys.Vec3
import kotlin.math.min
import kotlin.math.max

data class BorderBounds(
    val minX: Double,
    val minZ: Double,
    val maxX: Double,
    val maxZ: Double,
    val dimension: String
) {

    val width: Double
        get() = maxX - minX

    val length: Double
        get() = maxZ - minZ

    val centerX: Double
        get() = (minX + maxX) / 2.0

    val centerZ: Double
        get() = (minZ + maxZ) / 2.0

    fun contains(x: Double, z: Double): Boolean {
        return x >= minX &&
                x < maxX &&
                z >= minZ &&
                z < maxZ
    }

    fun contains(pos: BlockPos): Boolean {
        return contains(
            pos.x.toDouble(),
            pos.z.toDouble()
        )
    }

    fun contains(pos: Vec3): Boolean {
        return contains(pos.x, pos.z)
    }

    fun distanceSquared(x: Double, z: Double): Double {
        val dx = max(
            max(minX - x, x - maxX),
            0.0
        )

        val dz = max(
            max(minZ - z, z - maxZ),
            0.0
        )

        return dx * dx + dz * dz
    }

    companion object {

        fun fromRegion(region: BorderRegion): BorderBounds {
            return BorderBounds(
                minX = min(
                    region.first.x,
                    region.second.x
                ).toDouble(),

                minZ = min(
                    region.first.z,
                    region.second.z
                ).toDouble(),

                maxX = max(
                    region.first.x,
                    region.second.x
                ).toDouble() + 1.0,

                maxZ = max(
                    region.first.z,
                    region.second.z
                ).toDouble() + 1.0,

                dimension = region.first.dimension
            )
        }
    }
}