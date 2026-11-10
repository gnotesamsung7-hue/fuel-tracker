package com.parengmark.fueltracker.ui.trip

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.parengmark.fueltracker.data.FuelGauge
import com.parengmark.fueltracker.data.FuelStatus
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckInScreen(
    viewModel: TripViewModel,
    vehicleId: Long,
    onDone: () -> Unit,
    onBack: () -> Unit,
    onNearbyStations: () -> Unit
) {
    LaunchedEffect(vehicleId) {
        viewModel.load(vehicleId)
        viewModel.clearSaveError()
    }

    val vehicle by viewModel.vehicle.collectAsState()
    val logs by viewModel.logs.collectAsState()
    val fillUps by viewModel.fillUps.collectAsState()
    val estimate by viewModel.estimate.collectAsState()
    val saveError by viewModel.saveError.collectAsState()
    val result by viewModel.checkInResult.collectAsState()

    var odometerText by remember { mutableStateOf("") }
    var gaugeSet by remember { mutableStateOf(false) }
    var gaugeValue by remember { mutableFloatStateOf(4f) }
    var purpose by remember { mutableStateOf<String?>(null) }
    var note by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) { runCatching { focusRequester.requestFocus() } }

    // Start the slider where the app thinks the gauge is, so it's a small nudge.
    LaunchedEffect(estimate?.fractionLeft) {
        val e = estimate
        if (!gaugeSet && e != null) gaugeValue = FuelGauge.nearestEighths(e.fractionLeft).toFloat()
    }

    val v = vehicle
    val unit = v?.distanceUnitLabel ?: "km"
    val last = (logs.map { it.odometer } + fillUps.map { it.odometer }).maxOrNull()

    result?.let { r ->
        val e = r.estimate
        AlertDialog(
            onDismissRequest = { viewModel.dismissCheckInResult(); onDone() },
            title = { Text(e?.status?.headline() ?: "Checked in") },
            text = {
                Column {
                    if (r.distanceSinceLast != null && r.distanceSinceLast > 0) {
                        Text("${TripViewModel.plain(r.distanceSinceLast)} $unit since your last reading.")
                        Spacer(Modifier.height(8.dp))
                    }
                    if (e != null && v != null) {
                        FuelLevelBar(e.fractionLeft, e.status)
                        Spacer(Modifier.height(8.dp))
                        Text(e.describe(v))
                        if (e.status != FuelStatus.OK) {
                            Spacer(Modifier.height(8.dp))
                            Text(
                                if (e.status == FuelStatus.LOW) "You're below 1/8 of a tank. Fill up before your next long drive."
                                else "You're at about a quarter tank. Plan a top-up in the next day or two."
                            )
                        }
                    } else if (v?.tankCapacity == null) {
                        Text("Set your tank size on the dashboard to get top-up reminders here.")
                    } else {
                        Text("Set the fuel gauge on a check-in, or log a full-tank fill-up, to start the top-up estimate.")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { viewModel.dismissCheckInResult(); onDone() }) { Text("Drive safe") }
            },
            dismissButton = if (e != null && e.status != FuelStatus.OK) {
                {
                    TextButton(onClick = { viewModel.dismissCheckInResult(); onNearbyStations() }) {
                        Text("Nearby stations")
                    }
                }
            } else null
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Engine check-in") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Text(
                "Engine on? Log the odometer before you drive off.",
                style = MaterialTheme.typography.bodyLarge
            )
            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = odometerText,
                onValueChange = { odometerText = it },
                label = { Text("Odometer ($unit)") },
                textStyle = MaterialTheme.typography.headlineSmall,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester)
            )
            if (last != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Last reading: ${TripViewModel.plain(last)} $unit",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = { odometerText = TripViewModel.plain(last) }) { Text("Same") }
                }
            }

            Spacer(Modifier.height(16.dp))
            Text(
                if (gaugeSet) "Fuel gauge: ${FuelGauge.label(gaugeValue.roundToInt())}"
                else "Fuel gauge (optional): slide to match your dashboard",
                style = MaterialTheme.typography.titleSmall
            )
            Slider(
                value = gaugeValue,
                onValueChange = { gaugeValue = it; gaugeSet = true },
                valueRange = 0f..8f,
                steps = 7
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("E", style = MaterialTheme.typography.labelMedium)
                Text("1/2", style = MaterialTheme.typography.labelMedium)
                Text("F", style = MaterialTheme.typography.labelMedium)
            }
            val e = estimate
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    if (e != null) "App estimate: about ${FuelGauge.label(FuelGauge.nearestEighths(e.fractionLeft))}"
                    else "Setting the gauge helps the app learn your fuel level.",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.weight(1f)
                )
                if (gaugeSet) {
                    TextButton(onClick = { gaugeSet = false }) { Text("Skip gauge") }
                }
            }

            Spacer(Modifier.height(12.dp))
            Text("Trip (optional)", style = MaterialTheme.typography.titleSmall)
            Row(
                Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FuelGauge.purposes.forEach { p ->
                    FilterChip(
                        selected = purpose == p,
                        onClick = { purpose = if (purpose == p) null else p },
                        label = { Text(p) }
                    )
                }
            }

            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("Note (optional)") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            )

            if (saveError != null) {
                Text(
                    saveError ?: "",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            Button(
                onClick = {
                    viewModel.checkIn(
                        vehicleId = vehicleId,
                        odometerText = odometerText,
                        fuelEighths = if (gaugeSet) gaugeValue.roundToInt() else null,
                        purpose = purpose,
                        note = note
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
                    .height(56.dp)
            ) {
                Text("Check in", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}
