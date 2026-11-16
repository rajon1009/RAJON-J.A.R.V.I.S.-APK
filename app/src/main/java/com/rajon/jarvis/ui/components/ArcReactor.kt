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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.rajon.jarvis.ui.theme.JarvisCyan
import com.rajon.jarvis.ui.theme.JarvisCyanDark
import com.rajon.jarvis.ui.theme.JarvisCyanLight
import com.rajon.jarvis.ui.theme.JarvisOrange
import kotlin.math.cos
import kotlin.math.sin

/**
 * Animated arc-reactor style AI core. All drawing is done on a single Canvas so the
 * animation stays cheap (no layout recomposition per frame).
 */
@Composable
fun ArcReactor(
    modifier: Modifier = Modifier,
    isActive: Boolean = true,
    coreSize: Dp = 180.dp
) {
    val transition = rememberInfiniteTransition(label = "arc_reactor")

    val rotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(durationMillis = 14000, easing = LinearEasing)),
        label = "rotation"
    )
    val reverseRotation by transition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(tween(durationMillis = 20000, easing = LinearEasing)),
        label = "reverse_rotation"
    )
    val pulse by transition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.07f,
        animationSpec = infiniteRepeatable(
            tween(durationMillis = 1500, easing = FastOutSlowInEasing),
            RepeatMode.Reverse
        ),
        label = "pulse"
    )
    val sweep by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(durationMillis = 2600, easing = LinearEasing)),
        label = "sweep"
    )

    Canvas(modifier = modifier.size(coreSize)) {
        val c = center
        val radius = size.minDimension / 2f
        val outer = radius * 0.94f
        val glowAlpha = if (isActive) 0.32f else 0.10f

        // Outer glow
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(JarvisCyan.copy(alpha = glowAlpha), Color.Transparent),
                center = c,
                radius = outer
            ),
            radius = outer
        )

        // Dashed outer ring
        val dashCount = 72
        repeat(dashCount) { index ->
            val startAngle = index * (360f / dashCount) + rotation
            drawArc(
                color = JarvisCyan.copy(alpha = if (isActive) 0.55f else 0.22f),
                startAngle = startAngle,
                sweepAngle = 2.6f,
                useCenter = false,
                topLeft = Offset(c.x - outer, c.y - outer),
                size = Size(outer * 2f, outer * 2f),
                style = Stroke(width = 1.6.dp.toPx())
            )
        }

        // Main containment ring
        val ringA = outer * 0.84f
        drawCircle(
            color = JarvisCyan.copy(alpha = 0.30f),
            radius = ringA,
            style = Stroke(width = 1.dp.toPx())
        )

        // Bright sweeping arc
        drawArc(
            brush = Brush.sweepGradient(
                colors = listOf(Color.Transparent, JarvisCyan, JarvisCyanLight, Color.Transparent),
                center = c
            ),
            startAngle = rotation,
            sweepAngle = 115f,
            useCenter = false,
            topLeft = Offset(c.x - ringA, c.y - ringA),
            size = Size(ringA * 2f, ringA * 2f),
            style = Stroke(width = 3.5.dp.toPx(), cap = StrokeCap.Round)
        )

        // Orange accent arc (counter-rotating)
        drawArc(
            color = JarvisOrange.copy(alpha = if (isActive) 0.80f else 0.30f),
            startAngle = reverseRotation,
            sweepAngle = 38f,
            useCenter = false,
            topLeft = Offset(c.x - ringA, c.y - ringA),
            size = Size(ringA * 2f, ringA * 2f),
            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
        )

        // Inner segmented ring
        val ringB = outer * 0.64f
        repeat(12) { index ->
            val startAngle = index * 30f + reverseRotation * 0.5f
            drawArc(
                color = JarvisCyan.copy(alpha = 0.45f),
                startAngle = startAngle,
                sweepAngle = 14f,
                useCenter = false,
                topLeft = Offset(c.x - ringB, c.y - ringB),
                size = Size(ringB * 2f, ringB * 2f),
                style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
            )
        }

        // Coil spokes
        repeat(8) { index ->
            val angle = Math.toRadians((index * 45f + rotation * 0.35f).toDouble())
            val inner = outer * 0.36f
            val spokeStart = Offset(
                c.x + (cos(angle) * inner).toFloat(),
                c.y + (sin(angle) * inner).toFloat()
            )
            val spokeEnd = Offset(
                c.x + (cos(angle) * ringB * 0.94f).toFloat(),
                c.y + (sin(angle) * ringB * 0.94f).toFloat()
            )
            drawLine(
                color = JarvisCyan.copy(alpha = 0.22f),
                start = spokeStart,
                end = spokeEnd,
                strokeWidth = 1.2.dp.toPx(),
                cap = StrokeCap.Round
            )
        }

        // Expanding pulse ring
        val pulseRadius = outer * (0.34f + sweep * 0.58f)
        drawCircle(
            color = JarvisCyan.copy(alpha = ((1f - sweep) * 0.40f)),
            radius = pulseRadius,
            style = Stroke(width = 1.8.dp.toPx())
        )

        // Core
        val coreRadius = outer * 0.24f * pulse
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color.White, JarvisCyanLight, JarvisCyanDark),
                center = c,
                radius = coreRadius
            ),
            radius = coreRadius
        )
        drawCircle(
            color = Color.White.copy(alpha = 0.85f),
            radius = coreRadius * 0.36f
        )
    }
}
