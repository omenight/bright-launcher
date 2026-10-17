package com.omenight.bright.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.res.stringResource
import com.omenight.bright.R
import com.omenight.bright.data.LaunchableApp

/** What holding OK on an app tile opens. */
@Composable
fun AppMenu(
    app: LaunchableApp,
    isFavorite: Boolean,
    open: Boolean,
    onToggleFavorite: () -> Unit,
    onHide: () -> Unit,
    onAppInfo: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val firstRow = remember { FocusRequester() }
    RequestFocusOnEnter(firstRow, ready = true)
    // The menu opens while OK is still held, so its repeats and release must not press the first row
    var armed by remember { mutableStateOf(false) }

    SidePanel(
        title = app.label,
        open = open,
        modifier = modifier.onPreviewKeyEvent { event ->
            val confirm = event.key == Key.DirectionCenter || event.key == Key.Enter || event.key == Key.NumPadEnter
            if (!confirm || armed) return@onPreviewKeyEvent false
            armed = event.type == KeyEventType.KeyDown && event.nativeKeyEvent.repeatCount == 0
            !armed
        },
    ) {
        PanelRow(
            title = stringResource(if (isFavorite) R.string.app_favorite_remove else R.string.app_favorite_add),
            onClick = onToggleFavorite,
            modifier = Modifier.focusRequester(firstRow),
            leading = { RowGlyph(Glyph.Pin) },
        )
        PanelRow(
            title = stringResource(R.string.app_hide),
            onClick = onHide,
            leading = { RowGlyph(Glyph.Hide) },
        )
        PanelRow(
            title = stringResource(R.string.app_info),
            onClick = onAppInfo,
            leading = { RowGlyph(Glyph.Info) },
            trailing = { Chevron() },
        )
    }
}
