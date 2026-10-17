package com.omenight.bright.ui

import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.omenight.bright.ui.glass.glass
import com.omenight.bright.ui.theme.BrightColors

/** The glass side sheet every Bright menu lives in. */
@Composable
fun SidePanel(
    title: String,
    /** False while the panel animates out, so focus can return to the screen behind during the exit. */
    open: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier
            .fillMaxHeight()
            .width(360.dp)
            .padding(24.dp)
            .glass(28.dp, tint = Color.Black.copy(alpha = 0.28f))
            .padding(horizontal = 16.dp, vertical = 28.dp)
            // Keeps D-pad focus inside the panel, the screen behind it is not reachable
            .focusProperties { onExit = { if (open) cancelFocusChange() } }
            .focusGroup(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = title,
            style = TextStyle(fontSize = 26.sp, fontWeight = FontWeight.Light, shadow = BrightColors.TextShadow),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(start = 12.dp),
        )
        Spacer(Modifier.height(20.dp))
        content()
    }
}

@Composable
fun PanelRow(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    onFocused: () -> Unit = {},
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
            leading?.invoke()
            Text(
                text = title,
                style = TextStyle(fontSize = 17.sp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .padding(start = if (leading != null) 14.dp else 0.dp)
                    .weight(1f),
            )
            trailing?.invoke()
        }
    }
}

/** Marks a row that leads to another page or out of Bright. */
@Composable
fun Chevron() {
    GlyphIcon(Glyph.Chevron, size = 20.dp, color = BrightColors.TextMuted, modifier = Modifier.padding(start = 4.dp))
}

@Composable
fun RowGlyph(glyph: Glyph) {
    GlyphIcon(glyph, size = 24.dp, color = BrightColors.TextMuted)
}

@Composable
fun RowValue(text: String) {
    Text(
        text = text,
        color = BrightColors.TextMuted,
        style = TextStyle(fontSize = 15.sp),
        maxLines = 1,
        modifier = Modifier.padding(start = 12.dp),
    )
}
