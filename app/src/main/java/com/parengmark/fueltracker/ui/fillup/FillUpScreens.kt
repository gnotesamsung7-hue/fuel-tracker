package com.parengmark.fueltracker.ui.fillup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.parengmark.fueltracker.ui.common.Formatting

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FillUpListScreen(
    viewModel: FillUpViewModel,
    vehicleId: Long,
    onAddFillUp: () -> Unit,
    onBack: () -> Unit
) {
    LaunchedEffect(vehicleId) { viewModel.load(vehicleId) }

    val vehicle by viewModel.vehicle.collectAsState()
    val fillUps by viewModel.fillUps.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Fuel log") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        val v = vehicle
        if (v == null || fillUps.isEmpty()) {
            Column(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
                Text("No fill-ups logged yet.")
                Spacer(Modifier.height(16.dp))
                Button(onClick = onAddFillUp) { Text("Add fill-up") }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(fillUps, key = { it.id }) { fillUp ->
                    Card(Modifier.fillMaxWidth()) {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    "${Formatting.date(fillUp.dateEpochMillis)} · " +
                                        "${fillUp.odometer} ${v.distanceUnitLabel}"
                                )
                                Text(
                                    "${fillUp.volume} ${v.volumeUnitLabel} · " +
                                        Formatting.money(fillUp.totalCost, v.currencyCode) +
                                        if (!fillUp.fullTank) " · partial" else "",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                if (!fillUp.stationName.isNullOrBlank()) {
                                    Text(fillUp.stationName, style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                            IconButton(onClick = { viewModel.deleteFillUp(fillUp) }) {
                                Icon(Icons.Filled.Delete, contentDescription = "Delete")
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddFillUpScreen(
    viewModel: FillUpViewModel,
    vehicleId: Long,
    onSaved: () -> Unit,
    onBack: () -> Unit,
    initialVolumeText: String = "",
    initialCostText: String = "",
    initialStationText: String = ""
) {
    LaunchedEffect(vehicleId) { viewModel.load(vehicleId) }

    val vehicle by viewModel.vehicle.collectAsState()
    val lastFillUp by viewModel.lastFillUp.collectAsState()
    val saveError by viewModel.saveError.collectAsState()

    val fromScan = initialVolumeText.isNotBlank() || initialCostText.isNotBlank()

    var dateMillis by remember { mutableStateOf(System.currentTimeMillis()) }
    var odometerText by remember { mutableStateOf("") }
    var volumeText by remember { mutableStateOf(initialVolumeText) }
    var costText by remember { mutableStateOf(initialCostText) }
    var fullTank by remember { mutableStateOf(true) }
    var station by remember { mutableStateOf(initialStationText) }
    var notes by remember { mutableStateOf("") }
    var showDatePicker by remember { mutableStateOf(false) }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = dateMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                Button(onClick = {
                    datePickerState.selectedDateMillis?.let { dateMillis = it }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                Button(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add fill-up") },
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
                .padding(16.dp)
        ) {
            val v = vehicle

            if (fromScan) {
                Text(
                    "Values from your scanned receipt -- please double-check before saving.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            OutlinedButton(onClick = { showDatePicker = true }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Filled.CalendarToday, contentDescription = null)
                Spacer(Modifier.height(0.dp))
                Text("  " + Formatting.date(dateMillis))
            }

            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = odometerText,
                onValueChange = { odometerText = it },
                label = { Text("Odometer" + if (v != null) " (${v.distanceUnitLabel})" else "") },
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth()
            )
            if (lastFillUp != null) {
                Text(
                    "Last logged: ${lastFillUp!!.odometer}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = volumeText,
                onValueChange = { volumeText = it },
                label = { Text("Volume" + if (v != null) " (${v.volumeUnitLabel})" else "") },
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = costText,
                onValueChange = { costText = it },
                label = { Text("Total cost" + if (v != null) " (${v.currencyCode})" else "") },
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(checked = fullTank, onCheckedChange = { fullTank = it })
                Text("Full tank (needed for accurate economy calculation)")
            }

            OutlinedTextField(
                value = station,
                onValueChange = { station = it },
                label = { Text("Station (optional)") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            )

            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Notes (optional)") },
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
                    viewModel.addFillUp(
                        vehicleId = vehicleId,
                        dateEpochMillis = dateMillis,
                        odometer = odometerText.toDoubleOrNull() ?: -1.0,
                        volume = volumeText.toDoubleOrNull() ?: -1.0,
                        totalCost = costText.toDoubleOrNull() ?: -1.0,
                        fullTank = fullTank,
                        stationName = station,
                        notes = notes,
                        onSaved = onSaved
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
            ) {
                Text("Save fill-up")
            }
        }
    }
}
