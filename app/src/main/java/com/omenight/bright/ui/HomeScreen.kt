package com.omenight.bright.ui

import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.background
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.intl.LocaleList
import androidx.compose.ui.text.toUpperCase
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.util.Consumer
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.omenight.bright.HomeState
import com.omenight.bright.HomeViewModel
import com.omenight.bright.R
import com.omenight.bright.data.LaunchableApp
import com.omenight.bright.data.Updater
import com.omenight.bright.ui.glass.LocalBackdrop
import com.omenight.bright.ui.glass.backdrop
import com.omenight.bright.ui.glass.glass
import com.omenight.bright.ui.glass.BackdropEngine
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
    val wallpaper by viewModel.wallpaper.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val resources = LocalResources.current
    val backdrop by remember { BackdropEngine.start(context) }.collectAsStateWithLifecycle()
    var allAppsOpen by rememberSaveable { mutableStateOf(false) }
    var settingsOpen by rememberSaveable { mutableStateOf(false) }
    // Where focus goes back to when settings closes
    var settingsFromGear by remember { mutableStateOf(false) }
    val welcomed by viewModel.welcomed.collectAsStateWithLifecycle()
    // Hides the guide at once, the saved flag arrives only after the DataStore write
    var welcomeDismissed by remember { mutableStateOf(false) }
    val showWelcome = welcomed == false && !welcomeDismissed
    // Set once All apps has been closed, so home focuses its tile instead of the first favorite
    var returnedFromAllApps by remember { mutableStateOf(false) }
    // Bumped when an overlay closes, so home takes focus back
    var homeFocusEpoch by remember { mutableIntStateOf(0) }
    // The app whose menu is shown, kept after closing so the exit animation still has it
    var menuApp by remember { mutableStateOf<LaunchableApp?>(null) }
    var menuOpen by remember { mutableStateOf(false) }
    // The tile that takes focus back once the app menu closes, bumped epoch triggers the move
    var refocusApp by remember { mutableStateOf<String?>(null) }
    var refocusEpoch by remember { mutableIntStateOf(0) }
    var notice by remember { mutableStateOf<NoticeMessage?>(null) }
    val update by viewModel.update.collectAsStateWithLifecycle()
    // Announced once per version, the dot on the gear stays as the reminder
    var announcedUpdate by rememberSaveable { mutableStateOf<String?>(null) }
    LaunchedEffect(update) {
        val ready = update as? Updater.State.Ready ?: return@LaunchedEffect
        if (announcedUpdate == ready.version) return@LaunchedEffect
        announcedUpdate = ready.version
        notice = NoticeMessage(resources.getString(R.string.notice_update_ready, ready.version))
    }
    val closeAllApps = {
        if (allAppsOpen) returnedFromAllApps = true
        allAppsOpen = false
    }
    val dismissWelcome: () -> Unit = {
        welcomeDismissed = true
        viewModel.finishWelcome()
        homeFocusEpoch++
    }
    val openAppMenu = { app: LaunchableApp ->
        menuApp = app
        menuOpen = true
    }
    val closeAppMenu = { focus: String? ->
        menuOpen = false
        refocusApp = focus
        refocusEpoch++
    }
    // The tile next to [app] on the current screen, for when [app] is about to leave it
    val neighbor = { app: LaunchableApp ->
        val list = if (allAppsOpen) state.apps else state.favorites
        val index = list.indexOf(app)
        (list.getOrNull(index + 1) ?: list.getOrNull(index - 1))?.packageName
    }
    val closeSettings = {
        if (settingsOpen) {
            BackdropEngine.preview(null)
            settingsOpen = false
            homeFocusEpoch++
        }
    }

    // Pressing Home while Bright is already in front arrives as a new intent
    val activity = LocalActivity.current as? ComponentActivity
    DisposableEffect(activity) {
        val listener = Consumer<Intent> {
            menuOpen = false
            closeSettings()
            closeAllApps()
        }
        activity?.addOnNewIntentListener(listener)
        onDispose { activity?.removeOnNewIntentListener(listener) }
    }

    // A launcher is the bottom of the stack, so Back only ever closes All apps or settings
    BackHandler { closeAllApps() }
    // Registered later, so it wins while the panel is open
    BackHandler(enabled = settingsOpen) { closeSettings() }
    BackHandler(enabled = showWelcome) { dismissWelcome() }
    BackHandler(enabled = menuOpen) { closeAppMenu(menuApp?.packageName) }

    LifecycleResumeEffect(viewModel) {
        viewModel.onResume()
        onPauseOrDispose { }
    }

    val sheet by animateFloatAsState(if (allAppsOpen) 1f else 0f, tween(240), label = "sheet")

    CompositionLocalProvider(LocalBackdrop provides backdrop) {
        Box(
            Modifier
                .fillMaxSize()
                // Read inside the lambda so the fade only redraws, never recomposes
                .backdrop(backdrop, fallback = BrightColors.Background) { sheet }
                .onPreviewKeyEvent { event ->
                    val menu = event.key == Key.Menu && event.type == KeyEventType.KeyUp && !allAppsOpen && !showWelcome
                    if (menu) {
                        if (menuOpen) {
                            closeAppMenu(menuApp?.packageName)
                        } else if (settingsOpen) {
                            closeSettings()
                        } else {
                            settingsFromGear = false
                            settingsOpen = true
                        }
                    }
                    menu
                },
        ) {
            AnimatedContent(
                targetState = allAppsOpen,
                transitionSpec = { fadeIn(tween(240)) togetherWith fadeOut(tween(180)) },
                label = "screen",
            ) { showAllApps ->
                if (showAllApps) {
                    AllAppsScreen(state, viewModel, openAppMenu, refocusApp, refocusEpoch)
                } else {
                    HomeScreen(
                        state = state,
                        viewModel = viewModel,
                        focusAllApps = returnedFromAllApps,
                        focusEpoch = homeFocusEpoch,
                        focusGear = settingsFromGear,
                        updateReady = update is Updater.State.Ready,
                        refocusApp = refocusApp,
                        refocusEpoch = refocusEpoch,
                        onAppMenu = openAppMenu,
                        onOpenAllApps = { allAppsOpen = true },
                        onOpenSettings = {
                            settingsFromGear = true
                            settingsOpen = true
                        },
                    )
                }
            }

            AnimatedVisibility(
                visible = settingsOpen,
                enter = fadeIn(tween(220)) + slideInHorizontally(tween(260)) { it / 5 },
                exit = fadeOut(tween(160)) + slideOutHorizontally(tween(200)) { it / 5 },
                modifier = Modifier.align(Alignment.CenterEnd),
            ) {
                SettingsPanel(
                    current = wallpaper,
                    hidden = state.hidden,
                    open = settingsOpen,
                    onPreview = BackdropEngine::preview,
                    onSelect = viewModel::setWallpaper,
                    onUnhide = { app ->
                        viewModel.unhide(app)
                        notice = NoticeMessage(resources.getString(R.string.notice_unhidden, app.label))
                    },
                    onOpenSystemSettings = {
                        // Closed first, so coming back from system settings lands on home
                        closeSettings()
                        viewModel.openSystemSettings()
                    },
                    update = update,
                    version = viewModel.version,
                    onUpdate = viewModel::update,
                )
            }

            AnimatedVisibility(
                visible = menuOpen,
                enter = fadeIn(tween(220)) + slideInHorizontally(tween(260)) { it / 5 },
                exit = fadeOut(tween(160)) + slideOutHorizontally(tween(200)) { it / 5 },
                modifier = Modifier.align(Alignment.CenterEnd),
            ) {
                val app = menuApp ?: return@AnimatedVisibility
                val isFavorite = app.packageName in state.favoritePackages
                AppMenu(
                    app = app,
                    isFavorite = isFavorite,
                    open = menuOpen,
                    onToggleFavorite = {
                        // Removing from the dock takes the tile away, so focus moves to its neighbour
                        closeAppMenu(if (isFavorite && !allAppsOpen) neighbor(app) else app.packageName)
                        viewModel.toggleFavorite(app)
                        val text = if (isFavorite) R.string.notice_favorite_removed else R.string.notice_favorite_added
                        notice = NoticeMessage(resources.getString(text, app.label))
                    },
                    onHide = {
                        closeAppMenu(neighbor(app))
                        viewModel.hide(app)
                        notice = NoticeMessage(resources.getString(R.string.notice_hidden, app.label))
                    },
                    onAppInfo = {
                        closeAppMenu(app.packageName)
                        viewModel.openAppInfo(app)
                    },
                )
            }

            Notice(
                notice,
                Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 40.dp),
            )

            AnimatedVisibility(visible = showWelcome, enter = fadeIn(tween(300)), exit = fadeOut(tween(200))) {
                WelcomeGuide(open = showWelcome, onDone = dismissWelcome)
            }
        }
    }
}

