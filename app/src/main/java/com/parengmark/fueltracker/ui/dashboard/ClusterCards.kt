package com.parengmark.fueltracker.ui.dashboard

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.parengmark.fueltracker.data.ConsumptionCalculator
import com.parengmark.fueltracker.data.FillUp
import com.parengmark.fueltracker.data.FuelEstimate
import com.parengmark.fueltracker.data.FuelEstimator
import com.parengmark.fueltracker.data.FuelGauge
import com.parengmark.fueltracker.data.FuelStatus
import com.parengmark.fueltracker.data.PriceWatch
import com.parengmark.fueltracker.data.Vehicle
import com.parengmark.fueltracker.ui.common.DialGauge
import com.parengmark.fueltracker.ui.common.Formatting
import com.parengmark.fueltracker.ui.trip.color
import kotlin.math.abs
import kotlin.math.roundToInt

/** Two round gauges like a real instrument cluster: estimated fuel, and the
 * economy of your last full tank against your best. */
@Composable
fun InstrumentClusterCard(
    vehicle: Vehicle,
    fillUps: List<FillUp>,
    estimate: FuelEstimate?
) {
    val segments = ConsumptionCalculator.computeSegments(fillUps)
    val avg = ConsumptionCalculator.averageDistancePerVolume(fillUps)
    val last = segments.lastOrNull()?.distancePerVolume
    val best = segments.maxOfOrNull { it.distancePerVolume }
    val scaleMax = listOfNotNull(best, avg).maxOrNull()?.let { it * 1.25 }
    val costPerDistance = ConsumptionCalculator.costPerDistance(fillUps)

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            Row(Modifier.fillMaxWidth()) {
                DialGauge(
                    fraction = estimate?.fractionLeft?.toFloat(),
                    valueText = estimate?.let { FuelGauge.label(FuelGauge.nearestEighths(it.fractionLeft)) } ?: "--",
                    caption = estimate?.rangeLeft?.let { "~${it.roundToInt()} ${vehicle.distanceUnitLabel} left" } ?: "Fuel",
                    startLabel = "E",
                    endLabel = "F",
                    zoneColor = { f ->
                        when {
                            f <= FuelEstimator.LOW_FRACTION -> FuelStatus.LOW.color()
                            f <= FuelEstimator.TOP_UP_SOON_FRACTION -> FuelStatus.TOP_UP_SOON.color()
                            else -> FuelStatus.OK.color()
                        }
                    },
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(8.dp))
                DialGauge(
                    fraction = if (last != null && scaleMax != null) (last / scaleMax).toFloat() else null,
                    valueText = if (last != null) ConsumptionCalculator.formatEconomy(last, vehicle) else "--",
                    caption = if (last != null) "Last tank" else "Economy",
                    startLabel = "Thirsty",
                    endLabel = "Thrifty",
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "Average ${ConsumptionCalculator.formatEconomy(avg, vehicle)}" +
                    (costPerDistance?.let { " · ${Formatting.money(it, vehicle.currencyCode)} per ${vehicle.distanceUnitLabel} (fuel)" } ?: ""),
                style = MaterialTheme.typography.bodyMedium
            )
            if (best != null && last != null && segments.size >= 2) {
                Text(
                    "Best tank: ${ConsumptionCalculator.formatEconomy(best, vehicle)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/** Latest pump price, the change since last time, and a small price history line. */
@Composable
fun PriceWatchCard(watch: PriceWatch, vehicle: Vehicle) {
    val up = MaterialTheme.colorScheme.error
    val down = FuelStatus.OK.color()
    val line = MaterialTheme.colorScheme.secondary
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(
                "Price watch" + (watch.grade?.let { " · $it" } ?: ""),
                style = MaterialTheme.typography.bodyMedium
            )
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    Formatting.money(watch.latestPrice, vehicle.currencyCode) + "/${vehicle.volumeUnitLabel}",
                    style = MaterialTheme.typography.headlineSmall
                )
                Spacer(Modifier.width(8.dp))
                watch.change?.let { c ->
                    if (abs(c) >= 0.005) {
                        Text(
                            (if (c > 0) "▲ " else "▼ ") + Formatting.money(abs(c), vehicle.currencyCode),
                            color = if (c > 0) up else down,
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    } else {
                        Text("no change", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(bottom = 4.dp))
                    }
                }
            }
            if (watch.history.size >= 2) {
                Spacer(Modifier.height(8.dp))
                Canvas(Modifier.fillMaxWidth().height(40.dp)) {
                    val lo = watch.history.min()
                    val hi = watch.history.max()
                    val span = (hi - lo).takeIf { it > 0 } ?: 1.0
                    val stepX = size.width / (watch.history.size - 1)
                    val path = Path()
                    watch.history.forEachIndexed { i, p ->
                        val x = i * stepX
                        val y = (size.height - ((p - lo) / span * size.height)).toFloat()
                        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                    }
                    drawPath(path, line, style = Stroke(width = 4f, cap = StrokeCap.Round))
                    val lastY = (size.height - ((watch.history.last() - lo) / span * size.height)).toFloat()
                    drawCircle(line, radius = 6f, center = Offset(size.width, lastY))
                }
                Text(
                    "Low ${Formatting.money(watch.lowest, vehicle.currencyCode)} · High ${Formatting.money(watch.highest, vehicle.currencyCode)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
