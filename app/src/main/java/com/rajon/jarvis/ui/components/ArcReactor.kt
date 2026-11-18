package com.rajon.jarvis.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rajon.jarvis.ui.theme.JarvisCyan
import com.rajon.jarvis.ui.theme.JarvisCyanDark
import com.rajon.jarvis.ui.theme.JarvisCyanLight
import com.rajon.jarvis.ui.theme.JarvisOrange
import com.rajon.jarvis.ui.theme.JarvisOrangeLight
import com.rajon.jarvis.ui.theme.JarvisSuccess
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * 3D Holographic Arc Reactor AI Core inspired by Marvel's Iron Man J.A.R.V.I.S.
 *
 * Uses real-time 3D perspective projection (Pitch, Yaw, Roll) to render interlocking
 * holographic gyroscope rings, floating 3D particle nodes, expanding energy shockwaves,
 * and an ultra-futuristic Stark Industries HUD reticle.
 */
@Composable
fun ArcReactor(
    modifier: Modifier = Modifier,
    isActive: Boolean = true,
    isListening: Boolean = false,
    isSpeaking: Boolean = false,
    coreSize: Dp = 180.dp
) {
    val transition = rememberInfiniteTransition(label = "jarvis_3d_hologram")

    // Rotation speeds adapt dynamically to active states
    val spinDuration = when {
        isSpeaking -> 4500
        isListening -> 6000
        isActive -> 10000
        else -> 18000
    }

    // 3D Gimbal Rotation Angles
    val rotY by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(spinDuration, easing = LinearEasing)),
        label = "rot_y"
    )

    val rotX by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween((spinDuration * 1.6f).toInt(), easing = LinearEasing)),
        label = "rot_x"
    )

    val rotZ by transition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(tween((spinDuration * 2.2f).toInt(), easing = LinearEasing)),
        label = "rot_z"
    )

    // Core Heartbeat Pulse
    val pulse by transition.animateFloat(
        initialValue = 0.90f,
        targetValue = 1.14f,
        animationSpec = infiniteRepeatable(
            tween(if (isListening || isSpeaking) 750 else 1600, easing = FastOutSlowInEasing),
            RepeatMode.Reverse
        ),
        label = "core_pulse"
    )

    // Expanding Energy Wave
    val waveProgress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(if (isSpeaking) 1200 else 2400, easing = LinearEasing)),
        label = "energy_wave"
    )

    // Audio-reactive waveform phase
    val wavePhase by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(800, easing = LinearEasing)),
        label = "wave_phase"
    )

    val primaryColor = when {
        isListening -> JarvisSuccess
        isSpeaking -> JarvisCyanLight
        isActive -> JarvisCyan
        else -> JarvisCyan.copy(alpha = 0.4f)
    }

    Canvas(modifier = modifier.size(coreSize)) {
        val c = center
        val radius = size.minDimension / 2f
        val cameraZ = radius * 3.4f

        // 1. Ambient Background Glow
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    primaryColor.copy(alpha = if (isActive) 0.28f else 0.08f),
                    JarvisCyanDark.copy(alpha = if (isActive) 0.12f else 0.03f),
                    Color.Transparent
                ),
                center = c,
                radius = radius
            ),
            radius = radius
        )

        // 2. Iron Man HUD Outer Reticle & Corner Brackets
        drawHudReticle(c, radius, primaryColor, isActive)

        // 3. 3D Holographic Orbit Ring 1 (Equatorial Gyro, tilted 25° with tick notches)
        draw3DGimbalRing(
            center = c,
            ringRadius = radius * 0.88f,
            tiltX = 25f,
            tiltY = rotY,
            tiltZ = rotZ * 0.3f,
            cameraZ = cameraZ,
            baseColor = primaryColor,
            strokeWidth = 2.2.dp.toPx(),
            hasNotches = true
        )

        // 4. 3D Holographic Orbit Ring 2 (Polar Orbit, tilted 70° intersecting Ring 1)
        draw3DGimbalRing(
            center = c,
            ringRadius = radius * 0.76f,
            tiltX = rotX + 60f,
            tiltY = -rotY * 0.6f,
            tiltZ = 45f,
            cameraZ = cameraZ,
            baseColor = JarvisCyanLight,
            strokeWidth = 1.8.dp.toPx(),
            isDashed = true
        )

        // 5. 3D Holographic Orbit Ring 3 (Stark Gold Accent Ring, Counter-Rotating)
        draw3DGimbalRing(
            center = c,
            ringRadius = radius * 0.64f,
            tiltX = -rotX * 0.8f + 30f,
            tiltY = rotY * 1.2f,
            tiltZ = -rotZ * 0.5f,
            cameraZ = cameraZ,
            baseColor = if (isActive) JarvisOrangeLight else JarvisOrange.copy(alpha = 0.5f),
            strokeWidth = 2.4.dp.toPx(),
            sweepPortion = 0.45f
        )

        // 6. 3D Audio-Reactive Wave Ring (Pulses when Speaking or Listening)
        if (isSpeaking || isListening) {
            draw3DAudioWaveRing(
                center = c,
                baseRadius = radius * 0.50f,
                waveAmplitude = if (isSpeaking) 7.dp.toPx() else 4.dp.toPx(),
                wavePhase = wavePhase,
                tiltX = rotX * 0.5f,
                tiltY = rotY * 0.8f,
                cameraZ = cameraZ,
                color = if (isListening) JarvisSuccess else JarvisCyanLight
            )
        }

        // 7. 3D Floating Particle Swarm (Starfield Nodes)
        draw3DParticles(
            center = c,
            sphereRadius = radius * 0.82f,
            rotX = rotX * 0.4f,
            rotY = rotY * 0.7f,
            rotZ = rotZ * 0.3f,
            cameraZ = cameraZ,
            color = primaryColor
        )

        // 8. Expanding 3D Shockwave Rings
        val waveR = radius * (0.28f + waveProgress * 0.62f)
        val waveAlpha = (1f - waveProgress) * (if (isActive) 0.55f else 0.20f)
        drawCircle(
            color = primaryColor.copy(alpha = waveAlpha),
            radius = waveR,
            style = Stroke(width = 1.6.dp.toPx())
        )

        // 9. Central Arc Fusion Plasma Core
        val coreR = radius * 0.24f * pulse
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color.White,
                    JarvisCyanLight,
                    primaryColor,
                    Color.Transparent
                ),
                center = c,
                radius = coreR * 1.5f
            ),
            radius = coreR * 1.5f
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color.White, JarvisCyanLight),
                center = c,
                radius = coreR
            ),
            radius = coreR
        )
        // White-hot plasma centerpiece
        drawCircle(
            color = Color.White.copy(alpha = 0.95f),
            radius = coreR * 0.42f
        )
    }
}

