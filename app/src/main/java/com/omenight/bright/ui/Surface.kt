package com.omenight.bright.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import com.omenight.bright.ui.wallpaper.LocalBackdrop

private val FallbackSurface = Color(0xFF1B1D24)

/** Bright's one material, an opaque surface in the wallpaper's own hue. */
fun Modifier.surface(cornerRadius: Dp): Modifier = composed {
    background(LocalBackdrop.current?.surface ?: FallbackSurface, RoundedCornerShape(cornerRadius))
}