@Composable
private fun HomeScreen(
    state: HomeState,
    viewModel: HomeViewModel,
    focusAllApps: Boolean,
    focusEpoch: Int,
    focusGear: Boolean,
    updateReady: Boolean,
    refocusApp: String?,
    refocusEpoch: Int,
    onAppMenu: (LaunchableApp) -> Unit,
    onOpenAllApps: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val tiles = remember { TileFocus() }
    val allAppsTile = remember { FocusRequester() }
    val gear = remember { FocusRequester() }
    val initialFocus = when {
        focusGear && focusEpoch > 0 -> gear
        focusAllApps || state.favorites.isEmpty() -> allAppsTile
        else -> tiles[state.favorites.first().packageName]
    }
    RequestFocusOnEnter(initialFocus, state.loaded, key = focusEpoch)
    RefocusAfterMenu(tiles, refocusApp, refocusEpoch, fallback = allAppsTile)

    Box(Modifier.fillMaxSize()) {
        Clock(
            Modifier
                .align(Alignment.TopStart)
                .padding(start = ScreenPadding, top = 44.dp),
        )

        // Top-right status corner, Up from the dock lands on the gear
        Row(
            Modifier
                .align(Alignment.TopEnd)
                .padding(end = ScreenPadding - 6.dp, top = 52.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val network by viewModel.network.collectAsStateWithLifecycle()
            NetworkIndicator(network)
            Spacer(Modifier.width(14.dp))
            val settingsLabel = stringResource(R.string.settings)
            Surface(
                onClick = onOpenSettings,
                modifier = Modifier
                    .focusRequester(gear)
                    .semantics { contentDescription = settingsLabel },
                shape = ClickableSurfaceDefaults.shape(CircleShape),
                colors = ClickableSurfaceDefaults.colors(
                    containerColor = Color.Transparent,
                    focusedContainerColor = Color.White.copy(alpha = 0.16f),
                    pressedContainerColor = Color.White.copy(alpha = 0.24f),
                    contentColor = BrightColors.Text,
                    focusedContentColor = BrightColors.Text,
                ),
                scale = ClickableSurfaceDefaults.scale(focusedScale = 1.1f),
            ) {
                Box {
                    GlyphIcon(Glyph.Gear, size = 26.dp, modifier = Modifier.padding(10.dp))
                    if (updateReady) {
                        Box(
                            Modifier
                                .align(Alignment.TopEnd)
                                .padding(6.dp)
                                .size(9.dp)
                                .background(BrightColors.Text, CircleShape),
                        )
                    }
                }
            }
        }

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
                // Padding lives inside the row so focused tiles can lift and cast shadow without clipping.
                // Full width too, a lazy row clips along its axis and would cut the last tile's shadow.
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(start = DockPadding, end = DockPadding, top = 24.dp, bottom = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(CardGap),
                ) {
                    items(state.favorites, key = { it.packageName }) { app ->
                        AppCard(
                            app = app,
                            isFavorite = false,
                            loadArt = viewModel::art,
                            onClick = { viewModel.launch(app) },
                            onLongClick = { onAppMenu(app) },
                            modifier = Modifier.width(cardWidth),
                            cardModifier = Modifier.focusRequester(tiles[app.packageName]),
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

/** One focus requester per package, made on first use so lazy rows and grids can address any item. */
class TileFocus {
    private val requesters = HashMap<String, FocusRequester>()

    operator fun get(packageName: String): FocusRequester = requesters.getOrPut(packageName) { FocusRequester() }

    fun existing(packageName: String): FocusRequester? = requesters[packageName]
}

/** Puts focus back on the tile of [app] after the app menu closes, or on [fallback] when that tile is gone. */
@Composable
fun RefocusAfterMenu(tiles: TileFocus, app: String?, epoch: Int, fallback: FocusRequester?) {
    LaunchedEffect(epoch) {
        if (epoch == 0) return@LaunchedEffect
        withFrameNanos { }
        val target = app?.let(tiles::existing) ?: fallback ?: return@LaunchedEffect
        runCatching { target.requestFocus() }
    }
}

/** Moves focus to [target] once the screen is shown and [ready], and again whenever [key] changes. */
@Composable
fun RequestFocusOnEnter(target: FocusRequester, ready: Boolean, key: Any? = null) {
    LaunchedEffect(target, ready, key) {
        if (!ready) return@LaunchedEffect
        // Lazy items attach during layout, so wait one frame before requesting
        withFrameNanos { }
        runCatching { target.requestFocus() }
    }
}

@Composable
fun SectionTitle(@StringRes title: Int, modifier: Modifier = Modifier) {
    Text(
        text = stringResource(title).toUpperCase(LocaleList.current),
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