/**
 * Draws a 3D ring projected into 2D screen coordinates with depth shading.
 */
private fun DrawScope.draw3DGimbalRing(
    center: Offset,
    ringRadius: Float,
    tiltX: Float,
    tiltY: Float,
    tiltZ: Float,
    cameraZ: Float,
    baseColor: Color,
    strokeWidth: Float,
    isDashed: Boolean = false,
    hasNotches: Boolean = false,
    sweepPortion: Float = 1.0f
) {
    val segments = 64
    val maxIndex = (segments * sweepPortion).toInt().coerceAtLeast(4)

    val radX = Math.toRadians(tiltX.toDouble()).toFloat()
    val radY = Math.toRadians(tiltY.toDouble()).toFloat()
    val radZ = Math.toRadians(tiltZ.toDouble()).toFloat()

    val cosX = cos(radX); val sinX = sin(radX)
    val cosY = cos(radY); val sinY = sin(radY)
    val cosZ = cos(radZ); val sinZ = sin(radZ)

    var prevScreenX = 0f
    var prevScreenY = 0f
    var hasPrev = false

    for (i in 0..maxIndex) {
        val angle = i * (2f * PI.toFloat() / segments)

        // Local ring coordinates (in XY plane, Z = 0)
        val lx = ringRadius * cos(angle)
        val ly = ringRadius * sin(angle)
        val lz = 0f

        // 3D Euler Rotation
        val y1 = ly * cosX - lz * sinX
        val z1 = ly * sinX + lz * cosX

        val x2 = lx * cosY + z1 * sinY
        val z2 = -lx * sinY + z1 * cosY

        val x3 = x2 * cosZ - y1 * sinZ
        val y3 = x2 * sinZ + y1 * cosZ
        val z3 = z2

        // Perspective Projection
        val proj = cameraZ / (cameraZ - z3)
        val screenX = center.x + x3 * proj
        val screenY = center.y + y3 * proj

        // Depth-based lighting & stroke weight
        val depthRatio = ((z3 / ringRadius) + 1f) / 2f // 0 (back) to 1 (front)
        val alpha = (0.22f + depthRatio * 0.75f).coerceIn(0.15f, 1f)
        val width = strokeWidth * (0.7f + depthRatio * 0.7f)

        if (hasPrev) {
            val shouldDraw = !isDashed || (i % 4 < 2)
            if (shouldDraw) {
                drawLine(
                    color = baseColor.copy(alpha = alpha),
                    start = Offset(prevScreenX, prevScreenY),
                    end = Offset(screenX, screenY),
                    strokeWidth = width,
                    cap = StrokeCap.Round
                )
            }
        }

        // 3D Notches/ticks around the ring
        if (hasNotches && i % 8 == 0 && depthRatio > 0.3f) {
            val notchLen = 4.dp.toPx() * proj
            val nx = x3 * (1f + notchLen / ringRadius)
            val ny = y3 * (1f + notchLen / ringRadius)
            val notchScreenX = center.x + nx * proj
            val notchScreenY = center.y + ny * proj

            drawLine(
                color = baseColor.copy(alpha = (alpha * 1.1f).coerceAtMost(1f)),
                start = Offset(screenX, screenY),
                end = Offset(notchScreenX, notchScreenY),
                strokeWidth = width * 1.2f,
                cap = StrokeCap.Round
            )
        }

        prevScreenX = screenX
        prevScreenY = screenY
        hasPrev = true
    }
}

