package com.rajon.jarvis.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rajon.jarvis.ui.theme.JarvisCyan
import com.rajon.jarvis.ui.theme.JarvisDarkSurface
import com.rajon.jarvis.ui.theme.JarvisDarkSurfaceVariant

/**
 * Translucent HUD panel used across the dashboard, voice and monitoring screens.
 */
@Composable
fun GlassPanel(
    modifier: Modifier = Modifier,
    borderColor: Color = JarvisCyan.copy(alpha = 0.40f),
    contentPadding: PaddingValues = PaddingValues(16.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(18.dp)
    Column(
        modifier = modifier
            .clip(shape)
            .background(
                Brush.linearGradient(
                    listOf(
                        JarvisDarkSurfaceVariant.copy(alpha = 0.92f),
                        JarvisDarkSurface.copy(alpha = 0.86f)
                    )
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    listOf(borderColor, borderColor.copy(alpha = 0.05f))
                ),
                shape = shape
            )
            .padding(contentPadding),
        content = content
    )
}

@Composable
fun HudSectionTitle(
    text: String,
    modifier: Modifier = Modifier,
    accent: Color = JarvisCyan
) {
    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
        Spacer(
            modifier = Modifier
                .width(3.dp)
                .height(14.dp)
                .background(accent, RoundedCornerShape(2.dp))
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            color = accent,
            fontSize = 11.sp,
            letterSpacing = 1.6.sp,
            fontWeight = FontWeight.SemiBold,
            style = MaterialTheme.typography.labelMedium
        )
    }
}
