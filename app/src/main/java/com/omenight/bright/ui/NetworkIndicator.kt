package com.omenight.bright.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.omenight.bright.data.NetworkStatus
import com.omenight.bright.ui.theme.BrightColors

private val Lit = BrightColors.Text
private val Dim = BrightColors.Text.copy(alpha = 0.25f)

/** A drawn status glyph, so the APK carries no icon font or vector set. */
@Composable
fun NetworkIndicator(status: NetworkStatus, modifier: Modifier = Modifier) {
    Canvas(modifier.size(28.dp)) {
        when (status) {
            is NetworkStatus.Wifi -> wifi(bars = status.bars, crossed = false)
            NetworkStatus.Offline -> wifi(bars = -1, crossed = true)
            NetworkStatus.Ethernet, NetworkStatus.Other -> ethernet()
        }
    }
}

// A dot plus three concentric arcs opening upward, each arc lit when the signal reaches it
private fun DrawScope.wifi(bars: Int, crossed: Boolean) {
    val stroke = size.width * 0.09f
    val dotCenter = Offset(center.x, size.height * 0.80f)
    drawCircle(if (bars >= 0) Lit else Dim, radius = stroke * 0.8f, center = dotCenter)
    for (ring in 1..3) {
        val radius = size.width * (0.17f + 0.15f * ring)
        drawArc(
            color = if (ring <= bars) Lit else Dim,
            startAngle = 225f,
            sweepAngle = 90f,
            useCenter = false,
            topLeft = Offset(dotCenter.x - radius, dotCenter.y - radius),
            size = Size(radius * 2, radius * 2),
            style = Stroke(width = stroke, cap = StrokeCap.Round),
        )
    }
    if (crossed) {
        drawLine(
            Lit,
            start = Offset(size.width * 0.15f, size.height * 0.15f),
            end = Offset(size.width * 0.85f, size.height * 0.85f),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
    }
}

// A network jack seen head-on
private fun DrawScope.ethernet() {
    val stroke = size.width * 0.09f
    val body = Size(size.width * 0.70f, size.height * 0.55f)
    val topLeft = Offset((size.width - body.width) / 2, size.height * 0.28f)
    drawRoundRect(
        color = Lit,
        topLeft = topLeft,
        size = body,
        cornerRadius = CornerRadius(stroke),
        style = Stroke(width = stroke),
    )
    val clip = Size(body.width * 0.36f, body.height * 0.30f)
    drawRect(
        color = Lit,
        topLeft = Offset(center.x - clip.width / 2, topLeft.y + body.height - clip.height - stroke / 2),
        size = clip,
    )
    for (pin in 0..3) {
        val x = topLeft.x + body.width * (0.26f + pin * 0.16f)
        drawLine(Lit, Offset(x, topLeft.y + stroke * 1.6f), Offset(x, topLeft.y + body.height * 0.42f), strokeWidth = stroke * 0.6f)
    }
}
