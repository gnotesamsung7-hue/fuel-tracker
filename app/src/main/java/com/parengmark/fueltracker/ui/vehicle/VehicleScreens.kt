package com.parengmark.fueltracker.ui.vehicle

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.parengmark.fueltracker.data.MetricEconomyDisplay
import com.parengmark.fueltracker.data.UnitSystem
import com.parengmark.fueltracker.data.Vehicle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VehicleListScreen(
    viewModel: VehicleViewModel,
    onVehicleClick: (Long) -> Unit,
    onAddVehicle: () -> Unit
) {
    val vehicles by viewModel.vehicles.collectAsState()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Your Vehicles") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddVehicle) {
                Icon(Icons.Filled.Add, contentDescription = "Add vehicle")
            }
        }
    ) { padding ->
        if (vehicles.isEmpty()) {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text("Add your first vehicle to start logging fill-ups.")
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(vehicles, key = { it.id }) { vehicle ->
                    VehicleCard(vehicle = vehicle, onClick = { onVehicleClick(vehicle.id) })
                }
            }
        }
    }
}

@Composable
private fun VehicleCard(vehicle: Vehicle, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(vehicle.name, style = MaterialTheme.typography.titleLarge)
            Text(
                "${vehicle.distanceUnitLabel} / ${vehicle.volumeUnitLabel} · ${vehicle.currencyCode}",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddVehicleScreen(
    viewModel: VehicleViewModel,
    onDone: () -> Unit,
    onBack: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var unitSystem by remember { mutableStateOf(UnitSystem.METRIC) }
    var economyDisplay by remember { mutableStateOf(MetricEconomyDisplay.KM_PER_L) }
    var currency by remember { mutableStateOf("PHP") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add Vehicle") },
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
            Icon(
                Icons.Filled.DirectionsCar,
                contentDescription = null,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Vehicle name (e.g. \"Vios\", \"Wife's car\")") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(16.dp))
            Text("Unit system")
            UnitSystem.entries.forEach { option ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectable(
                            selected = unitSystem == option,
                            onClick = { unitSystem = option }
                        )
                ) {
                    RadioButton(selected = unitSystem == option, onClick = { unitSystem = option })
                    Text(if (option == UnitSystem.METRIC) "Metric (km, liters)" else "Imperial (miles, gallons)")
                }
            }

            if (unitSystem == UnitSystem.METRIC) {
                Spacer(Modifier.height(8.dp))
                Text("Show economy as")
                MetricEconomyDisplay.entries.forEach { option ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = economyDisplay == option,
                                onClick = { economyDisplay = option }
                            )
                    ) {
                        RadioButton(selected = economyDisplay == option, onClick = { economyDisplay = option })
                        Text(if (option == MetricEconomyDisplay.KM_PER_L) "km/L" else "L/100km")
                    }
                }
            }

            OutlinedTextField(
                value = currency,
                onValueChange = { currency = it.uppercase().take(3) },
                label = { Text("Currency code (e.g. PHP, USD)") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            )

            Button(
                onClick = {
                    viewModel.addVehicle(name, unitSystem, economyDisplay, currency)
                    onDone()
                },
                enabled = name.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
            ) {
                Text("Save vehicle")
            }
        }
    }
}
