package com.parengmark.fueltracker.ui.theme

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * The whole-app background for the car-dashboard theme: a dark panel with a
 * faint carbon-fibre weave, two ghosted instrument dials at the bottom and a
 * soft amber backlight glow at the top. Purely decorative and static.
 */
@Composable
fun DashboardBackdrop(modifier: Modifier = Modifier) {
    Canvas(modifier.fillMaxSize()) {
        // Panel
        drawRect(
            Brush.verticalGradient(
                listOf(Color(0xFF0D1117), Color(0xFF11161E), Color(0xFF080A0E))
            )
        )

        // Carbon weave: alternating light/dark diagonal tiles.
        val tile = 6.dp.toPx()
        val light = Color.White.copy(alpha = 0.022f)
        var y = 0f
        var row = 0
        while (y < size.height) {
            var x = if (row % 2 == 0) 0f else tile
            while (x < size.width) {
                drawRect(light, topLeft = Offset(x, y), size = Size(tile, tile / 2))
                drawRect(light, topLeft = Offset(x + tile, y + tile / 2), size = Size(tile / 2, tile / 2))
                x += tile * 2
            }
            y += tile
            row++
        }

        // Backlight glow at the top.
        drawRect(
            Brush.radialGradient(
                colors = listOf(DashAmber.copy(alpha = 0.10f), Color.Transparent),
                center = Offset(size.width / 2, 0f),
                radius = size.width * 0.9f
            )
        )

        // Two ghosted dials near the bottom, like a cluster seen through glass.
        val r = min(size.width * 0.26f, size.height * 0.16f)
        val cy = size.height - r * 0.9f
        listOf(size.width * 0.27f, size.width * 0.73f).forEachIndexed { i, cx ->
            val ring = if (i == 0) DashCyan else DashAmber
            drawCircle(ring.copy(alpha = 0.06f), radius = r, center = Offset(cx, cy), style = Stroke(width = 3f))
            // Tick marks across a 240-degree sweep.
            for (t in 0..24) {
                val angle = Math.toRadians((150.0 + t * 10.0))
                val inner = if (t % 4 == 0) r * 0.80f else r * 0.88f
                val start = Offset(cx + (cos(angle) * inner).toFloat(), cy + (sin(angle) * inner).toFloat())
                val end = Offset(cx + (cos(angle) * r * 0.95f).toFloat(), cy + (sin(angle) * r * 0.95f).toFloat())
                val tickColor = if (t >= 21) DashRed.copy(alpha = 0.12f) else ring.copy(alpha = 0.08f)
                drawLine(tickColor, start, end, strokeWidth = if (t % 4 == 0) 4f else 2f, cap = StrokeCap.Round)
            }
            rotate(degrees = if (i == 0) 200f else 250f, pivot = Offset(cx, cy)) {
                drawLine(
                    DashRed.copy(alpha = 0.10f),
                    Offset(cx, cy),
                    Offset(cx + r * 0.7f, cy),
                    strokeWidth = 5f,
                    cap = StrokeCap.Round
                )
            }
        }

        // Vignette
        drawRect(
            Brush.radialGradient(
                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.45f)),
                center = Offset(size.width / 2, size.height / 2),
                radius = maxOf(size.width, size.height) * 0.8f
            )
        )
    }
}
