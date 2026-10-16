package com.omenight.bright.ui

import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.util.Consumer
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.tv.material3.Text
import com.omenight.bright.HomeState
import com.omenight.bright.HomeViewModel
import com.omenight.bright.R
import com.omenight.bright.ui.glass.LocalBackdrop
import com.omenight.bright.ui.glass.backdrop
import com.omenight.bright.ui.glass.glass
import com.omenight.bright.ui.glass.rememberDawnBackdrop
import com.omenight.bright.ui.theme.BrightColors

const val Columns = 5
val CardGap = 22.dp
val ScreenPadding = 56.dp

private val DockMargin = 40.dp
private val DockPadding = 28.dp
private val DockCorner = 30.dp

@Composable
fun LauncherRoot(viewModel: HomeViewModel = viewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val backdrop = rememberDawnBackdrop()
    var allAppsOpen by rememberSaveable { mutableStateOf(false) }
    // Set once All apps has been closed, so home focuses its tile instead of the first favorite
    var returnedFromAllApps by remember { mutableStateOf(false) }
    val closeAllApps = {
        if (allAppsOpen) returnedFromAllApps = true
        allAppsOpen = false
    }

    // Pressing Home while Bright is already in front arrives as a new intent
    val activity = LocalActivity.current as? ComponentActivity
    DisposableEffect(activity) {
        val listener = Consumer<Intent> { closeAllApps() }
        activity?.addOnNewIntentListener(listener)
        onDispose { activity?.removeOnNewIntentListener(listener) }
    }

    // A launcher is the bottom of the stack, so Back only ever closes All apps
    BackHandler { closeAllApps() }

    val sheet by animateFloatAsState(if (allAppsOpen) 1f else 0f, tween(240), label = "sheet")

    CompositionLocalProvider(LocalBackdrop provides backdrop) {
        Box(
            Modifier
                .fillMaxSize()
                // Read inside the lambda so the fade only redraws, never recomposes
                .backdrop(backdrop, fallback = BrightColors.Background) { sheet },
        ) {
            AnimatedContent(
                targetState = allAppsOpen,
                transitionSpec = { fadeIn(tween(240)) togetherWith fadeOut(tween(180)) },
                label = "screen",
            ) { showAllApps ->
                if (showAllApps) {
                    AllAppsScreen(state, viewModel)
                } else {
                    HomeScreen(
                        state = state,
                        viewModel = viewModel,
                        focusAllApps = returnedFromAllApps,
                        onOpenAllApps = { allAppsOpen = true },
                    )
                }
            }
        }
    }
}

@Composable
private fun HomeScreen(
    state: HomeState,
    viewModel: HomeViewModel,
    focusAllApps: Boolean,
    onOpenAllApps: () -> Unit,
) {
    val firstFavorite = remember { FocusRequester() }
    val allAppsTile = remember { FocusRequester() }
    val initialFocus = if (focusAllApps || state.favorites.isEmpty()) allAppsTile else firstFavorite
    RequestFocusOnEnter(initialFocus, state.loaded)

    Box(Modifier.fillMaxSize()) {
        Clock(
            Modifier
                .align(Alignment.TopStart)
                .padding(start = ScreenPadding, top = 44.dp),
        )

        // The glass dock, the one large glass surface on home
        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(start = DockMargin, end = DockMargin, bottom = 32.dp)
                .glass(DockCorner),
        ) {
            if (state.loaded && state.favorites.isEmpty()) {
                Text(
                    text = stringResource(R.string.favorites_empty),
                    color = BrightColors.TextMuted,
                    style = TextStyle(fontSize = 14.sp, shadow = BrightColors.TextShadow),
                    modifier = Modifier.padding(start = DockPadding, top = 20.dp),
                )
            }
            BoxWithConstraints(Modifier.fillMaxWidth()) {
                val cardWidth = (maxWidth - DockPadding * 2 - CardGap * (Columns - 1)) / Columns
                // Padding lives inside the row so focused tiles can lift and cast shadow without clipping
                LazyRow(
                    contentPadding = PaddingValues(start = DockPadding, end = DockPadding, top = 24.dp, bottom = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(CardGap),
                ) {
                    items(state.favorites, key = { it.packageName }) { app ->
                        AppCard(
                            app = app,
                            isFavorite = false,
                            loadArt = viewModel::art,
                            onClick = { viewModel.launch(app) },
                            onLongClick = { viewModel.toggleFavorite(app) },
                            modifier = Modifier.width(cardWidth),
                            cardModifier = if (app == state.favorites.first()) {
                                Modifier.focusRequester(firstFavorite)
                            } else {
                                Modifier
                            },
                        )
                    }
                    item(key = "all-apps") {
                        AllAppsCard(
                            onClick = onOpenAllApps,
                            modifier = Modifier.width(cardWidth),
                            cardModifier = Modifier.focusRequester(allAppsTile),
                        )
                    }
                }
            }
        }
    }
}

/** Moves focus to [target] once the screen is shown and [ready]. */
@Composable
fun RequestFocusOnEnter(target: FocusRequester, ready: Boolean) {
    LaunchedEffect(target, ready) {
        if (!ready) return@LaunchedEffect
        // Lazy items attach during layout, so wait one frame before requesting
        withFrameNanos { }
        runCatching { target.requestFocus() }
    }
}

@Composable
fun SectionTitle(@StringRes title: Int, modifier: Modifier = Modifier) {
    Text(
        text = stringResource(title).uppercase(),
        style = TextStyle(
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 2.sp,
            shadow = BrightColors.TextShadow,
        ),
        color = BrightColors.TextMuted,
        modifier = modifier,
    )
}