/**
 * Audio-reactive sinusoidal waveform projected onto a 3D orbital plane.
 */
private fun DrawScope.draw3DAudioWaveRing(
    center: Offset,
    baseRadius: Float,
    waveAmplitude: Float,
    wavePhase: Float,
    tiltX: Float,
    tiltY: Float,
    cameraZ: Float,
    color: Color
) {
    val segments = 72
    val radX = Math.toRadians(tiltX.toDouble()).toFloat()
    val radY = Math.toRadians(tiltY.toDouble()).toFloat()
    val cosX = cos(radX); val sinX = sin(radX)
    val cosY = cos(radY); val sinY = sin(radY)

    var prevX = 0f
    var prevY = 0f
    var hasPrev = false

    for (i in 0..segments) {
        val angle = i * (2f * PI.toFloat() / segments)
        val r = baseRadius + sin(angle * 8f + wavePhase) * waveAmplitude

        val lx = r * cos(angle)
        val ly = r * sin(angle)

        val y1 = ly * cosX
        val z1 = ly * sinX

        val x2 = lx * cosY + z1 * sinY
        val z2 = -lx * sinY + z1 * cosY

        val proj = cameraZ / (cameraZ - z2)
        val sx = center.x + x2 * proj
        val sy = center.y + y1 * proj

        val depth = ((z2 / baseRadius) + 1f) / 2f
        val alpha = (0.35f + depth * 0.65f).coerceIn(0.2f, 1f)

        if (hasPrev) {
            drawLine(
                color = color.copy(alpha = alpha),
                start = Offset(prevX, prevY),
                end = Offset(sx, sy),
                strokeWidth = 2.4.dp.toPx() * (0.8f + depth * 0.5f),
                cap = StrokeCap.Round
            )
        }

        prevX = sx
        prevY = sy
        hasPrev = true
    }
}

/**
 * 3D Holographic Particle Nodes orbiting in a spherical cloud.
 */
