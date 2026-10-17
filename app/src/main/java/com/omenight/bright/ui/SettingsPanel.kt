package com.omenight.bright.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.omenight.bright.R
import com.omenight.bright.data.LaunchableApp
import com.omenight.bright.data.Updater
import com.omenight.bright.ui.glass.WallpaperPreset
import com.omenight.bright.ui.theme.BrightColors

/**
 * Side panel opened by the gear or the Menu key. The menu leads to one page per option and to the
 * system settings. Focusing a wallpaper previews it on the real background; OK keeps it.
 */
@Composable
fun SettingsPanel(
    current: WallpaperPreset,
    hidden: List<LaunchableApp>,
    open: Boolean,
    /** Called with null when focus leaves the wallpapers, so the saved one shows again. */
    onPreview: (WallpaperPreset?) -> Unit,
    onSelect: (WallpaperPreset) -> Unit,
    onUnhide: (LaunchableApp) -> Unit,
    onOpenSystemSettings: () -> Unit,
    update: Updater.State,
    version: String,
    onUpdate: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var page by remember { mutableStateOf(Page.Menu) }
    // Registered after the root handlers, so Back on a page returns to the menu instead of closing
    BackHandler(enabled = open && page != Page.Menu) {
        onPreview(null)
        page = Page.Menu
    }
    // The last hidden app was brought back, an empty page would leave nothing to focus
    if (page == Page.Hidden && hidden.isEmpty()) page = Page.Menu

    val title = when (page) {
        Page.Menu -> R.string.settings
        Page.Wallpaper -> R.string.wallpaper
        Page.Hidden -> R.string.hidden_apps
    }
    SidePanel(stringResource(title), open, modifier) {
        when (page) {
            Page.Menu -> MenuPage(
                current = current,
                hiddenCount = hidden.size,
                onOpenWallpapers = { page = Page.Wallpaper },
                onOpenHidden = { page = Page.Hidden },
                onOpenSystemSettings = onOpenSystemSettings,
                update = update,
                version = version,
                onUpdate = onUpdate,
            )
            Page.Wallpaper -> WallpaperPage(current, onPreview, onSelect)
            Page.Hidden -> HiddenPage(hidden, onUnhide)
        }
    }
}

private enum class Page { Menu, Wallpaper, Hidden }

@Composable
private fun MenuPage(
    current: WallpaperPreset,
    hiddenCount: Int,
    onOpenWallpapers: () -> Unit,
    onOpenHidden: () -> Unit,
    onOpenSystemSettings: () -> Unit,
    update: Updater.State,
    version: String,
    onUpdate: () -> Unit,
) {
    val firstRow = remember { FocusRequester() }
    RequestFocusOnEnter(firstRow, ready = true)

    PanelRow(
        title = stringResource(R.string.wallpaper),
        onClick = onOpenWallpapers,
        modifier = Modifier.focusRequester(firstRow),
        leading = { Swatch(current) },
        trailing = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                RowValue(stringResource(current.title))
                Chevron()
            }
        },
    )
    // Only shown once something is hidden, the page would otherwise be empty
    if (hiddenCount > 0) {
        PanelRow(
            title = stringResource(R.string.hidden_apps),
            onClick = onOpenHidden,
            leading = { RowGlyph(Glyph.Hide) },
            trailing = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RowValue(hiddenCount.toString())
                    Chevron()
                }
            },
        )
    }
    PanelRow(
        title = stringResource(R.string.system_settings),
        onClick = onOpenSystemSettings,
        leading = { RowGlyph(Glyph.Gear) },
        trailing = { Chevron() },
    )
    if (update != Updater.State.Disabled) {
        // The version is the title, so the state fits as one short word even on a 720p panel
        PanelRow(
            title = "${stringResource(R.string.app_name)} $version",
            onClick = onUpdate,
            leading = { RowGlyph(Glyph.Update) },
            trailing = { RowValue(updateValue(update)) },
        )
    }
}

@Composable
private fun updateValue(update: Updater.State): String = when (update) {
    Updater.State.Disabled, Updater.State.Idle -> stringResource(R.string.update_check)
    Updater.State.Checking -> stringResource(R.string.update_checking)
    Updater.State.UpToDate -> stringResource(R.string.update_up_to_date)
    is Updater.State.Downloading -> stringResource(R.string.update_downloading)
    is Updater.State.Ready -> stringResource(R.string.update_install, update.version)
    is Updater.State.Installing -> stringResource(R.string.update_installing)
    Updater.State.Failed -> stringResource(R.string.update_failed)
}

@Composable
private fun WallpaperPage(
    current: WallpaperPreset,
    onPreview: (WallpaperPreset?) -> Unit,
    onSelect: (WallpaperPreset) -> Unit,
) {
    val currentRow = remember { FocusRequester() }
    RequestFocusOnEnter(currentRow, ready = true)

    WallpaperPreset.entries.forEach { preset ->
        PanelRow(
            title = stringResource(preset.title),
            onFocused = { onPreview(preset) },
            onClick = { onSelect(preset) },
            modifier = if (preset == current) Modifier.focusRequester(currentRow) else Modifier,
            leading = { Swatch(preset) },
            trailing = if (preset == current) {
                { Box(Modifier.size(7.dp).background(BrightColors.Text, CircleShape)) }
            } else {
                null
            },
        )
    }
}

@Composable
private fun HiddenPage(hidden: List<LaunchableApp>, onUnhide: (LaunchableApp) -> Unit) {
    val rows = remember { TileFocus() }
    // The row that takes focus once the one just brought back disappears
    var next by remember { mutableStateOf(hidden.firstOrNull()?.packageName) }
    LaunchedEffect(hidden) {
        withFrameNanos { }
        next?.let { runCatching { rows.existing(it)?.requestFocus() } }
    }

    val showLabel = stringResource(R.string.show_app)
    Column(Modifier.verticalScroll(rememberScrollState())) {
        hidden.forEachIndexed { index, app ->
            PanelRow(
                title = app.label,
                onClick = {
                    next = (hidden.getOrNull(index + 1) ?: hidden.getOrNull(index - 1))?.packageName
                    onUnhide(app)
                },
                modifier = Modifier.focusRequester(rows[app.packageName]),
                trailing = { RowValue(showLabel) },
            )
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
