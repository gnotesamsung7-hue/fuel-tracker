package com.parengmark.fueltracker.ui.trip

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Key
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.parengmark.fueltracker.data.FuelEstimate
import com.parengmark.fueltracker.data.FuelStatus
import com.parengmark.fueltracker.data.Vehicle
import kotlin.math.roundToInt

private val FuelGreen = Color(0xFF2E7D32)
private val FuelAmber = Color(0xFFF9A825)
private val FuelRed = Color(0xFFC62828)

fun FuelStatus.color(): Color = when (this) {
    FuelStatus.OK -> FuelGreen
    FuelStatus.TOP_UP_SOON -> FuelAmber
    FuelStatus.LOW -> FuelRed
}

fun FuelStatus.headline(): String = when (this) {
    FuelStatus.OK -> "You're good"
    FuelStatus.TOP_UP_SOON -> "Top up soon"
    FuelStatus.LOW -> "Fill up now"
}

/** "About 18 L in the tank, ~180 km left" */
fun FuelEstimate.describe(vehicle: Vehicle): String {
    val volume = "${volumeLeft.roundToInt()} ${vehicle.volumeUnitLabel}"
    val range = rangeLeft?.let { ", ~${it.roundToInt()} ${vehicle.distanceUnitLabel} left" } ?: ""
    return "About $volume in the tank (${(fractionLeft * 100).roundToInt()}%)$range"
}

/** A horizontal fuel gauge: E on the left, F on the right, with the
 * top-up-soon line marked at a quarter tank. */
@Composable
fun FuelLevelBar(fraction: Double, status: FuelStatus, modifier: Modifier = Modifier) {
    Column(modifier) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(14.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(7.dp))
        ) {
            Box(
                Modifier
                    .fillMaxWidth(fraction.toFloat().coerceIn(0.03f, 1f))
                    .fillMaxHeight()
                    .background(status.color(), RoundedCornerShape(7.dp))
            )
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("E", style = MaterialTheme.typography.labelSmall)
            Text("1/4", style = MaterialTheme.typography.labelSmall)
            Text("1/2", style = MaterialTheme.typography.labelSmall)
            Text("3/4", style = MaterialTheme.typography.labelSmall)
            Text("F", style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
fun TankSizeDialog(
    vehicle: Vehicle,
    onSave: (String) -> Boolean,
    onDismiss: () -> Unit
) {
    var text by remember {
        mutableStateOf(vehicle.tankCapacity?.let { TripViewModel.plain(it) } ?: "")
    }
    var error by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Fuel tank size") },
        text = {
            Column {
                Text(
                    "Check your owner's manual or search your model's tank size. " +
                        "Most cars are ${if (vehicle.volumeUnitLabel == "L") "35-70 L" else "9-18 gal"}."
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it; error = false },
                    label = { Text("Tank size (${vehicle.volumeUnitLabel})") },
                    isError = error,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { if (onSave(text)) onDismiss() else error = true }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

/** Dashboard card: fuel estimate plus the big "engine on" check-in button. */
@Composable
fun EngineCheckInCard(
    vehicle: Vehicle,
    estimate: FuelEstimate?,
    hasEconomy: Boolean,
    onCheckIn: () -> Unit,
    onOpenJournal: () -> Unit,
    onSetTankSize: (String) -> Boolean
) {
    var showTankDialog by remember { mutableStateOf(false) }
    if (showTankDialog) {
        TankSizeDialog(vehicle = vehicle, onSave = onSetTankSize, onDismiss = { showTankDialog = false })
    }

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Fuel estimate", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                if (estimate != null) {
                    Text(
                        estimate.status.headline(),
                        color = estimate.status.color(),
                        style = MaterialTheme.typography.titleSmall
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            when {
                vehicle.tankCapacity == null -> {
                    Text("Set your tank size to get top-up reminders.")
                    TextButton(onClick = { showTankDialog = true }) { Text("Set tank size") }
                }
                estimate == null -> {
                    Text(
                        if (hasEconomy) {
                            "Log a full-tank fill-up, or set the fuel gauge when you check in, to start the estimate."
                        } else {
                            "Log a full-tank fill-up, or set the fuel gauge when you check in. The " +
                                "estimate keeps working between fill-ups once two full-tank fill-ups give your economy."
                        },
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                else -> {
                    FuelLevelBar(estimate.fractionLeft, estimate.status)
                    Spacer(Modifier.height(6.dp))
                    Text(estimate.describe(vehicle), style = MaterialTheme.typography.bodyMedium)
                }
            }
            if (vehicle.tankCapacity != null) {
                TextButton(onClick = { showTankDialog = true }) {
                    Text("Tank: ${TripViewModel.plain(vehicle.tankCapacity)} ${vehicle.volumeUnitLabel} (edit)")
                }
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Button(onClick = onCheckIn, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Filled.Key, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Engine on: check in")
                }
                Spacer(Modifier.width(8.dp))
                TextButton(onClick = onOpenJournal) { Text("Journal") }
            }
        }
    }
}
