package com.parengmark.fueltracker.ui.compare

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.parengmark.fueltracker.data.ConsumptionCalculator
import com.parengmark.fueltracker.ui.common.Formatting

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompareVehiclesScreen(
    viewModel: CompareVehiclesViewModel,
    onBack: () -> Unit
) {
    LaunchedEffect(Unit) { viewModel.load() }
    val summaries by viewModel.summaries.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Compare vehicles") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (summaries.isEmpty()) {
            Column(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
                Text("Add at least one vehicle to see it here.")
            }
        } else {
            val householdTotal = summaries.sumOf { it.totalSpend }
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp)) {
                            Text("Household total fuel spend", style = MaterialTheme.typography.bodyMedium)
                            Text(
                                Formatting.money(householdTotal, summaries.first().vehicle.currencyCode),
                                style = MaterialTheme.typography.headlineMedium
                            )
                        }
                    }
                }

                items(summaries, key = { it.vehicle.id }) { summary ->
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp)) {
                            Text(summary.vehicle.name, style = MaterialTheme.typography.titleLarge)
                            Text(
                                "Economy: " + ConsumptionCalculator.formatEconomy(summary.avgEconomy, summary.vehicle),
                                style = MaterialTheme.typography.bodyMedium
                            )
                            if (summary.costPerDistance != null) {
                                Text(
                                    "Cost: " + Formatting.money(summary.costPerDistance, summary.vehicle.currencyCode) +
                                        " per ${summary.vehicle.distanceUnitLabel}",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                            Text(
                                "Total spent: " + Formatting.money(summary.totalSpend, summary.vehicle.currencyCode) +
                                    " (${summary.fillUpCount} fill-ups)",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
        }
    }
}
