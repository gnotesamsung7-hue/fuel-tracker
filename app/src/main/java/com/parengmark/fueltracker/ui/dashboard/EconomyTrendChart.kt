package com.parengmark.fueltracker.ui.dashboard

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp
import com.parengmark.fueltracker.data.ConsumptionCalculator
import com.parengmark.fueltracker.data.EconomySegment
import com.parengmark.fueltracker.data.Vehicle

/** A deliberately simple Canvas line chart -- no external charting library,
 * see the spec's note on keeping this build's dependency surface small.
 * Plots one point per full-tank-to-full-tank [EconomySegment], oldest to
 * newest (segments are already chronological by odometer). */
@Composable
fun EconomyTrendChart(
    segments: List<EconomySegment>,
    vehicle: Vehicle,
    modifier: Modifier = Modifier
) {
    if (segments.size < 2) {
        Text(
            "Log a couple more full-tank fill-ups to see your economy trend over time.",
            style = MaterialTheme.typography.bodyMedium,
            modifier = modifier
        )
        return
    }

    val values = segments.map { ConsumptionCalculator.displayValue(it.distancePerVolume, vehicle) }
    val minValue = values.min()
    val maxValue = values.max()
    val range = (maxValue - minValue).let { if (it <= 0.0) 1.0 else it }
    val unit = ConsumptionCalculator.unitSuffix(vehicle)
    val lineColor = MaterialTheme.colorScheme.primary

    Column(modifier) {
        Text("Economy trend (last ${values.size} spans)", style = MaterialTheme.typography.bodyMedium)
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .padding(top = 4.dp)
        ) {
            val stepX = if (values.size > 1) size.width / (values.size - 1) else size.width
            val points = values.mapIndexed { index, value ->
                val normalized = ((value - minValue) / range).toFloat()
                val x = index * stepX
                val y = size.height - (normalized * size.height)
                Offset(x, y)
            }
            for (i in 0 until points.size - 1) {
                drawLine(
                    color = lineColor,
                    start = points[i],
                    end = points[i + 1],
                    strokeWidth = 4f,
                    cap = StrokeCap.Round
                )
            }
            points.forEach { p -> drawCircle(color = lineColor, radius = 5f, center = p) }
        }
        Text(
            "Low: %.1f %s    High: %.1f %s".format(minValue, unit, maxValue, unit),
            style = MaterialTheme.typography.bodyMedium
        )
    }
}
