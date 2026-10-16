package com.omenight.bright.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import com.omenight.bright.ui.theme.BrightColors

/** Bright's few icons, drawn in code so the APK ships no icon font or vector set. */
enum class Glyph { Gear, Grid, House, Pin }

@Composable
fun GlyphIcon(glyph: Glyph, size: Dp, modifier: Modifier = Modifier, color: Color = BrightColors.Text) {
    Canvas(modifier.size(size)) {
        when (glyph) {
            Glyph.Gear -> drawPath(gearPath(this.size.minDimension), color)
            Glyph.Grid -> grid(color)
            Glyph.House -> house(color)
            Glyph.Pin -> drawCircle(color, radius = this.size.minDimension * 0.18f)
        }
    }
}

// Eight rounded teeth around a body, with the axle hole cut out
private fun gearPath(size: Float): Path {
    val c = size / 2
    val body = Path().apply { addOval(Rect(Offset(c, c), size * 0.30f)) }
    val tooth = RoundRect(
        rect = Rect(c - size * 0.08f, c - size * 0.46f, c + size * 0.08f, c - size * 0.22f),
        cornerRadius = CornerRadius(size * 0.035f),
    )
    val teeth = Path()
    repeat(8) { i ->
        val path = Path().apply { addRoundRect(tooth) }
        path.transform(Matrix().apply {
            translate(c, c)
            rotateZ(45f * i)
            translate(-c, -c)
        })
        teeth.addPath(path)
    }
    val hole = Path().apply { addOval(Rect(Offset(c, c), size * 0.12f)) }
    return Path.combine(PathOperation.Difference, Path.combine(PathOperation.Union, body, teeth), hole)
}

private fun DrawScope.grid(color: Color) {
    val inset = size.width * 0.15f
    val step = (size.width - inset * 2) / 2
    val radius = size.width / 12
    for (row in 0..2) for (column in 0..2) {
        drawCircle(color, radius, Offset(inset + column * step, inset + row * step))
    }
}

private fun DrawScope.house(color: Color) {
    val w = size.width
    val path = Path().apply {
        moveTo(w * 0.15f, w * 0.48f)
        lineTo(w * 0.50f, w * 0.18f)
        lineTo(w * 0.85f, w * 0.48f)
        moveTo(w * 0.25f, w * 0.40f)
        lineTo(w * 0.25f, w * 0.82f)
        lineTo(w * 0.75f, w * 0.82f)
        lineTo(w * 0.75f, w * 0.40f)
    }
    drawPath(path, color, style = Stroke(width = w * 0.08f, cap = StrokeCap.Round, join = StrokeJoin.Round))
}
