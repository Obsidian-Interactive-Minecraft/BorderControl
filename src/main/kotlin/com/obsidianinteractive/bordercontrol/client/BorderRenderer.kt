
package com.obsidianinteractive.bordercontrol.client

import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.BufferBuilder
import com.mojang.blaze3d.vertex.DefaultVertexFormat
import com.mojang.blaze3d.vertex.Tesselator
import com.mojang.blaze3d.vertex.VertexFormat
import com.obsidianinteractive.bordercontrol.BorderControl
import net.minecraft.Util
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.GameRenderer
import net.minecraft.resources.ResourceLocation
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.client.event.RenderLevelStageEvent
import net.minecraftforge.client.event.ClientPlayerNetworkEvent
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod
import org.joml.Matrix4f
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

object BorderRenderer {

    private var firstRender = true


    private val LOGGER =
        org.apache.logging.log4j.LogManager.getLogger(
            "BorderControl/Renderer"
        )

    private val detectedStages = mutableSetOf<String>()
    private var lastDebugTime = 0L

    private val TEXTURE = ResourceLocation(
        "minecraft",
        "textures/misc/forcefield.png"
    )

    private const val RENDER_DISTANCE = 64.0
    private const val TEXTURE_SCALE = 0.5f
    private const val ALPHA = 100

    fun onRender(event: RenderLevelStageEvent) {

        if (firstRender) {
            firstRender = false

            LOGGER.info(
                "FIRST RENDER EVENT RECEIVED! Stage: {}",
                event.stage
            )
        }

        val stage = event.stage.toString()

        if (detectedStages.add(stage)) {
            LOGGER.info("Detected render stage: {}", stage)
        }

        if (event.stage !=
            RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS
        ) {
            return
        }

        val minecraft = Minecraft.getInstance()
        val level = minecraft.level ?: return

        val camera = event.camera.position

        val dimension = level.dimension()
            .location().toString()

        val regions = ClientRegions.getNearbyRegions(
            dimension,
            camera.x,
            camera.z,
            RENDER_DISTANCE
        )

        val now = Util.getMillis()

        if (now - lastDebugTime >= 5000L) {
            lastDebugTime = now

            LOGGER.info(
                "Rendering: dimension={}, cached={}, nearby={}",
                dimension,
                ClientRegions.getRegions().size,
                regions.size
            )
        }

        if (regions.isEmpty()) return

        val minY = max(
            level.minBuildHeight.toDouble(),
            camera.y - RENDER_DISTANCE
        )

        val maxY = min(
            level.maxBuildHeight.toDouble(),
            camera.y + RENDER_DISTANCE
        )

        if (minY >= maxY) return

        val animation =
            (Util.getMillis() % 3000L) / 3000.0f

        val poseStack = event.poseStack

        poseStack.pushPose()

        try {
            poseStack.translate(
                -camera.x,
                -camera.y,
                -camera.z
            )

            val matrix = poseStack.last().pose()

            RenderSystem.enableBlend()
            RenderSystem.defaultBlendFunc()
            RenderSystem.disableCull()
            RenderSystem.enableDepthTest()
            RenderSystem.depthMask(false)

            RenderSystem.setShader(
                GameRenderer::getPositionColorTexShader
            )

            RenderSystem.setShaderTexture(0, TEXTURE)
            RenderSystem.setShaderColor(
                1f, 1f, 1f, 1f
            )

            val tesselator = Tesselator.getInstance()
            val buffer = tesselator.builder

            buffer.begin(
                VertexFormat.Mode.QUADS,
                DefaultVertexFormat.POSITION_COLOR_TEX
            )

            var renderedWalls = 0

            for (region in regions) {

                val bounds = region.bounds

                val red = if (region.allowed) 0 else 255
                val green = if (region.allowed) 255 else 0

                val x1 = bounds.minX
                val x2 = bounds.maxX
                val z1 = bounds.minZ
                val z2 = bounds.maxZ

                // North wall
                if (abs(camera.z - z1) <= RENDER_DISTANCE) {
                    wall(
                        buffer, matrix,
                        x1, z1, x2, z1,
                        minY, maxY,
                        red, green, animation
                    )
                    renderedWalls++
                }

                // South wall
                if (abs(camera.z - z2) <= RENDER_DISTANCE) {
                    wall(
                        buffer, matrix,
                        x2, z2, x1, z2,
                        minY, maxY,
                        red, green, animation
                    )
                    renderedWalls++
                }

                // West wall
                if (abs(camera.x - x1) <= RENDER_DISTANCE) {
                    wall(
                        buffer, matrix,
                        x1, z2, x1, z1,
                        minY, maxY,
                        red, green, animation
                    )
                    renderedWalls++
                }

                // East wall
                if (abs(camera.x - x2) <= RENDER_DISTANCE) {
                    wall(
                        buffer, matrix,
                        x2, z1, x2, z2,
                        minY, maxY,
                        red, green, animation
                    )
                    renderedWalls++
                }
            }

            if (renderedWalls > 0) {
                tesselator.end()
            } else {
                buffer.end().release()
            }

        } finally {
            RenderSystem.depthMask(true)
            RenderSystem.enableCull()
            RenderSystem.disableBlend()

            RenderSystem.setShaderColor(
                1f, 1f, 1f, 1f
            )

            poseStack.popPose()
        }
    }

    private fun wall(
        buffer: BufferBuilder,
        matrix: Matrix4f,
        x1: Double,
        z1: Double,
        x2: Double,
        z2: Double,
        minY: Double,
        maxY: Double,
        red: Int,
        green: Int,
        animation: Float
    ) {

        val width =
            abs(x2 - x1) + abs(z2 - z1)

        val u1 = animation
        val u2 = u1 + width.toFloat() * TEXTURE_SCALE

        val v1 =
            minY.toFloat() * TEXTURE_SCALE + animation

        val v2 =
            maxY.toFloat() * TEXTURE_SCALE + animation

        buffer.vertex(
            matrix,
            x1.toFloat(),
            minY.toFloat(),
            z1.toFloat()
        )
            .color(red, green, 0, ALPHA)
            .uv(u1, v1)
            .endVertex()

        buffer.vertex(
            matrix,
            x2.toFloat(),
            minY.toFloat(),
            z2.toFloat()
        )
            .color(red, green, 0, ALPHA)
            .uv(u2, v1)
            .endVertex()

        buffer.vertex(
            matrix,
            x2.toFloat(),
            maxY.toFloat(),
            z2.toFloat()
        )
            .color(red, green, 0, ALPHA)
            .uv(u2, v2)
            .endVertex()

        buffer.vertex(
            matrix,
            x1.toFloat(),
            maxY.toFloat(),
            z1.toFloat()
        )
            .color(red, green, 0, ALPHA)
            .uv(u1, v2)
            .endVertex()
    }

    fun onDisconnect(
        event: ClientPlayerNetworkEvent.LoggingOut
    ) {
        ClientRegions.clear()
    }
}