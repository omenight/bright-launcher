package com.omenight.bright.data

import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Shader
import android.os.Build
import androidx.core.graphics.scale
import com.omenight.bright.ui.wallpaper.WallpaperBaker
import com.omenight.bright.ui.wallpaper.WallpaperPreset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Copies Bright's wallpaper into the system one. TV settings and other side panels draw the system
 * wallpaper behind themselves and Android hides the launcher meanwhile, so without this they sit on black.
 */
class SystemWallpaper(private val context: Context) {

    /** Returns false when the system refused, so the caller can try again later. */
    suspend fun set(preset: WallpaperPreset): Boolean = withContext(Dispatchers.Default) {
        val manager = WallpaperManager.getInstance(context)
        if (!manager.isWallpaperSupported) return@withContext false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && !manager.isSetWallpaperAllowed) return@withContext false

        val metrics = context.resources.displayMetrics
        val bitmap = WallpaperBaker.render(preset).scale(metrics.widthPixels, metrics.heightPixels)
        // Same grain as the in-app backdrop, an upscaled gradient bands on 8-bit TV panels without it
        Canvas(bitmap).drawPaint(
            Paint().apply { shader = BitmapShader(WallpaperBaker.grain(), Shader.TileMode.REPEAT, Shader.TileMode.REPEAT) },
        )
        withContext(Dispatchers.IO) {
            runCatching {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    manager.setBitmap(bitmap, null, true, WallpaperManager.FLAG_SYSTEM)
                } else {
                    manager.setBitmap(bitmap)
                }
            }.isSuccess
        }.also { bitmap.recycle() }
    }
}
