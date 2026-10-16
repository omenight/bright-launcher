package com.omenight.bright.ui.glass

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import androidx.core.graphics.createBitmap
import androidx.core.graphics.scale
import kotlin.random.Random

/**
 * Produces the backdrop bitmaps once, on the CPU. Glass surfaces sample these instead of
 * blurring the screen every frame, which low-end TV GPUs (and anything below API 31) cannot do.
 */
internal object WallpaperBaker {
    private const val WIDTH = 320
    private const val HEIGHT = 180
    private const val OPAQUE = 0xFF shl 24

    fun render(preset: WallpaperPreset): Bitmap {
        val bitmap = createBitmap(WIDTH, HEIGHT)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG)

        paint.shader = LinearGradient(
            0f, 0f, 0f, HEIGHT.toFloat(),
            preset.base.map { OPAQUE or it }.toIntArray(),
            floatArrayOf(0f, 0.55f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawRect(0f, 0f, WIDTH.toFloat(), HEIGHT.toFloat(), paint)
        preset.glows.forEach { glow(canvas, paint, it) }

        return boxBlur(bitmap, radius = 4, passes = 2)
    }

    /** The frosted version glass shows, heavily blurred and more saturated (Apple's "vibrancy"). */
    fun frost(source: Bitmap): Bitmap {
        val small = source.scale(source.width / 2, source.height / 2)
        val blurred = boxBlur(small, radius = 6, passes = 3)
        val out = createBitmap(blurred.width, blurred.height)
        val paint = Paint(Paint.FILTER_BITMAP_FLAG).apply {
            colorFilter = ColorMatrixColorFilter(ColorMatrix().apply { setSaturation(1.4f) })
        }
        Canvas(out).drawBitmap(blurred, 0f, 0f, paint)
        return out
    }

    fun darken(source: Bitmap, amount: Float): Bitmap {
        val out = source.copy(Bitmap.Config.ARGB_8888, true)
        Canvas(out).drawColor((amount * 255).toInt() shl 24)
        return out
    }

    /** A small tileable noise texture that hides gradient banding on 8-bit TV panels. */
    fun grain(size: Int = 128): Bitmap {
        val random = Random(seed = 7)
        val pixels = IntArray(size * size) {
            val alpha = random.nextInt(0, 14)
            val shade = if (random.nextBoolean()) 0xFFFFFF else 0x000000
            (alpha shl 24) or shade
        }
        return Bitmap.createBitmap(pixels, size, size, Bitmap.Config.ARGB_8888)
    }

    private fun glow(canvas: Canvas, paint: Paint, glow: Glow) {
        val color = ((glow.alpha * 255).toInt() shl 24) or glow.rgb
        paint.shader = RadialGradient(
            glow.x * WIDTH, glow.y * HEIGHT, glow.radius * WIDTH,
            intArrayOf(color, color and 0x00FFFFFF),
            null,
            Shader.TileMode.CLAMP,
        )
        canvas.drawRect(0f, 0f, WIDTH.toFloat(), HEIGHT.toFloat(), paint)
    }

    // Repeated box blur passes converge on a gaussian and stay cheap at these sizes
    fun boxBlur(source: Bitmap, radius: Int, passes: Int): Bitmap {
        val width = source.width
        val height = source.height
        val a = IntArray(width * height)
        val b = IntArray(width * height)
        source.getPixels(a, 0, width, 0, 0, width, height)
        repeat(passes) {
            blurPass(a, b, width, height, radius, horizontal = true)
            blurPass(b, a, width, height, radius, horizontal = false)
        }
        return Bitmap.createBitmap(a, width, height, Bitmap.Config.ARGB_8888)
    }

    private fun blurPass(src: IntArray, dst: IntArray, width: Int, height: Int, radius: Int, horizontal: Boolean) {
        val lines = if (horizontal) height else width
        val length = if (horizontal) width else height
        val window = radius * 2 + 1
        for (line in 0 until lines) {
            fun index(i: Int): Int {
                val k = i.coerceIn(0, length - 1)
                return if (horizontal) line * width + k else k * width + line
            }
            var sa = 0
            var sr = 0
            var sg = 0
            var sb = 0
            for (i in -radius..radius) {
                val p = src[index(i)]
                sa += p ushr 24
                sr += p shr 16 and 0xFF
                sg += p shr 8 and 0xFF
                sb += p and 0xFF
            }
            for (i in 0 until length) {
                dst[index(i)] = (sa / window shl 24) or (sr / window shl 16) or (sg / window shl 8) or (sb / window)
                val leaving = src[index(i - radius)]
                val entering = src[index(i + radius + 1)]
                sa += (entering ushr 24) - (leaving ushr 24)
                sr += (entering shr 16 and 0xFF) - (leaving shr 16 and 0xFF)
                sg += (entering shr 8 and 0xFF) - (leaving shr 8 and 0xFF)
                sb += (entering and 0xFF) - (leaving and 0xFF)
            }
        }
    }
}
