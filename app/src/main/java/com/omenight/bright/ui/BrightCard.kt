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
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.core.graphics.createBitmap
import androidx.tv.material3.Border
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.CardScale
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.omenight.bright.ui.glass.WallpaperBaker
import com.omenight.bright.ui.glass.glass
import com.omenight.bright.ui.glass.rimBrush
import com.omenight.bright.ui.theme.BrightColors
import kotlin.math.roundToInt

val TileCorner = 14.dp
private val TileShape = RoundedCornerShape(TileCorner)
private const val FocusedScale = 1.08f
private val ShadowBlur = 14.dp
private val ShadowDrop = 4.dp

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
    val lift by animateFloatAsState(if (focused) 1f else 0f, tween(180), label = "lift")

    // Lifted above its neighbours so the shadow is not covered by the next tile
    Column(modifier.zIndex(if (focused) 1f else 0f)) {
        Card(
            onClick = onClick,
            onLongClick = onLongClick,
            modifier = cardModifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .onFocusChanged { focused = it.isFocused }
                // Our own scale and baked shadow, the tv Glow re-blurs its shadow on every scaled frame
                .graphicsLayer {
                    scaleX = 1f + (FocusedScale - 1f) * lift
                    scaleY = scaleX
                }
                .drawWithCache {
                    val shadow = tileShadow(size, TileCorner.toPx(), ShadowBlur.toPx())
                    val spread = size.width * shadow.spread
                    onDrawBehind {
                        if (lift > 0f) {
                            drawImage(
                                image = shadow.image,
                                dstOffset = IntOffset(-spread.roundToInt(), (-spread + ShadowDrop.toPx()).roundToInt()),
                                dstSize = IntSize((size.width + spread * 2).roundToInt(), (size.height + spread * 2).roundToInt()),
                                alpha = lift,
                            )
                        }
                    }
                },
            shape = CardDefaults.shape(TileShape),
            colors = CardDefaults.colors(
                containerColor = Color.Transparent,
                focusedContainerColor = Color.Transparent,
                pressedContainerColor = Color.Transparent,
            ),
            scale = CardScale.None,
            border = CardDefaults.border(
                border = Border.None,
                focusedBorder = Border.None,
                pressedBorder = Border.None,
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

private class TileShadow(
    val image: ImageBitmap,
    /** Margin on each side, as a fraction of the tile width. */
    val spread: Float,
)

private val shadowCache = HashMap<Long, TileShadow>()

// Baked at quarter size and stretched when drawn, a blurred shadow loses nothing to upscaling
private fun tileShadow(size: Size, corner: Float, blur: Float): TileShadow {
    val key = (size.width.toLong() shl 32) or size.height.toLong()
    return shadowCache.getOrPut(key) {
        val scale = 0.25f
        val margin = blur * 3 * scale
        val w = size.width * scale
        val h = size.height * scale
        val bitmap = createBitmap((w + margin * 2).roundToInt(), (h + margin * 2).roundToInt())
        val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply { color = ShadowColor }
        android.graphics.Canvas(bitmap).drawRoundRect(margin, margin, margin + w, margin + h, corner * scale, corner * scale, paint)
        // Three box passes of radius r approximate a gaussian with sigma close to r
        val blurred = WallpaperBaker.boxBlur(bitmap, radius = (blur * scale).roundToInt().coerceAtLeast(1), passes = 3)
        TileShadow(blurred.asImageBitmap(), spread = margin / w)
    }
}

private const val ShadowColor = 0xB3000000.toInt()
