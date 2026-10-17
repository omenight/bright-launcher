package com.omenight.bright.ui

import android.content.ActivityNotFoundException
import android.os.SystemClock
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.omenight.bright.R
import com.omenight.bright.data.HomeRole
import com.omenight.bright.ui.theme.BrightColors

/** Shown once on first launch. Teaches the three things a remote user cannot discover alone. */
@Composable
fun WelcomeGuide(
    /** False while the guide fades out, so home can take focus during the exit. */
    open: Boolean,
    onDone: () -> Unit,
) {
    val context = LocalContext.current
    val homeRole = remember { HomeRole(context) }
    var isDefaultHome by remember { mutableStateOf(homeRole.isDefault()) }
    // Shown when this TV has no home picker, or refuses third-party homes
    var showAdbHint by remember { mutableStateOf(false) }
    var pickerOpenedAt by remember { mutableLongStateOf(0L) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        isDefaultHome = homeRole.isDefault()
        // TVs that block other home apps (Google TV) bounce straight back, a person takes longer to cancel
        val bounced = SystemClock.elapsedRealtime() - pickerOpenedAt < 1_000
        if (!isDefaultHome && bounced) showAdbHint = true
    }
    // Settings screens return without a result, so check again whenever we come back
    LifecycleResumeEffect(Unit) {
        isDefaultHome = homeRole.isDefault()
        onPauseOrDispose { }
    }

    val gotIt = remember { FocusRequester() }
    RequestFocusOnEnter(gotIt, ready = true)

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.35f)),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            Modifier
                .width(580.dp)
                .surface(30.dp)
                .padding(horizontal = 40.dp, vertical = 36.dp)
                .focusProperties { onExit = { if (open) cancelFocusChange() } }
                .focusGroup(),
        ) {
            Text(
                text = stringResource(R.string.welcome_title),
                style = TextStyle(fontSize = 32.sp, fontWeight = FontWeight.Light),
            )
            Text(
                text = stringResource(R.string.welcome_subtitle),
                color = BrightColors.TextMuted,
                style = TextStyle(fontSize = 16.sp),
                modifier = Modifier.padding(top = 6.dp),
            )
            Spacer(Modifier.height(28.dp))
            Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
                Tip(Glyph.Pin, stringResource(R.string.tip_favorites))
                Tip(Glyph.Grid, stringResource(R.string.tip_all_apps))
                Tip(Glyph.Gear, stringResource(R.string.tip_settings))
                if (!isDefaultHome) Tip(Glyph.House, stringResource(R.string.tip_home))
            }
            if (!isDefaultHome && showAdbHint) {
                Text(
                    text = stringResource(R.string.home_adb_hint, "${context.packageName}/com.omenight.bright.MainActivity"),
                    color = BrightColors.TextMuted,
                    style = TextStyle(fontSize = 13.sp, fontFamily = FontFamily.Monospace),
                    modifier = Modifier.padding(start = 46.dp, top = 10.dp),
                )
            }
            Spacer(Modifier.height(32.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                if (!isDefaultHome && !showAdbHint) {
                    GuideButton(stringResource(R.string.set_as_home), primary = false) {
                        val intent = homeRole.requestIntent()
                        if (intent == null) {
                            showAdbHint = true
                        } else {
                            try {
                                pickerOpenedAt = SystemClock.elapsedRealtime()
                                picker.launch(intent)
                            } catch (_: ActivityNotFoundException) {
                                showAdbHint = true
                            }
                        }
                    }
                }
                GuideButton(
                    text = stringResource(R.string.got_it),
                    primary = true,
                    modifier = Modifier.focusRequester(gotIt),
                    onClick = onDone,
                )
            }
        }
    }
}

@Composable
private fun Tip(glyph: Glyph, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.width(30.dp), contentAlignment = Alignment.Center) {
            GlyphIcon(glyph, size = 24.dp)
        }
        Text(
            text = text,
            style = TextStyle(fontSize = 17.sp),
            modifier = Modifier.padding(start = 16.dp),
        )
    }
}

// Focused buttons turn solid white with dark text, the strongest focus cue Bright uses
@Composable
private fun GuideButton(text: String, primary: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(50)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = Color.White.copy(alpha = if (primary) 0.18f else 0.08f),
            focusedContainerColor = BrightColors.Text,
            pressedContainerColor = BrightColors.Text.copy(alpha = 0.85f),
            contentColor = BrightColors.Text,
            focusedContentColor = Color(0xFF101014),
            pressedContentColor = Color(0xFF101014),
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.04f),
    ) {
        Text(
            text = text,
            style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Medium),
            modifier = Modifier.padding(horizontal = 26.dp, vertical = 12.dp),
        )
    }
}
