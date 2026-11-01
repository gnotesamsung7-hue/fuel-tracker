package com.parengmark.fueltracker.ui.dashboard

import android.content.Intent
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
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.parengmark.fueltracker.data.ConsumptionCalculator
import com.parengmark.fueltracker.data.CsvExporter
import com.parengmark.fueltracker.data.export.PdfExporter
import com.parengmark.fueltracker.data.export.XlsxExporter
import com.parengmark.fueltracker.ui.common.Formatting

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    vehicleId: Long,
    onAddFillUp: () -> Unit,
    onScanReceipt: () -> Unit,
    onOpenMaintenance: () -> Unit,
    onNearbyStations: () -> Unit,
    onCompareVehicles: () -> Unit,
    onOpenExpenses: () -> Unit,
    onAddExpense: () -> Unit,
    onViewAllFillUps: () -> Unit,
    onSwitchVehicle: () -> Unit
) {
    LaunchedEffect(vehicleId) { viewModel.load(vehicleId) }

    val vehicle by viewModel.vehicle.collectAsState()
    val fillUps by viewModel.fillUps.collectAsState()
    val expenses by viewModel.expenses.collectAsState()
    val context = LocalContext.current
    var menuExpanded by remember { mutableStateOf(false) }

    fun shareFile(uri: android.net.Uri, mimeType: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Export vehicle log"))
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(vehicle?.name ?: "Dashboard") },
                navigationIcon = {
                    IconButton(onClick = onSwitchVehicle) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Switch vehicle")
                    }
                },
                actions = {
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(Icons.Filled.MoreVert, contentDescription = "More")
                    }
                    DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                        DropdownMenuItem(
                            text = { Text("Scan receipt") },
                            onClick = { menuExpanded = false; onScanReceipt() }
                        )
                        DropdownMenuItem(
                            text = { Text("Nearby fuel stations") },
                            onClick = { menuExpanded = false; onNearbyStations() }
                        )
                        DropdownMenuItem(
                            text = { Text("Expenses") },
                            onClick = { menuExpanded = false; onOpenExpenses() }
                        )
                        DropdownMenuItem(
                            text = { Text("Maintenance") },
                            onClick = { menuExpanded = false; onOpenMaintenance() }
                        )
                        DropdownMenuItem(
                            text = { Text("Compare vehicles") },
                            onClick = { menuExpanded = false; onCompareVehicles() }
                        )
                        HorizontalDivider()
                        DropdownMenuItem(
                            text = { Text("Export as CSV") },
                            onClick = {
                                menuExpanded = false
                                val v = vehicle
                                if (v != null) shareFile(CsvExporter.export(context, v, fillUps, expenses), "text/csv")
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Export as Excel") },
                            onClick = {
                                menuExpanded = false
                                val v = vehicle
                                if (v != null) {
                                    shareFile(
                                        XlsxExporter.export(context, v, fillUps, expenses),
                                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                                    )
                                }
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Export as PDF") },
                            onClick = {
                                menuExpanded = false
                                val v = vehicle
                                if (v != null) shareFile(PdfExporter.export(context, v, fillUps, expenses), "application/pdf")
                            }
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddFillUp) {
                Icon(Icons.Filled.Add, contentDescription = "Add fill-up")
            }
        }
    ) { padding ->
        val v = vehicle
        if (v != null) {
            val avgEconomy = ConsumptionCalculator.averageDistancePerVolume(fillUps)
            val costPerDistance = ConsumptionCalculator.costPerDistance(fillUps)
            val segments = ConsumptionCalculator.computeSegments(fillUps)

            // One scrolling list for the whole dashboard, so the cards above
            // never squeeze the recent fill-ups off a small screen.
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 88.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp)) {
                            Text("Average economy", style = MaterialTheme.typography.bodyMedium)
                            Text(
                                ConsumptionCalculator.formatEconomy(avgEconomy, v),
                                style = MaterialTheme.typography.headlineMedium
                            )
                            if (costPerDistance != null) {
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    "${Formatting.money(costPerDistance, v.currencyCode)} per ${v.distanceUnitLabel} (fuel)",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }

                item {
                    CostOfOwnershipCard(
                        vehicle = v,
                        fillUps = fillUps,
                        expenses = expenses,
                        onOpenExpenses = onOpenExpenses,
                        onAddExpense = onAddExpense
                    )
                }

                if (segments.isNotEmpty()) {
                    item {
                        Card(Modifier.fillMaxWidth()) {
                            EconomyTrendChart(
                                segments = segments,
                                vehicle = v,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                            )
                        }
                    }
                }

                item {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Recent fill-ups", style = MaterialTheme.typography.titleLarge)
                        TextButton(onClick = onViewAllFillUps) { Text("View all") }
                    }
                }

                if (fillUps.isEmpty()) {
                    item { Text("No fill-ups logged yet. Tap + to add your first one.") }
                } else {
                    items(fillUps.take(5), key = { it.id }) { fillUp ->
                        Card(Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(12.dp)) {
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
                            }
                        }
                    }
                }
            }
        }
    }
}
