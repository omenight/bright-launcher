package com.omenight.bright.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.omenight.bright.R
import com.omenight.bright.ui.glass.WallpaperPreset
import com.omenight.bright.ui.glass.glass
import com.omenight.bright.ui.theme.BrightColors

/**
 * Side panel holding Bright's few settings. Focusing a wallpaper previews it on the real
 * background; OK keeps it.
 */
@Composable
fun SettingsPanel(
    current: WallpaperPreset,
    /** False while the panel animates out, so focus can return to home during the exit. */
    open: Boolean,
    onPreview: (WallpaperPreset) -> Unit,
    onSelect: (WallpaperPreset) -> Unit,
    modifier: Modifier = Modifier,
) {
    val currentRow = remember { FocusRequester() }
    RequestFocusOnEnter(currentRow, ready = true)

    Column(
        modifier
            .fillMaxHeight()
            .width(360.dp)
            .padding(24.dp)
            .glass(28.dp, tint = Color.Black.copy(alpha = 0.28f))
            .padding(horizontal = 16.dp, vertical = 28.dp)
            // Keeps D-pad focus inside the panel, the home screen behind it is not reachable
            .focusProperties { onExit = { if (open) cancelFocusChange() } }
            .focusGroup(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = stringResource(R.string.settings),
            style = TextStyle(fontSize = 26.sp, fontWeight = FontWeight.Light, shadow = BrightColors.TextShadow),
            modifier = Modifier.padding(start = 12.dp),
        )
        Spacer(Modifier.height(20.dp))
        SectionTitle(R.string.wallpaper, Modifier.padding(start = 12.dp, bottom = 8.dp))
        WallpaperPreset.entries.forEach { preset ->
            SettingsRow(
                title = stringResource(preset.title),
                selected = preset == current,
                onFocused = { onPreview(preset) },
                onClick = { onSelect(preset) },
                modifier = if (preset == current) Modifier.focusRequester(currentRow) else Modifier,
                leading = { Swatch(preset) },
            )
        }
    }
}

@Composable
private fun SettingsRow(
    title: String,
    selected: Boolean,
    onFocused: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leading: @Composable () -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .onFocusChanged { if (it.isFocused) onFocused() },
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(14.dp)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = Color.Transparent,
            focusedContainerColor = Color.White.copy(alpha = 0.14f),
            pressedContainerColor = Color.White.copy(alpha = 0.20f),
            contentColor = BrightColors.Text,
            focusedContentColor = BrightColors.Text,
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1f),
    ) {
        Row(
            Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            leading()
            Text(
                text = title,
                style = TextStyle(fontSize = 17.sp),
                modifier = Modifier
                    .padding(start = 14.dp)
                    .weight(1f),
            )
            if (selected) {
                Box(
                    Modifier
                        .size(7.dp)
                        .background(BrightColors.Text, CircleShape),
                )
            }
        }
    }
}

// A tiny round preview of the preset, its strongest glow over its base
@Composable
private fun Swatch(preset: WallpaperPreset) {
    val glow = preset.glows.first()
    Box(
        Modifier
            .size(24.dp)
            .background(
                Brush.linearGradient(
                    listOf(Color(0xFF000000.toInt() or preset.base[1]), Color(0xFF000000.toInt() or glow.rgb)),
                ),
                CircleShape,
            ),
    )
}
