package com.example.game.engine

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import com.example.core.model.DayPhase
import com.example.core.model.DinosaurSpecies
import com.example.core.model.GrowthStage
import com.example.core.model.WeatherType
import kotlin.math.*

object GameWorldRenderer {

    fun draw(
        drawScope: DrawScope,
        engine: GameWorldEngine,
        activeSpecies: DinosaurSpecies,
        activeStage: GrowthStage
    ) {
        val width = drawScope.size.width
        val height = drawScope.size.height
        val dayPhase = engine.getCurrentDayPhase()

        // 1. Sky & Horizon Gradient
        drawSky(drawScope, width, height, dayPhase)

        // 2. Parallax Distant Mountains
        drawDistantMountains(drawScope, width, height, engine.cameraX)

        // 3. Gameplay World (Camera transform)
        val centerX = width / 2f
        val centerY = height / 2f
        val camX = engine.cameraX - engine.screenShake.offsetX
        val camY = engine.cameraY - engine.screenShake.offsetY

        // Draw Ground & Terrain
        drawGround(drawScope, camX, camY, centerX, centerY, engine.worldWidth, engine.worldHeight)

        // Draw River Stream
        drawRiver(drawScope, camX, camY, centerX, centerY, engine.walkAnimTime)

        // Draw Resource Nodes
        engine.resourceNodes.forEach { node ->
            drawResourceNode(drawScope, node, camX, camY, centerX, centerY)
        }

        // Draw Wildlife Entities
        engine.wildlifeList.forEach { w ->
            drawWildlife(drawScope, w, camX, camY, centerX, centerY)
        }

        // Draw Player Dinosaur
        drawPlayer(
            drawScope = drawScope,
            engine = engine,
            species = activeSpecies,
            stage = activeStage,
            camX = camX,
            camY = camY,
            centerX = centerX,
            centerY = centerY
        )

        // Draw Particles
        engine.particles.forEach { p ->
            val screenX = p.x - camX + centerX
            val screenY = p.y - camY + centerY
            if (screenX in -20f..width + 20f && screenY in -20f..height + 20f) {
                drawScope.drawCircle(
                    color = Color(p.colorHex).copy(alpha = p.alpha),
                    radius = p.size,
                    center = Offset(screenX, screenY)
                )
            }
        }

        // 4. Volcanic / Weather Ambient Overlay
        drawWeatherOverlay(drawScope, width, height, engine)
    }

