package com.parengmark.fueltracker.ui.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.parengmark.fueltracker.data.Expense
import com.parengmark.fueltracker.data.FillUp
import com.parengmark.fueltracker.data.Vehicle
import com.parengmark.fueltracker.ui.common.Formatting

/** Fuel + every other expense, and what that works out to per km/mi driven.
 * Distance comes from the fuel log (first to last odometer), the same span
 * the fuel-only cost-per-distance figure uses. */
@Composable
fun CostOfOwnershipCard(
    vehicle: Vehicle,
    fillUps: List<FillUp>,
    expenses: List<Expense>,
    onOpenExpenses: () -> Unit,
    onAddExpense: () -> Unit
) {
    val currency = vehicle.currencyCode
    val fuelTotal = fillUps.sumOf { it.totalCost }
    val otherTotal = expenses.sumOf { it.amount }
    val allIn = fuelTotal + otherTotal
    val distance = if (fillUps.size >= 2) {
        fillUps.maxOf { it.odometer } - fillUps.minOf { it.odometer }
    } else 0.0

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text("Total cost to run", style = MaterialTheme.typography.bodyMedium)
            Text(Formatting.money(allIn, currency), style = MaterialTheme.typography.headlineSmall)
            Text(
                "Fuel ${Formatting.money(fuelTotal, currency)} · Other ${Formatting.money(otherTotal, currency)}",
                style = MaterialTheme.typography.bodyMedium
            )
            if (distance > 0 && otherTotal > 0) {
                Text(
                    "All-in: ${Formatting.money(allIn / distance, currency)} per ${vehicle.distanceUnitLabel}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onAddExpense) { Text("Add expense") }
                TextButton(onClick = onOpenExpenses) {
                    Text(if (expenses.isEmpty()) "Expenses" else "Expenses (${expenses.size})")
                }
            }
        }
    }
}
