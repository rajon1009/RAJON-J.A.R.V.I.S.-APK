package com.rajon.jarvis.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlin.math.*
import kotlin.random.Random

/**
 * Full-screen Iron Man / J.A.R.V.I.S. holographic 3D HUD background.
 * Renders a live animated:
 *  - Deep space star-field
 *  - Animated hex-grid with depth parallax
 *  - Rotating 3D perspective ring orbits
 *  - Scanning sweep line
 *  - Corner bracket decorations
 *  - Animated data-stream particles
 */
@Composable
fun JarvisHudBackground(
    modifier: Modifier = Modifier,
    isActive: Boolean = true,
    primaryColor: Color = Color(0xFF00BCD4),
    accentColor: Color = Color(0xFFFF9800),
    content: @Composable () -> Unit
) {
    // Infinite animation driver
    val infiniteTransition = rememberInfiniteTransition(label = "hud_bg")

    val scanY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "scan_y"
    )

    val ringRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ring_rotation"
    )

    val ringRotation2 by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ring_rotation2"
    )

    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    val particleOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "particle_offset"
    )

    val hexOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(20000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "hex_offset"
    )

    // Static star positions (memoized)
    val stars = remember {
        List(80) {
            Triple(
                Random.nextFloat(),   // x
                Random.nextFloat(),   // y
                Random.nextFloat()    // size factor
            )
        }
    }

    val particles = remember {
        List(20) {
            Pair(Random.nextFloat(), Random.nextFloat()) // x base, speed factor
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        // Deep space background
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF050D18),
                            Color(0xFF020810),
                            Color(0xFF010508)
                        )
                    )
                )
        )

        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val cx = w / 2f
            val cy = h / 2f

            // ── 1. Stars ──────────────────────────────────────────────────
            stars.forEach { (sx, sy, sf) ->
                val starAlpha = (0.2f + sf * 0.5f) * pulseAlpha
                drawCircle(
                    color = Color.White.copy(alpha = starAlpha),
                    radius = 0.5f + sf * 1.0f,
                    center = Offset(sx * w, sy * h)
                )
            }

            // ── 2. Hex Grid ───────────────────────────────────────────────
            drawHexGrid(
                width = w,
                height = h,
                primaryColor = primaryColor,
                offset = hexOffset,
                baseAlpha = 0.06f
            )

            // ── 3. Corner HUD Brackets ────────────────────────────────────
            val cornerSize = minOf(w, h) * 0.08f
            val cornerStroke = Stroke(width = 2f, cap = StrokeCap.Round)
            val cornerAlpha = primaryColor.copy(alpha = 0.6f)
            val corners = listOf(
                Offset(0f, 0f) to Pair(1f, 1f),
                Offset(w, 0f) to Pair(-1f, 1f),
                Offset(0f, h) to Pair(1f, -1f),
                Offset(w, h) to Pair(-1f, -1f)
            )
            corners.forEach { (corner, dir) ->
                val (dx, dy) = dir
                // Horizontal arm
                drawLine(
                    color = cornerAlpha,
                    start = corner,
                    end = Offset(corner.x + dx * cornerSize, corner.y),
                    strokeWidth = 2f,
                    cap = StrokeCap.Round
                )
                // Vertical arm
                drawLine(
                    color = cornerAlpha,
                    start = corner,
                    end = Offset(corner.x, corner.y + dy * cornerSize),
                    strokeWidth = 2f,
                    cap = StrokeCap.Round
                )
                // Small dot at corner
                drawCircle(
                    color = primaryColor.copy(alpha = pulseAlpha),
                    radius = 3f,
                    center = corner
                )
            }

            // ── 4. Rotating 3D perspective rings ─────────────────────────
            if (isActive) {
                val ringRadiusX = w * 0.38f
                val ringRadiusY = w * 0.08f // perspective flatten
                val ringAlpha = primaryColor.copy(alpha = 0.18f)

                // Outer ring (slow)
                drawPerspectiveRing(
                    cx = cx, cy = cy * 0.42f,
                    rx = ringRadiusX, ry = ringRadiusY,
                    rotationDeg = ringRotation,
                    color = ringAlpha,
                    strokeWidth = 1.5f
                )

                // Inner ring (faster, reverse)
                drawPerspectiveRing(
                    cx = cx, cy = cy * 0.42f,
                    rx = ringRadiusX * 0.75f, ry = ringRadiusY * 0.75f,
                    rotationDeg = ringRotation2,
                    color = accentColor.copy(alpha = 0.12f),
                    strokeWidth = 1f
                )
            }

            // ── 5. Scan line ──────────────────────────────────────────────
            if (isActive) {
                val scanYPos = scanY * h
                val scanBrush = Brush.verticalGradient(
                    colors = listOf(
                        Color.Transparent,
                        primaryColor.copy(alpha = 0.3f),
                        primaryColor.copy(alpha = 0.08f),
                        Color.Transparent
                    ),
                    startY = scanYPos - 30f,
                    endY = scanYPos + 30f
                )
                drawRect(
                    brush = scanBrush,
                    topLeft = Offset(0f, scanYPos - 30f),
                    size = androidx.compose.ui.geometry.Size(w, 60f)
                )
                // Bright leading edge
                drawLine(
                    color = primaryColor.copy(alpha = 0.5f),
                    start = Offset(0f, scanYPos),
                    end = Offset(w, scanYPos),
                    strokeWidth = 1f
                )
            }

            // ── 6. Data-stream particles (falling dots) ───────────────────
            if (isActive) {
                particles.forEachIndexed { i, (px, speed) ->
                    val t = (particleOffset + speed * 0.5f) % 1f
                    val py = t * h
                    val alpha = sin(t * PI).toFloat().coerceIn(0f, 1f) * 0.6f
                    drawCircle(
                        color = primaryColor.copy(alpha = alpha),
                        radius = 2f,
                        center = Offset(px * w, py)
                    )
                }
            }

            // ── 7. Horizontal divider lines (HUD readout style) ───────────
            val lineAlpha = primaryColor.copy(alpha = 0.08f)
            for (i in 1..6) {
                val lineY = (h / 7f) * i
                drawLine(
                    color = lineAlpha,
                    start = Offset(0f, lineY),
                    end = Offset(w, lineY),
                    strokeWidth = 0.5f
                )
            }

            // ── 8. Bottom status bar accent ───────────────────────────────
            drawLine(
                color = primaryColor.copy(alpha = 0.3f),
                start = Offset(0f, h - 100f),
                end = Offset(w, h - 100f),
                strokeWidth = 0.5f
            )
        }

        // Content on top
        content()
    }
}

