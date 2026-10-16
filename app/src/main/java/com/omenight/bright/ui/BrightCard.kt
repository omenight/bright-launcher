package com.omenight.bright.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Border
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.Glow
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.omenight.bright.ui.glass.glass
import com.omenight.bright.ui.glass.rimBrush
import com.omenight.bright.ui.theme.BrightColors

val TileCorner = 14.dp
private val TileShape = RoundedCornerShape(TileCorner)

/**
 * The one tile style used everywhere. Focus lifts the tile with a real shadow, brightens its
 * specular rim and sweeps a glint across it once. The label shows only while focused.
 */
@Composable
fun BrightCard(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    cardModifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
    glassBackground: Boolean = false,
    content: @Composable BoxScope.() -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    val glint = remember { Animatable(1f) }
    LaunchedEffect(focused) {
        if (focused) {
            glint.snapTo(0f)
            glint.animateTo(1f, tween(durationMillis = 1100, easing = FastOutSlowInEasing))
        }
    }
    val rimStrength by animateFloatAsState(if (focused) 1f else 0.45f, label = "rim")
    val labelAlpha by animateFloatAsState(if (focused) 1f else 0f, label = "label")

    Column(modifier) {
        Card(
            onClick = onClick,
            onLongClick = onLongClick,
            modifier = cardModifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .onFocusChanged { focused = it.isFocused },
            shape = CardDefaults.shape(TileShape),
            colors = CardDefaults.colors(
                containerColor = Color.Transparent,
                focusedContainerColor = Color.Transparent,
                pressedContainerColor = Color.Transparent,
            ),
            scale = CardDefaults.scale(focusedScale = 1.08f),
            border = CardDefaults.border(
                border = Border.None,
                focusedBorder = Border.None,
                pressedBorder = Border.None,
            ),
            glow = CardDefaults.glow(
                focusedGlow = Glow(elevationColor = Color.Black.copy(alpha = 0.7f), elevation = 24.dp),
            ),
        ) {
            Box(
                Modifier
                    .fillMaxSize()
                    .then(if (glassBackground) Modifier.glass(TileCorner, rim = false) else Modifier)
                    .drawWithContent {
                        drawContent()
                        val t = glint.value
                        if (t < 1f) {
                            // A diagonal band of light travelling left to right
                            val x = size.width * (-0.4f + t * 1.8f)
                            val band = size.width * 0.35f
                            drawRect(
                                Brush.linearGradient(
                                    0f to Color.Transparent,
                                    0.5f to Color.White.copy(alpha = 0.22f),
                                    1f to Color.Transparent,
                                    start = Offset(x - band, 0f),
                                    end = Offset(x + band, size.height),
                                ),
                            )
                        }
                        val stroke = 1.dp.toPx()
                        drawRoundRect(
                            brush = rimBrush(size, rimStrength),
                            topLeft = Offset(stroke / 2, stroke / 2),
                            size = size.copy(width = size.width - stroke, height = size.height - stroke),
                            cornerRadius = CornerRadius(TileCorner.toPx() - stroke / 2),
                            style = Stroke(stroke),
                        )
                    },
                content = content,
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = BrightColors.Text,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .padding(top = 12.dp, start = 2.dp)
                .graphicsLayer { alpha = labelAlpha },
        )
    }
}