private fun DrawScope.draw3DParticles(
    center: Offset,
    sphereRadius: Float,
    rotX: Float,
    rotY: Float,
    rotZ: Float,
    cameraZ: Float,
    color: Color
) {
    val radX = Math.toRadians(rotX.toDouble()).toFloat()
    val radY = Math.toRadians(rotY.toDouble()).toFloat()
    val radZ = Math.toRadians(rotZ.toDouble()).toFloat()
    val cosX = cos(radX); val sinX = sin(radX)
    val cosY = cos(radY); val sinY = sin(radY)
    val cosZ = cos(radZ); val sinZ = sin(radZ)

    // Predefined spherical positions (theta, phi) for 24 particles
    for (i in 0 until 24) {
        val theta = (i * 0.523f) % (2f * PI.toFloat())
        val phi = ((i * 1.137f) % PI.toFloat()) - (PI.toFloat() / 2f)

        val px = sphereRadius * cos(phi) * cos(theta)
        val py = sphereRadius * cos(phi) * sin(theta)
        val pz = sphereRadius * sin(phi)

        // 3D rotation
        val y1 = py * cosX - pz * sinX
        val z1 = py * sinX + pz * cosX

        val x2 = px * cosY + z1 * sinY
        val z2 = -px * sinY + z1 * cosY

        val x3 = x2 * cosZ - y1 * sinZ
        val y3 = x2 * sinZ + y1 * cosZ
        val z3 = z2

        val proj = cameraZ / (cameraZ - z3)
        val sx = center.x + x3 * proj
        val sy = center.y + y3 * proj

        val depth = ((z3 / sphereRadius) + 1f) / 2f
        val alpha = (0.20f + depth * 0.80f).coerceIn(0.1f, 1f)
        val pSize = 1.6.dp.toPx() * (0.7f + depth * 1.1f)

        drawCircle(
            color = color.copy(alpha = alpha),
            radius = pSize,
            center = Offset(sx, sy)
        )

        // Subtle crosshair twinkle on the closest particles
        if (depth > 0.82f) {
            val spark = 3.5.dp.toPx()
            drawLine(
                color = Color.White.copy(alpha = alpha * 0.7f),
                start = Offset(sx - spark, sy),
                end = Offset(sx + spark, sy),
                strokeWidth = 1.dp.toPx()
            )
            drawLine(
                color = Color.White.copy(alpha = alpha * 0.7f),
                start = Offset(sx, sy - spark),
                end = Offset(sx, sy + spark),
                strokeWidth = 1.dp.toPx()
            )
        }
    }
}

/**
 * Iron Man HUD Reticle, Corner Ticks, and Circular Crosshairs.
 */
private fun DrawScope.drawHudReticle(
    center: Offset,
    radius: Float,
    color: Color,
    isActive: Boolean
) {
    val outerR = radius * 0.96f
    val bracketLen = radius * 0.16f
    val hudAlpha = if (isActive) 0.50f else 0.20f

    // 4 Corner Brackets
    val offsets = listOf(
        Offset(-outerR, -outerR) to (Offset(bracketLen, 0f) to Offset(0f, bracketLen)),
        Offset(outerR, -outerR) to (Offset(-bracketLen, 0f) to Offset(0f, bracketLen)),
        Offset(-outerR, outerR) to (Offset(bracketLen, 0f) to Offset(0f, -bracketLen)),
        Offset(outerR, outerR) to (Offset(-bracketLen, 0f) to Offset(0f, -bracketLen))
    )

    for ((corner, legs) in offsets) {
        val pt = center + corner
        drawLine(
            color = color.copy(alpha = hudAlpha),
            start = pt,
            end = pt + legs.first,
            strokeWidth = 1.6.dp.toPx()
        )
        drawLine(
            color = color.copy(alpha = hudAlpha),
            start = pt,
            end = pt + legs.second,
            strokeWidth = 1.6.dp.toPx()
        )
    }

    // Faint outer targeting ring
    drawCircle(
        color = color.copy(alpha = hudAlpha * 0.4f),
        radius = outerR,
        style = Stroke(width = 1.dp.toPx())
    )

    // Cardinal tick marks
    val tickLen = 6.dp.toPx()
    val cardinals = listOf(
        Offset(0f, -outerR),
        Offset(0f, outerR),
        Offset(-outerR, 0f),
        Offset(outerR, 0f)
    )
    for (card in cardinals) {
        val start = center + card
        val dir = if (card.x == 0f) Offset(0f, if (card.y < 0) tickLen else -tickLen) else Offset(if (card.x < 0) tickLen else -tickLen, 0f)
        drawLine(
            color = color.copy(alpha = hudAlpha * 0.8f),
            start = start,
            end = start + dir,
            strokeWidth = 1.6.dp.toPx()
        )
    }
}