private fun DrawScope.drawHexGrid(
    width: Float,
    height: Float,
    primaryColor: Color,
    offset: Float,
    baseAlpha: Float
) {
    val hexSize = 32f
    val hexH = hexSize * sqrt(3f)
    val hexW = hexSize * 2f
    val shiftX = (offset * hexW * 1.5f) % (hexW * 1.5f)
    val shiftY = (offset * hexH) % hexH

    var row = -1
    var colStart = -1
    while (row * hexH - shiftY < height + hexH) {
        var col = colStart
        val yOffset = if (col % 2 == 0) 0f else hexH / 2f
        val baseY = row * hexH - shiftY + yOffset
        while (col * hexW * 1.5f - shiftX < width + hexW) {
            val cx = col * hexW * 1.5f - shiftX + hexW / 2f
            val cy = baseY + hexH / 2f
            drawHexagon(cx, cy, hexSize, primaryColor.copy(alpha = baseAlpha))
            col++
        }
        row++
    }
}

private fun DrawScope.drawHexagon(
    cx: Float, cy: Float, size: Float, color: Color
) {
    val path = Path()
    for (i in 0..5) {
        val angle = Math.toRadians((60 * i - 30).toDouble()).toFloat()
        val x = cx + size * cos(angle)
        val y = cy + size * sin(angle)
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
    drawPath(path, color = color, style = Stroke(width = 0.8f))
}

private fun DrawScope.drawPerspectiveRing(
    cx: Float, cy: Float,
    rx: Float, ry: Float,
    rotationDeg: Float,
    color: Color,
    strokeWidth: Float
) {
    val path = Path()
    val steps = 72
    for (i in 0..steps) {
        val angle = (2 * PI * i / steps).toFloat()
        val rotRad = Math.toRadians(rotationDeg.toDouble()).toFloat()
        val cosA = cos(angle + rotRad)
        val sinA = sin(angle + rotRad)
        // Perspective depth: points behind center are lighter
        val depth = (sinA + 1f) / 2f
        val x = cx + rx * cosA
        val y = cy + ry * sinA
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
    drawPath(
        path = path,
        color = color,
        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
    )
}
