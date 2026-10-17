package com.omenight.bright.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.dp
import com.omenight.bright.HomeState
import com.omenight.bright.HomeViewModel
import com.omenight.bright.R
import com.omenight.bright.data.LaunchableApp

@Composable
fun AllAppsScreen(
    state: HomeState,
    viewModel: HomeViewModel,
    onAppMenu: (LaunchableApp) -> Unit,
    refocusApp: String?,
    refocusEpoch: Int,
) {
    val tiles = remember { TileFocus() }
    val firstApp = state.apps.firstOrNull()?.let { tiles[it.packageName] }
    if (firstApp != null) RequestFocusOnEnter(firstApp, ready = true)
    RefocusAfterMenu(tiles, refocusApp, refocusEpoch, fallback = firstApp)

    // The frosted sheet behind this screen is painted by the root backdrop, not here
    Column(Modifier.fillMaxSize()) {
        SectionTitle(R.string.all_apps, Modifier.padding(start = ScreenPadding, top = 40.dp))
        // The title stays put, only the grid scrolls
        LazyVerticalGrid(
            columns = GridCells.Fixed(Columns),
            contentPadding = PaddingValues(start = ScreenPadding, end = ScreenPadding, top = 20.dp, bottom = 40.dp),
            horizontalArrangement = Arrangement.spacedBy(CardGap),
            verticalArrangement = Arrangement.spacedBy(CardGap),
        ) {
            items(state.apps, key = { it.packageName }) { app ->
                AppCard(
                    app = app,
                    isFavorite = app.packageName in state.favoritePackages,
                    loadArt = viewModel::art,
                    onClick = { viewModel.launch(app) },
                    onLongClick = { onAppMenu(app) },
                    cardModifier = Modifier.focusRequester(tiles[app.packageName]),
                )
            }
        }
    }
}
