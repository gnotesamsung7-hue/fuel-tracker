package com.parengmark.fueltracker.ui.maintenance

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
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
import com.parengmark.fueltracker.data.DueStatus
import com.parengmark.fueltracker.data.MaintenanceReminder
import com.parengmark.fueltracker.data.MaintenanceStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MaintenanceListScreen(
    viewModel: MaintenanceViewModel,
    vehicleId: Long,
    onAddReminder: () -> Unit,
    onBack: () -> Unit
) {
    LaunchedEffect(vehicleId) { viewModel.load(vehicleId) }

    val vehicle by viewModel.vehicle.collectAsState()
    val reminders by viewModel.reminders.collectAsState()
    val currentOdometer by viewModel.currentOdometer.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Maintenance") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddReminder) {
                Icon(Icons.Filled.Add, contentDescription = "Add reminder")
            }
        }
    ) { padding ->
        val v = vehicle
        if (reminders.isEmpty()) {
            Column(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
                Text("No maintenance reminders yet -- oil changes, tire rotations, registration renewal, etc.")
                Spacer(Modifier.height(16.dp))
                Button(onClick = onAddReminder) { Text("Add reminder") }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(reminders, key = { it.id }) { reminder ->
                    val status = MaintenanceStatus.compute(reminder, currentOdometer, System.currentTimeMillis())
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp)) {
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(reminder.type, style = MaterialTheme.typography.titleLarge)
                                IconButton(onClick = { viewModel.deleteReminder(reminder) }) {
                                    Icon(Icons.Filled.Delete, contentDescription = "Delete")
                                }
                            }
                            Text(statusLabel(status), color = statusColor(status))
                            if (reminder.nextDueOdometer != null && v != null) {
                                Text("Due at ${reminder.nextDueOdometer} ${v.distanceUnitLabel}")
                            }
                            if (reminder.nextDueDateMillis != null) {
                                Text("Due by " + com.parengmark.fueltracker.ui.common.Formatting.date(reminder.nextDueDateMillis!!))
                            }
                            TextButton(onClick = { viewModel.markDone(reminder) }) {
                                Text("Mark done today")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun statusLabel(status: DueStatus): String = when (status) {
    DueStatus.OVERDUE -> "Overdue"
    DueStatus.DUE_SOON -> "Due soon"
    DueStatus.OK -> "OK"
    DueStatus.UNKNOWN -> "No odometer data yet"
}

@Composable
private fun statusColor(status: DueStatus) = when (status) {
    DueStatus.OVERDUE -> MaterialTheme.colorScheme.error
    DueStatus.DUE_SOON -> MaterialTheme.colorScheme.tertiary
    DueStatus.OK -> MaterialTheme.colorScheme.primary
    DueStatus.UNKNOWN -> MaterialTheme.colorScheme.onSurfaceVariant
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddMaintenanceReminderScreen(
    viewModel: MaintenanceViewModel,
    vehicleId: Long,
    onDone: () -> Unit,
    onBack: () -> Unit
) {
    LaunchedEffect(vehicleId) { viewModel.load(vehicleId) }

    var type by remember { mutableStateOf("") }
    var intervalDistanceText by remember { mutableStateOf("") }
    var intervalDaysText by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add reminder") },
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
            OutlinedTextField(
                value = type,
                onValueChange = { type = it },
                label = { Text("What (e.g. Oil change, Tire rotation)") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = intervalDistanceText,
                onValueChange = { intervalDistanceText = it },
                label = { Text("Every N distance units (optional)") },
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = intervalDaysText,
                onValueChange = { intervalDaysText = it },
                label = { Text("Every N days (optional)") },
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )

            Text(
                "Fill in at least one of the two above. The reminder starts counting from today / your latest logged odometer reading.",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 8.dp)
            )

            Button(
                onClick = {
                    viewModel.addReminder(
                        vehicleId = vehicleId,
                        type = type,
                        intervalDistance = intervalDistanceText.toDoubleOrNull(),
                        intervalDays = intervalDaysText.toIntOrNull()
                    )
                    onDone()
                },
                enabled = type.isNotBlank() && (intervalDistanceText.toDoubleOrNull() != null || intervalDaysText.toIntOrNull() != null),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
            ) {
                Text("Save reminder")
            }
        }
    }
}
