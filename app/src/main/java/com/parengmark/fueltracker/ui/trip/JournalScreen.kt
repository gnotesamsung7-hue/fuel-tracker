package com.parengmark.fueltracker.ui.trip

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.dp
import com.parengmark.fueltracker.data.ConsumptionCalculator
import com.parengmark.fueltracker.data.FuelGauge
import com.parengmark.fueltracker.data.OdometerJournal
import com.parengmark.fueltracker.data.OdometerLog
import com.parengmark.fueltracker.ui.common.Formatting
import java.text.SimpleDateFormat
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JournalScreen(
    viewModel: TripViewModel,
    vehicleId: Long,
    onCheckIn: () -> Unit,
    onBack: () -> Unit
) {
    LaunchedEffect(vehicleId) { viewModel.load(vehicleId) }

    val vehicle by viewModel.vehicle.collectAsState()
    val logs by viewModel.logs.collectAsState()
    val fillUps by viewModel.fillUps.collectAsState()
    var pendingDelete by remember { mutableStateOf<OdometerLog?>(null) }

    pendingDelete?.let { log ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Delete this check-in?") },
            text = { Text("Odometer ${TripViewModel.plain(log.odometer)} at ${timeFormat.format(log.timestampMillis)}") },
            confirmButton = {
                TextButton(onClick = { viewModel.deleteLog(log); pendingDelete = null }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { pendingDelete = null }) { Text("Cancel") } }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Odometer journal") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onCheckIn,
                icon = { Icon(Icons.Filled.Key, contentDescription = null) },
                text = { Text("Check in") }
            )
        }
    ) { padding ->
        val v = vehicle ?: return@Scaffold
        val days = remember(logs, fillUps) { OdometerJournal.build(logs, fillUps) }

        if (days.isEmpty()) {
            Column(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
                Text("Nothing in the journal yet.", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                Text(
                    "Each time you start the engine, check in with the odometer. The journal " +
                        "shows how far you drive each day, and the app uses it to tell you when to top up."
                )
                Spacer(Modifier.height(16.dp))
                Button(onClick = onCheckIn) { Text("Check in now") }
            }
            return@Scaffold
        }

        val unit = v.distanceUnitLabel
        val economy = ConsumptionCalculator.averageDistancePerVolume(fillUps)
        val costPerDistance = ConsumptionCalculator.costPerDistance(fillUps)
        val today = LocalDate.now()
        val weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val monthStart = today.withDayOfMonth(1)
        val todayDistance = OdometerJournal.distanceSince(days, today)
        val weekDistance = OdometerJournal.distanceSince(days, weekStart)
        val monthDistance = OdometerJournal.distanceSince(days, monthStart)
        val recentDriving = days.filter { !it.date.isBefore(today.minusDays(29)) && it.distance > 0 }
        val avgPerDrivingDay = if (recentDriving.isNotEmpty()) recentDriving.sumOf { it.distance } / recentDriving.size else null

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Row(Modifier.fillMaxWidth()) {
                            Stat("Today", "${TripViewModel.plain(todayDistance)} $unit", Modifier.weight(1f))
                            Stat("This week", "${TripViewModel.plain(weekDistance)} $unit", Modifier.weight(1f))
                            Stat("This month", "${TripViewModel.plain(monthDistance)} $unit", Modifier.weight(1f))
                        }
                        if (avgPerDrivingDay != null) {
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "Average ${TripViewModel.plain(avgPerDrivingDay)} $unit on days you drive (last 30 days)",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }

            items(days, key = { it.date.toEpochDay() }) { day ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp)) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                if (day.date == today) "Today" else day.date.format(dayFormat),
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                "+${TripViewModel.plain(day.distance)} $unit",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        val extras = buildList {
                            add("${TripViewModel.plain(day.firstOdometer)} → ${TripViewModel.plain(day.lastOdometer)}")
                            if (day.distance > 0 && economy != null) {
                                add("≈ ${"%.1f".format(day.distance / economy)} ${v.volumeUnitLabel}")
                            }
                            if (day.distance > 0 && costPerDistance != null) {
                                add("≈ ${Formatting.money(day.distance * costPerDistance, v.currencyCode)}")
                            }
                        }
                        Text(extras.joinToString(" · "), style = MaterialTheme.typography.bodySmall)

                        Spacer(Modifier.height(6.dp))
                        HorizontalDivider()

                        day.fillUps.forEach { f ->
                            Row(
                                Modifier.fillMaxWidth().padding(vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Filled.LocalGasStation, contentDescription = "Fill-up", modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    "Fill-up at ${TripViewModel.plain(f.odometer)} · ${TripViewModel.plain(f.volume)} ${v.volumeUnitLabel}" +
                                        (f.fuelGrade?.let { " · $it" } ?: ""),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                        day.checkIns.forEach { log ->
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.Key, contentDescription = "Check-in", modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                val parts = buildList {
                                    add(timeFormat.format(log.timestampMillis))
                                    add("${TripViewModel.plain(log.odometer)} $unit")
                                    log.fuelEighths?.let { add("gauge ${FuelGauge.label(it)}") }
                                    log.purpose?.let { add(it) }
                                    log.note?.let { add(it) }
                                }
                                Text(
                                    parts.joinToString(" · "),
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(onClick = { pendingDelete = log }) {
                                    Icon(Icons.Filled.Close, contentDescription = "Delete check-in", modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Stat(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(label, style = MaterialTheme.typography.bodySmall)
        Text(value, style = MaterialTheme.typography.titleMedium)
    }
}

private val dayFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE, MMM d", Locale.getDefault())
private val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