    private fun drawSky(drawScope: DrawScope, w: Float, h: Float, phase: DayPhase) {
        val (topColor, bottomColor) = when (phase) {
            DayPhase.DAWN -> Pair(Color(0xFF2C1654), Color(0xFFFF9E80))
            DayPhase.MORNING -> Pair(Color(0xFF29B6F6), Color(0xFFE1F5FE))
            DayPhase.NOON -> Pair(Color(0xFF0288D1), Color(0xFF81D4FA))
            DayPhase.SUNSET -> Pair(Color(0xFF4A148C), Color(0xFFFF6F00))
            DayPhase.NIGHT -> Pair(Color(0xFF0A0E1A), Color(0xFF1A233A))
        }

        drawScope.drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(topColor, bottomColor),
                startY = 0f,
                endY = h * 0.45f
            ),
            size = Size(w, h * 0.45f)
        )

        // Celestial body (Sun or Moon)
        if (phase == DayPhase.NIGHT) {
            // Moon
            drawScope.drawCircle(
                color = Color(0xFFFFF9C4),
                radius = 24f,
                center = Offset(w * 0.8f, h * 0.12f)
            )
            // Stars
            for (i in 0..15) {
                val sx = (w * ((i * 37) % 100) / 100f)
                val sy = (h * 0.35f * ((i * 53) % 100) / 100f)
                drawScope.drawCircle(color = Color.White.copy(alpha = 0.8f), radius = 2f, center = Offset(sx, sy))
            }
        } else {
            // Sun with amber glow
            drawScope.drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFFFFEE58), Color(0xFFFF9800).copy(alpha = 0.2f), Color.Transparent),
                    center = Offset(w * 0.25f, h * 0.12f),
                    radius = 80f
                ),
                radius = 80f,
                center = Offset(w * 0.25f, h * 0.12f)
            )
        }
    }

    private fun drawDistantMountains(drawScope: DrawScope, w: Float, h: Float, camX: Float) {
        val horizonY = h * 0.45f
        val parallaxX = (camX * 0.08f) % (w * 1.5f)

        // Mountain silhouettes
        val mountainPath = Path().apply {
            moveTo(-100f, horizonY)
            var curX = -100f - parallaxX
            val peakCount = 10
            val segW = (w * 2f) / peakCount
            for (i in 0..peakCount + 3) {
                val peakH = if (i % 2 == 0) 90f else 55f
                val nextX = curX + segW
                lineTo(curX + segW * 0.5f, horizonY - peakH)
                lineTo(nextX, horizonY)
                curX = nextX
            }
            lineTo(w + 100f, horizonY)
            close()
        }

        drawScope.drawPath(mountainPath, color = Color(0xFF142B24).copy(alpha = 0.85f))
    }

    private fun drawGround(
        drawScope: DrawScope,
        camX: Float,
        camY: Float,
        centerX: Float,
        centerY: Float,
        worldW: Float,
        worldH: Float
    ) {
        val left = -camX + centerX
        val top = -camY + centerY

        // Main grass soil
        drawScope.drawRect(
            color = Color(0xFF1B3D30),
            topLeft = Offset(left, top),
            size = Size(worldW, worldH)
        )

        // Ground grid & mossy texture tiles
        val tileSize = 160f
        val startTileX = ((camX - centerX) / tileSize).toInt().coerceAtLeast(0)
        val endTileX = ((camX + centerX) / tileSize + 1).toInt().coerceAtMost((worldW / tileSize).toInt())
        val startTileY = ((camY - centerY) / tileSize).toInt().coerceAtLeast(0)
        val endTileY = ((camY + centerY) / tileSize + 1).toInt().coerceAtMost((worldH / tileSize).toInt())

        for (tx in startTileX..endTileX) {
            for (ty in startTileY..endTileY) {
                val tileScreenX = tx * tileSize - camX + centerX
                val tileScreenY = ty * tileSize - camY + centerY
                val isAlt = (tx + ty) % 2 == 0
                val tileColor = if (isAlt) Color(0xFF1E4436) else Color(0xFF18382C)
                drawScope.drawRect(
                    color = tileColor,
                    topLeft = Offset(tileScreenX, tileScreenY),
                    size = Size(tileSize, tileSize)
                )
                // Ancient grass patches
                if ((tx * 7 + ty * 13) % 5 == 0) {
                    drawScope.drawCircle(
                        color = Color(0xFF295744),
                        radius = 18f,
                        center = Offset(tileScreenX + 40f, tileScreenY + 40f)
                    )
                }
            }
        }

        // World borders
        drawScope.drawRect(
            color = Color(0xFF0B1914),
            topLeft = Offset(left - 200f, top - 200f),
            size = Size(worldW + 400f, 200f)
        )
        drawScope.drawRect(
            color = Color(0xFF0B1914),
            topLeft = Offset(left - 200f, top + worldH),
            size = Size(worldW + 400f, 200f)
        )
        drawScope.drawRect(
            color = Color(0xFF0B1914),
            topLeft = Offset(left - 200f, top),
            size = Size(200f, worldH)
        )
        drawScope.drawRect(
            color = Color(0xFF0B1914),
            topLeft = Offset(left + worldW, top),
            size = Size(200f, worldH)
        )
    }

    private fun drawRiver(
        drawScope: DrawScope,
        camX: Float,
        camY: Float,
        centerX: Float,
        centerY: Float,
        animTime: Float
    ) {
        val riverX = 1100f - camX + centerX
        val riverW = 140f

        // Winding water strip
        drawScope.drawRect(
            brush = Brush.horizontalGradient(
                colors = listOf(Color(0xFF00796B), Color(0xFF00ACC1), Color(0xFF00796B)),
                startX = riverX,
                endX = riverX + riverW
            ),
            topLeft = Offset(riverX, -camY + centerY),
            size = Size(riverW, 1600f)
        )

        // Water shimmers
        for (i in 0..12) {
            val sy = (i * 120f + (animTime * 20f) % 120f) - camY + centerY
            drawScope.drawLine(
                color = Color.White.copy(alpha = 0.4f),
                start = Offset(riverX + 25f + sin(animTime + i) * 15f, sy),
                end = Offset(riverX + 85f + sin(animTime + i) * 15f, sy),
                strokeWidth = 3f
            )
        }
    }

    private fun drawResourceNode(
        drawScope: DrawScope,
        node: ResourceNode,
        camX: Float,
        camY: Float,
        centerX: Float,
        centerY: Float
    ) {
        val sx = node.x - camX + centerX
        val sy = node.y - camY + centerY

        if (node.isDepleted) {
            // Depleted stump or gray stone
            drawScope.drawCircle(color = Color(0xFF37474F).copy(alpha = 0.5f), radius = 16f, center = Offset(sx, sy))
            return
        }

        when (node.type) {
            ResourceNodeType.BERRY_BUSH -> {
                // Lush bush foliage
                drawScope.drawCircle(color = Color(0xFF2E7D32), radius = 24f, center = Offset(sx, sy))
                drawScope.drawCircle(color = Color(0xFF388E3C), radius = 18f, center = Offset(sx - 8f, sy - 6f))
                // Bright berry clusters
                drawScope.drawCircle(color = Color(0xFFE91E63), radius = 5f, center = Offset(sx - 6f, sy - 4f))
                drawScope.drawCircle(color = Color(0xFFE91E63), radius = 5f, center = Offset(sx + 8f, sy - 2f))
                drawScope.drawCircle(color = Color(0xFFE91E63), radius = 5f, center = Offset(sx + 2f, sy + 8f))
            }
            ResourceNodeType.SPRING_POOL -> {
                // Water ripples
                drawScope.drawCircle(color = Color(0xFF00E5FF).copy(alpha = 0.6f), radius = 35f, center = Offset(sx, sy))
                drawScope.drawCircle(color = Color(0xFF0097A7), radius = 26f, center = Offset(sx, sy))
                drawScope.drawCircle(color = Color.White.copy(alpha = 0.7f), radius = 8f, center = Offset(sx, sy))
            }
            ResourceNodeType.PETRIFIED_LOG -> {
                drawScope.drawRoundRect(
                    color = Color(0xFF5D4037),
                    topLeft = Offset(sx - 28f, sy - 14f),
                    size = Size(56f, 28f),
                    cornerRadius = CornerRadius(8f, 8f)
                )
                drawScope.drawCircle(color = Color(0xFF8D6E63), radius = 10f, center = Offset(sx + 20f, sy))
            }
            ResourceNodeType.BASALT_ROCK -> {
                drawScope.drawRoundRect(
                    color = Color(0xFF455A64),
                    topLeft = Offset(sx - 22f, sy - 18f),
                    size = Size(44f, 36f),
                    cornerRadius = CornerRadius(10f, 10f)
                )
                drawScope.drawCircle(color = Color(0xFF78909C), radius = 9f, center = Offset(sx - 6f, sy - 4f))
            }
            ResourceNodeType.FOSSIL_MOUND -> {
                drawScope.drawCircle(color = Color(0xFF8D6E63), radius = 22f, center = Offset(sx, sy))
                // Exposed bone fragment
                drawScope.drawLine(
                    color = Color(0xFFFFF9C4),
                    start = Offset(sx - 10f, sy - 8f),
                    end = Offset(sx + 10f, sy + 8f),
                    strokeWidth = 6f
                )
            }
            ResourceNodeType.HIDDEN_NEST -> {
                drawScope.drawCircle(color = Color(0xFF6D4C41), radius = 22f, center = Offset(sx, sy))
                // Speckled egg
                drawScope.drawOval(
                    color = Color(0xFFFFD54F),
                    topLeft = Offset(sx - 8f, sy - 12f),
                    size = Size(16f, 24f)
                )
            }
            ResourceNodeType.SHELTER_SITE -> {
                // Thatched canopy
                drawScope.drawRoundRect(
                    color = Color(0xFF2E7D32),
                    topLeft = Offset(sx - 35f, sy - 30f),
                    size = Size(70f, 60f),
                    cornerRadius = CornerRadius(12f, 12f)
                )
                // Campfire glow
                drawScope.drawCircle(color = Color(0xFFFF7043), radius = 12f, center = Offset(sx + 20f, sy + 15f))
                drawScope.drawCircle(color = Color(0xFFFFEB3B), radius = 6f, center = Offset(sx + 20f, sy + 15f))
            }
        }
    }

    private fun drawWildlife(
        drawScope: DrawScope,
        w: Wildlife,
        camX: Float,
        camY: Float,
        centerX: Float,
        centerY: Float
    ) {
        val sx = w.x - camX + centerX
        val sy = w.y - camY + centerY
        val dir = if (w.facingRight) 1f else -1f

        // Shadow
        drawScope.drawOval(
            color = Color.Black.copy(alpha = 0.35f),
            topLeft = Offset(sx - 18f, sy + 14f),
            size = Size(36f, 12f)
        )

        when (w.type) {
            EntityType.PREY_GALLIMIMUS -> {
                // Slender bird-like dinosaur
                val bodyColor = Color(0xFFDCE775)
                drawScope.drawOval(color = bodyColor, topLeft = Offset(sx - 16f, sy - 14f), size = Size(32f, 20f))
                // Head
                drawScope.drawCircle(color = bodyColor, radius = 9f, center = Offset(sx + dir * 18f, sy - 18f))
                // Eye
                drawScope.drawCircle(color = Color.Black, radius = 2f, center = Offset(sx + dir * 21f, sy - 20f))
                // Tail
                drawScope.drawLine(
                    color = bodyColor,
                    start = Offset(sx - dir * 14f, sy - 6f),
                    end = Offset(sx - dir * 32f, sy - 12f),
                    strokeWidth = 5f
                )
            }
            EntityType.NEUTRAL_TRICERATOPS -> {
                val bodyColor = Color(0xFF8D6E63)
                // Robust body
                drawScope.drawOval(color = bodyColor, topLeft = Offset(sx - 26f, sy - 18f), size = Size(52f, 34f))
                // Head frill
                drawScope.drawCircle(color = Color(0xFF5D4037), radius = 16f, center = Offset(sx + dir * 20f, sy - 12f))
                // Horns
                drawScope.drawLine(
                    color = Color(0xFFFFF9C4),
                    start = Offset(sx + dir * 24f, sy - 16f),
                    end = Offset(sx + dir * 38f, sy - 28f),
                    strokeWidth = 4f
                )
            }
            EntityType.HOSTILE_ALLOSAURUS -> {
                val bodyColor = Color(0xFFC62828)
                // Fierce apex carnivore
                drawScope.drawOval(color = bodyColor, topLeft = Offset(sx - 24f, sy - 20f), size = Size(48f, 32f))
                // Head
                drawScope.drawCircle(color = Color(0xFFB71C1C), radius = 15f, center = Offset(sx + dir * 24f, sy - 16f))
                // Glowing predator eye
                drawScope.drawCircle(color = Color(0xFFFFD54F), radius = 3.5f, center = Offset(sx + dir * 28f, sy - 19f))
                // Tail
                drawScope.drawLine(
                    color = bodyColor,
                    start = Offset(sx - dir * 22f, sy - 8f),
                    end = Offset(sx - dir * 46f, sy - 16f),
                    strokeWidth = 7f
                )
            }
        }

        // Mini health bar for hostile/damaged
        if (w.health < w.maxHealth) {
            val barW = 34f
            val pct = (w.health / w.maxHealth).coerceIn(0f, 1f)
            drawScope.drawRect(
                color = Color.Black.copy(alpha = 0.6f),
                topLeft = Offset(sx - barW / 2f, sy - 34f),
                size = Size(barW, 4f)
            )
            drawScope.drawRect(
                color = Color(0xFFE53935),
                topLeft = Offset(sx - barW / 2f, sy - 34f),
                size = Size(barW * pct, 4f)
            )
        }
    }

    private fun drawPlayer(
        drawScope: DrawScope,
        engine: GameWorldEngine,
        species: DinosaurSpecies,
        stage: GrowthStage,
        camX: Float,
        camY: Float,
        centerX: Float,
        centerY: Float
    ) {
        val px = engine.playerX - camX + centerX
        val py = engine.playerY - camY + centerY
        val pz = engine.playerZ
        val dir = if (engine.playerFacingRight) 1f else -1f
        val scale = stage.scaleMultiplier

        // Invulnerability blink
        if (engine.invulnerableTimer > 0f && (engine.walkAnimTime * 8).toInt() % 2 == 0) {
            return
        }

        // Ground shadow (scales down as jump height increases)
        val shadowScale = (1f - (pz / 250f)).coerceIn(0.4f, 1f) * scale
        drawScope.drawOval(
            color = Color.Black.copy(alpha = 0.4f * shadowScale),
            topLeft = Offset(px - 22f * shadowScale, py + 14f * scale),
            size = Size(44f * shadowScale, 14f * shadowScale)
        )

        // Player dino elevation offset
        val drawY = py - pz

        val primaryColor = Color(species.primaryColorHex)
        val accentColor = Color(species.accentColorHex)

        drawScope.scale(scaleX = scale, scaleY = scale, pivot = Offset(px, drawY)) {
            // Legs running animation
            val legOffset1 = sin(engine.walkAnimTime) * 12f
            val legOffset2 = -legOffset1

            drawScope.drawLine(
                color = primaryColor.copy(alpha = 0.8f),
                start = Offset(px - 6f, drawY + 8f),
                end = Offset(px - 6f + legOffset1 * 0.4f, drawY + 20f),
                strokeWidth = 6f,
                cap = StrokeCap.Round
            )
            drawScope.drawLine(
                color = primaryColor,
                start = Offset(px + 6f, drawY + 8f),
                end = Offset(px + 6f + legOffset2 * 0.4f, drawY + 20f),
                strokeWidth = 6f,
                cap = StrokeCap.Round
            )

            // Dynamic articulated tail with sway
            val tailTipX = px - dir * (32f + cos(Math.toRadians(engine.tailAngle.toDouble())).toFloat() * 12f)
            val tailTipY = drawY - 4f + sin(Math.toRadians(engine.tailAngle.toDouble())).toFloat() * 10f

            drawScope.drawLine(
                color = primaryColor,
                start = Offset(px - dir * 14f, drawY - 2f),
                end = Offset(tailTipX, tailTipY),
                strokeWidth = 8f,
                cap = StrokeCap.Round
            )
            // Tail accent plumage / tip
            drawScope.drawCircle(color = accentColor, radius = 5f, center = Offset(tailTipX, tailTipY))

            // Torso / Body
            drawScope.drawOval(
                color = primaryColor,
                topLeft = Offset(px - 18f, drawY - 14f),
                size = Size(36f, 24f)
            )

            // Belly accent
            drawScope.drawOval(
                color = accentColor.copy(alpha = 0.7f),
                topLeft = Offset(px - 12f, drawY - 4f),
                size = Size(24f, 12f)
            )

            // Head & Neck
            val headX = px + dir * 18f
            val headY = drawY - 14f
            drawScope.drawCircle(color = primaryColor, radius = 13f, center = Offset(headX, headY))

            // Snout
            drawScope.drawRoundRect(
                color = primaryColor,
                topLeft = Offset(headX + (if (dir > 0) 0f else -14f), headY - 4f),
                size = Size(14f, 12f),
                cornerRadius = CornerRadius(4f, 4f)
            )

            // Eye (amber glowing with pupil)
            val eyeX = headX + dir * 6f
            val eyeY = headY - 4f
            drawScope.drawCircle(color = Color(0xFFFFB300), radius = 3.5f, center = Offset(eyeX, eyeY))
            drawScope.drawCircle(color = Color.Black, radius = 1.8f, center = Offset(eyeX + dir * 0.5f, eyeY))

            // Feathers / Crest spikes
            drawScope.drawLine(
                color = accentColor,
                start = Offset(headX - dir * 6f, headY - 10f),
                end = Offset(headX - dir * 14f, headY - 18f),
                strokeWidth = 3f,
                cap = StrokeCap.Round
            )

            // Attack Bite / Claw slash effect
            if (engine.isAttacking) {
                drawScope.drawArc(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFFFFC857), Color(0xFFF04F52), Color.Transparent),
                        center = Offset(headX + dir * 24f, headY),
                        radius = 35f
                    ),
                    startAngle = if (dir > 0) -45f else 135f,
                    sweepAngle = 90f,
                    useCenter = false,
                    topLeft = Offset(headX + dir * 10f - 25f, headY - 25f),
                    size = Size(50f, 50f),
                    style = Stroke(width = 6f)
                )
            }
        }
    }

    private fun drawWeatherOverlay(drawScope: DrawScope, w: Float, h: Float, engine: GameWorldEngine) {
        when (engine.currentWeather) {
            WeatherType.RAIN, WeatherType.THUNDERSTORM -> {
                // Rain streaks
                val rainCount = if (engine.currentWeather == WeatherType.THUNDERSTORM) 45 else 25
                for (i in 0 until rainCount) {
                    val rx = (i * 47 + engine.walkAnimTime * 200f) % w
                    val ry = (i * 31 + engine.walkAnimTime * 450f) % h
                    drawScope.drawLine(
                        color = Color(0xFF80D8FF).copy(alpha = 0.5f),
                        start = Offset(rx, ry),
                        end = Offset(rx - 12f, ry + 22f),
                        strokeWidth = 2f
                    )
                }
            }
            WeatherType.VOLCANIC_ASH -> {
                // Red glowing volcanic vignette
                drawScope.drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(Color.Transparent, Color(0xFFFF5722).copy(alpha = if (engine.disasterActive) 0.35f else 0.15f)),
                        center = Offset(w / 2f, h / 2f),
                        radius = w * 0.7f
                    ),
                    size = Size(w, h)
                )
            }
            WeatherType.HEATWAVE -> {
                // Solar yellow shimmer
                drawScope.drawRect(
                    color = Color(0xFFFFD54F).copy(alpha = 0.1f),
                    size = Size(w, h)
                )
            }
            else -> {}
        }
    }
}
