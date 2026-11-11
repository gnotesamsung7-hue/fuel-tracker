package com.parengmark.fueltracker.ui.common

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * A car-style round gauge: a 240-degree arc with ticks, a coloured fill up to
 * [fraction] and a needle that sweeps into place. Null [fraction] draws the
 * empty dial with the needle resting at the start ("no reading yet").
 *
 * [zoneColor] picks the arc colour for a given fraction, e.g. red near E.
 */
@Composable
fun DialGauge(
    fraction: Float?,
    valueText: String,
    caption: String,
    startLabel: String,
    endLabel: String,
    modifier: Modifier = Modifier,
    zoneColor: ((Float) -> Color)? = null
) {
    val target = (fraction ?: 0f).coerceIn(0f, 1f)
    val animated by animateFloatAsState(targetValue = target, animationSpec = tween(900), label = "needle")
    val track = MaterialTheme.colorScheme.surfaceVariant
    val tick = MaterialTheme.colorScheme.onSurfaceVariant
    val needle = MaterialTheme.colorScheme.tertiary
    val defaultArc = MaterialTheme.colorScheme.primary
    val arcColor = zoneColor?.invoke(target) ?: defaultArc

    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.fillMaxWidth().aspectRatio(1.25f), contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxWidth().aspectRatio(1.25f)) {
                val stroke = size.width * 0.07f
                val radius = min(size.width, size.height * 1.25f) / 2f - stroke
                val center = Offset(size.width / 2f, size.height * 0.56f)
                val topLeft = Offset(center.x - radius, center.y - radius)
                val arcSize = Size(radius * 2, radius * 2)
                val start = 150f
                val sweep = 240f

                drawArc(track, start, sweep, false, topLeft, arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
                if (fraction != null) {
                    drawArc(
                        arcColor, start, sweep * animated, false, topLeft, arcSize,
                        style = Stroke(stroke, cap = StrokeCap.Round)
                    )
                }

                for (i in 0..8) {
                    val a = Math.toRadians((start + sweep * i / 8f).toDouble())
                    val outer = radius - stroke * 0.9f
                    val inner = outer - if (i % 2 == 0) stroke * 0.9f else stroke * 0.5f
                    drawLine(
                        tick.copy(alpha = 0.7f),
                        Offset(center.x + (cos(a) * inner).toFloat(), center.y + (sin(a) * inner).toFloat()),
                        Offset(center.x + (cos(a) * outer).toFloat(), center.y + (sin(a) * outer).toFloat()),
                        strokeWidth = if (i % 2 == 0) 3f else 2f
                    )
                }

                val na = Math.toRadians((start + sweep * (if (fraction != null) animated else 0f)).toDouble())
                val tip = radius - stroke * 2.2f
                drawLine(
                    needle,
                    center,
                    Offset(center.x + (cos(na) * tip).toFloat(), center.y + (sin(na) * tip).toFloat()),
                    strokeWidth = stroke * 0.35f,
                    cap = StrokeCap.Round
                )
                drawCircle(needle, radius = stroke * 0.55f, center = center)
            }
            Column(
                Modifier.align(Alignment.BottomCenter).padding(bottom = 2.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    valueText,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            }
            Text(
                startLabel,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.align(Alignment.BottomStart).padding(start = 6.dp, bottom = 22.dp)
            )
            Text(
                endLabel,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.align(Alignment.BottomEnd).padding(end = 6.dp, bottom = 22.dp)
            )
        }
        Text(caption, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
