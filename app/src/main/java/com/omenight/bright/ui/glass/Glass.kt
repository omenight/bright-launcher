package com.omenight.bright.ui.glass

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.findRootCoordinates
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlin.math.roundToInt

/** The wallpaper behind the whole launcher plus its pre-blurred copies that glass shows. */
@Immutable
class Backdrop(
    val wallpaper: ImageBitmap,
    val frosted: ImageBitmap,
    /** Frosted and darkened, the full-screen sheet behind All apps. */
    val sheet: ImageBitmap,
    val grain: ImageBitmap,
)

/** Null only for the moment before the first bake finishes. */
val LocalBackdrop = staticCompositionLocalOf<Backdrop?> { null }

/** The default backdrop, baked once per process on a background thread. */
object DawnBackdrop {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /** Touch this as early as possible, so the bake overlaps Compose's own startup. */
    val bake: Deferred<Backdrop> by lazy {
        scope.async {
            val wallpaper = WallpaperBaker.dawn()
            val frosted = WallpaperBaker.frost(wallpaper)
            Backdrop(
                wallpaper = wallpaper.asImageBitmap(),
                frosted = frosted.asImageBitmap(),
                sheet = WallpaperBaker.darken(frosted, amount = 0.30f).asImageBitmap(),
                grain = WallpaperBaker.grain().asImageBitmap(),
            )
        }
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
@Composable
fun rememberDawnBackdrop(): Backdrop? {
    val bake = DawnBackdrop.bake
    // Normally done before the first frame, so the wallpaper is there from the start
    val initial = if (bake.isCompleted) bake.getCompleted() else null
    return produceState(initial) { value = bake.await() }.value
}

/**
 * Paints the wallpaper behind the content, crossfading to the frosted sheet as [sheet] goes to 1.
 * Apply once, on the root. At rest it costs exactly one full-screen image plus the grain.
 */
fun Modifier.backdrop(backdrop: Backdrop?, fallback: Color, sheet: () -> Float): Modifier = this
    .drawWithCache {
        if (backdrop == null) return@drawWithCache onDrawBehind { drawRect(fallback) }
        val grain = ShaderBrush(ImageShader(backdrop.grain, TileMode.Repeated, TileMode.Repeated))
        val dst = size.toIntSize()
        onDrawBehind {
            val amount = sheet()
            if (amount < 1f) drawImage(backdrop.wallpaper, dstSize = dst, filterQuality = FilterQuality.Low)
            if (amount > 0f) {
                drawImage(backdrop.sheet, dstSize = dst, alpha = amount, filterQuality = FilterQuality.Low)
            }
            drawRect(grain)
        }
    }

/**
 * Liquid-glass material with rounded corners. Shows the frosted wallpaper slice under this element,
 * magnified by [lens] to read as refraction, with a top sheen and a specular rim lit from the top-left.
 */
fun Modifier.glass(
    cornerRadius: Dp,
    tint: Color = Color.White.copy(alpha = 0.06f),
    lens: Float = 1.06f,
    rim: Boolean = true,
): Modifier = composed {
    val backdrop = LocalBackdrop.current
    // Read only while drawing, so moving costs a redraw rather than a recomposition
    val origin = remember { mutableStateOf(Offset.Zero) }
    val rootSize = remember { mutableStateOf(IntSize.Zero) }

    // A rounded-rect layer clip is analytic on the GPU, unlike a path clip, and it also bounds
    // the oversized frosted image draw below to just this element
    clip(RoundedCornerShape(cornerRadius))
        .onGloballyPositioned {
            origin.value = it.positionInRoot()
            rootSize.value = it.findRootCoordinates().size
        }
        .drawWithCache {
            val sheen = Brush.verticalGradient(
                0f to Color.White.copy(alpha = 0.10f),
                0.45f to Color.Transparent,
            )
            val rimBrush = rimBrush(size, strength = 1f)
            val rimWidth = 1.dp.toPx()
            val rimRadius = CornerRadius(cornerRadius.toPx() - rimWidth / 2)

            onDrawBehind {
                if (backdrop != null) {
                    val at = origin.value
                    withTransform({
                        scale(lens, lens, pivot = center)
                        translate(-at.x, -at.y)
                    }) {
                        drawImage(backdrop.frosted, dstSize = rootSize.value, filterQuality = FilterQuality.Low)
                    }
                }
                drawRect(tint)
                drawRect(sheen)
                if (rim) {
                    drawRoundRect(
                        brush = rimBrush,
                        topLeft = Offset(rimWidth / 2, rimWidth / 2),
                        size = Size(size.width - rimWidth, size.height - rimWidth),
                        cornerRadius = rimRadius,
                        style = Stroke(rimWidth),
                    )
                }
            }
        }
}

/** Specular edge light, bright at the top-left, fading through the middle, catching again bottom-right. */
fun rimBrush(size: Size, strength: Float): Brush = Brush.linearGradient(
    0f to Color.White.copy(alpha = 0.55f * strength),
    0.45f to Color.White.copy(alpha = 0.06f * strength),
    1f to Color.White.copy(alpha = 0.22f * strength),
    start = Offset.Zero,
    end = Offset(size.width, size.height),
)

private fun Size.toIntSize() = IntSize(width.roundToInt(), height.roundToInt())
