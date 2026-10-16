package com.omenight.bright.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.omenight.bright.R
import com.omenight.bright.data.AppArt
import com.omenight.bright.data.LaunchableApp
import com.omenight.bright.ui.theme.BrightColors

@Composable
fun AppCard(
    app: LaunchableApp,
    isFavorite: Boolean,
    loadArt: suspend (LaunchableApp) -> AppArt,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
    cardModifier: Modifier = Modifier,
) {
    val art by produceState<AppArt?>(null, app.packageName) { value = loadArt(app) }

    BrightCard(
        label = app.label,
        onClick = onClick,
        onLongClick = onLongClick,
        modifier = modifier,
        cardModifier = cardModifier,
        // Apps without a TV banner sit on glass instead of an empty box
        glassBackground = art !is AppArt.Banner,
    ) {
        when (val current = art) {
            is AppArt.Banner -> Image(
                bitmap = current.image,
                contentDescription = app.label,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )

            is AppArt.Icon -> Image(
                bitmap = current.image,
                contentDescription = app.label,
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxHeight(0.5f)
                    .aspectRatio(1f),
            )

            null -> Unit
        }
        if (isFavorite) {
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .size(6.dp)
                    .background(BrightColors.Text, CircleShape),
            )
        }
    }
}

@Composable
fun AllAppsCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    cardModifier: Modifier = Modifier,
) {
    BrightCard(
        label = stringResource(R.string.all_apps),
        onClick = onClick,
        modifier = modifier,
        cardModifier = cardModifier,
        glassBackground = true,
    ) {
        GlyphIcon(Glyph.Grid, size = 32.dp, modifier = Modifier.align(Alignment.Center))
    }
}
