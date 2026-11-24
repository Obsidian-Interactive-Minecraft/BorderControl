
package com.obsidianinteractive.bordercontrol.border

import net.minecraft.world.phys.Vec3

object BorderCollision {

    fun crosses(
        bounds: BorderBounds,
        from: Vec3,
        to: Vec3
    ): Boolean {

        val wasInside = bounds.contains(from)
        val isInside = bounds.contains(to)

        // Entering or leaving a region.
        if (wasInside != isInside) {
            return true
        }

        // Movement entirely inside the region.
        if (wasInside && isInside) {
            return false
        }

        // Check whether movement passes through
        // the region without ending inside it.
        var near = 0.0
        var far = 1.0

        fun clip(
            origin: Double,
            delta: Double,
            min: Double,
            max: Double
        ): Boolean {

            if (kotlin.math.abs(delta) < 0.0000001) {
                return origin >= min && origin < max
            }

            val first = (min - origin) / delta
            val second = (max - origin) / delta

            near = maxOf(near, minOf(first, second))
            far = minOf(far, maxOf(first, second))

            return near < far
        }

        return clip(
            from.x,
            to.x - from.x,
            bounds.minX,
            bounds.maxX
        ) && clip(
            from.z,
            to.z - from.z,
            bounds.minZ,
            bounds.maxZ
        )
    }
}