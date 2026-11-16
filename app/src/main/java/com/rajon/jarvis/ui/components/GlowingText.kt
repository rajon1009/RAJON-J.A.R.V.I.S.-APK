package com.rajon.jarvis.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.rajon.jarvis.ui.theme.JarvisCyan
import com.rajon.jarvis.ui.theme.JarvisCyanGlowStrong

@Composable
fun GlowingText(
    text: String,
    modifier: Modifier = Modifier,
    textColor: Color = JarvisCyan,
    glowColor: Color = JarvisCyanGlowStrong,
    style: TextStyle = MaterialTheme.typography.headlineMedium,
    blurRadius: Float = 16f
) {
    Text(
        text = text,
        modifier = modifier,
        style = style.copy(
            color = textColor,
            fontWeight = FontWeight.Bold,
            shadow = Shadow(
                color = glowColor,
                offset = Offset(0f, 0f),
                blurRadius = blurRadius
            )
        )
    )
}
