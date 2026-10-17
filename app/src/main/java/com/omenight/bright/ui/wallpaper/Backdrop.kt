package com.omenight.bright.ui.wallpaper

import android.content.Context
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.IntSize
import com.omenight.bright.data.SettingsStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlin.math.roundToInt

/** The wallpaper behind the whole launcher and the surface color matched to it. */
@Immutable
class Backdrop(
    val wallpaper: ImageBitmap,
    val grain: ImageBitmap,
    /** Opaque color of sheets, cards and the notice, the wallpaper's own hue lifted towards white. */
    val surface: Color,
)

/** Null only for the moment before the first bake finishes. */
val LocalBackdrop = staticCompositionLocalOf<Backdrop?> { null }

/**
 * Bakes the saved wallpaper, or the one being previewed, on a background thread and keeps the
 * result for the process. Started from onCreate so the first bake overlaps Compose's own startup.
 */
@OptIn(ExperimentalCoroutinesApi::class)
object BackdropEngine {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val preview = MutableStateFlow<WallpaperPreset?>(null)
    private var backdrop: StateFlow<Backdrop?>? = null

    @Synchronized
    fun start(context: Context): StateFlow<Backdrop?> = backdrop ?: combine(
        SettingsStore(context.applicationContext).wallpaper,
        preview,
    ) { saved, previewing -> previewing ?: saved }
        .distinctUntilChanged()
        // A newer choice drops the result of a bake still in flight
        .mapLatest(::bake)
        .stateIn(scope, SharingStarted.Eagerly, null)
        .also { backdrop = it }

    /** Shows [preset] instead of the saved wallpaper until called again with null. */
    fun preview(preset: WallpaperPreset?) {
        preview.value = preset
    }

    private fun bake(preset: WallpaperPreset) = Backdrop(
        wallpaper = WallpaperBaker.render(preset).asImageBitmap(),
        grain = grain,
        surface = lerp(Color(0xFF000000.toInt() or preset.base[1]), Color.White, 0.09f),
    )

    private val grain by lazy { WallpaperBaker.grain().asImageBitmap() }
}

/**
 * Paints the wallpaper behind the content, dimming it as [dim] goes to 1 for All apps.
 * Apply once, on the root. It costs one full-screen image plus the grain.
 */
fun Modifier.backdrop(backdrop: Backdrop?, fallback: Color, dim: () -> Float): Modifier = this
    .drawWithCache {
        if (backdrop == null) return@drawWithCache onDrawBehind { drawRect(fallback) }
        val grain = ShaderBrush(ImageShader(backdrop.grain, TileMode.Repeated, TileMode.Repeated))
        val dst = IntSize(size.width.roundToInt(), size.height.roundToInt())
        onDrawBehind {
            drawImage(backdrop.wallpaper, dstSize = dst, filterQuality = FilterQuality.Low)
            val amount = dim()
            if (amount > 0f) drawRect(Color.Black.copy(alpha = 0.45f * amount))
            drawRect(grain)
        }
    }
