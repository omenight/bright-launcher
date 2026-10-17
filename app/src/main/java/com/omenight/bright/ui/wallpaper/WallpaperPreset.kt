package com.omenight.bright.ui.wallpaper

import androidx.annotation.StringRes
import com.omenight.bright.R

/** A soft light source painted over the base gradient, positions and radius relative to the width/height. */
class Glow(val x: Float, val y: Float, val radius: Float, val rgb: Int, val alpha: Float)

/**
 * Built-in wallpapers, described as data and rendered by [WallpaperBaker], so they cost
 * a few hundred bytes of code instead of image assets.
 */
enum class WallpaperPreset(
    @StringRes val title: Int,
    /** Top, middle and bottom of the base gradient. */
    val base: IntArray,
    val glows: List<Glow>,
) {
    Dawn(
        R.string.wallpaper_dawn,
        intArrayOf(0x060914, 0x0C1030, 0x22143A),
        listOf(
            Glow(0.10f, 1.10f, 0.70f, 0xFF9A62, 0.60f),
            Glow(0.50f, 1.05f, 0.50f, 0xC2457E, 0.38f),
            Glow(0.38f, 0.20f, 0.45f, 0x3A3DB0, 0.40f),
            Glow(0.95f, 0.10f, 0.55f, 0x1E6E8C, 0.35f),
        ),
    ),
    Dusk(
        R.string.wallpaper_dusk,
        intArrayOf(0x0B0716, 0x1E0E2A, 0x3A1426),
        listOf(
            Glow(0.85f, 1.10f, 0.70f, 0xFF7A3D, 0.55f),
            Glow(0.35f, 1.00f, 0.55f, 0xB03070, 0.40f),
            Glow(0.20f, 0.15f, 0.50f, 0x4B2A8F, 0.35f),
        ),
    ),
    Ocean(
        R.string.wallpaper_ocean,
        intArrayOf(0x020814, 0x06203A, 0x0A3550),
        listOf(
            Glow(0.20f, 1.05f, 0.65f, 0x1FA3A8, 0.45f),
            Glow(0.80f, 0.20f, 0.55f, 0x2050C8, 0.40f),
            Glow(0.60f, 1.10f, 0.40f, 0x3FD0E0, 0.25f),
        ),
    ),
    Forest(
        R.string.wallpaper_forest,
        intArrayOf(0x040A08, 0x0B1F18, 0x12302A),
        listOf(
            Glow(0.15f, 1.05f, 0.60f, 0x2E8B57, 0.45f),
            Glow(0.70f, 1.10f, 0.45f, 0x8BA84A, 0.25f),
            Glow(0.90f, 0.15f, 0.50f, 0x1E6E6A, 0.35f),
        ),
    ),
    Midnight(
        R.string.wallpaper_midnight,
        intArrayOf(0x000000, 0x050507, 0x0A0A0E),
        listOf(
            Glow(0.15f, 1.10f, 0.60f, 0x3A3A48, 0.35f),
            Glow(0.90f, 0.10f, 0.50f, 0x24242E, 0.30f),
        ),
    ),
}
