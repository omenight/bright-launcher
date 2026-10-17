package com.omenight.bright.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import com.omenight.bright.ui.glass.glass
import kotlinx.coroutines.delay

/** One confirmation line, a new instance shows again even when the text repeats. */
class NoticeMessage(val text: String)

/** A short glass pill confirming an action, gone after a few seconds. Never takes focus. */
@Composable
fun Notice(message: NoticeMessage?, modifier: Modifier = Modifier) {
    var visible by remember { mutableStateOf(false) }
    // Kept through the exit animation, which still needs the text
    var shown by remember { mutableStateOf(message) }
    LaunchedEffect(message) {
        if (message == null) return@LaunchedEffect
        shown = message
        visible = true
        delay(2_800)
        visible = false
    }
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = fadeIn(tween(180)) + slideInVertically(tween(220)) { -it / 2 },
        exit = fadeOut(tween(220)),
    ) {
        Text(
            text = shown?.text.orEmpty(),
            style = TextStyle(fontSize = 16.sp, textAlign = TextAlign.Center),
            modifier = Modifier
                .widthIn(max = 640.dp)
                .glass(22.dp, tint = Color.Black.copy(alpha = 0.35f))
                .padding(horizontal = 24.dp, vertical = 12.dp),
        )
    }
}
