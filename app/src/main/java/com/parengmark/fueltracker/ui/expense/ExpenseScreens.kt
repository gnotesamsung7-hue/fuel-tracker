package com.parengmark.fueltracker.ui.expense

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Handyman
import androidx.compose.material.icons.filled.LocalCarWash
import androidx.compose.material.icons.filled.LocalParking
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.OilBarrel
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.TireRepair
import androidx.compose.material.icons.filled.Toll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.parengmark.fueltracker.data.Expense
import com.parengmark.fueltracker.data.ExpenseCategory
import com.parengmark.fueltracker.ui.common.Formatting
import java.util.Calendar

fun ExpenseCategory.icon(): ImageVector = when (this) {
    ExpenseCategory.MAINTENANCE -> Icons.Filled.Handyman
    ExpenseCategory.OIL_CHANGE -> Icons.Filled.OilBarrel
    ExpenseCategory.REPAIR -> Icons.Filled.Build
    ExpenseCategory.TIRES -> Icons.Filled.TireRepair
    ExpenseCategory.TOLL -> Icons.Filled.Toll
    ExpenseCategory.PARKING -> Icons.Filled.LocalParking
    ExpenseCategory.INSURANCE -> Icons.Filled.Security
    ExpenseCategory.REGISTRATION -> Icons.Filled.Badge
    ExpenseCategory.CAR_WASH -> Icons.Filled.LocalCarWash
    ExpenseCategory.OTHER -> Icons.Filled.MoreHoriz
}

private fun startOfThisMonthMillis(): Long = Calendar.getInstance().apply {
    set(Calendar.DAY_OF_MONTH, 1)
    set(Calendar.HOUR_OF_DAY, 0)
    set(Calendar.MINUTE, 0)
    set(Calendar.SECOND, 0)
    set(Calendar.MILLISECOND, 0)
}.timeInMillis

/** Shows a number without a pointless ".0" (12345.0 -> "12345"). */
private fun plainNumber(value: Double): String =
    if (value == Math.floor(value)) value.toLong().toString() else value.toString()

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseListScreen(
    viewModel: ExpenseViewModel,
    vehicleId: Long,
    onAddExpense: () -> Unit,
    onEditExpense: (Long) -> Unit,
    onBack: () -> Unit
) {
    LaunchedEffect(vehicleId) { viewModel.load(vehicleId) }

    val vehicle by viewModel.vehicle.collectAsState()
    val expenses by viewModel.expenses.collectAsState()
    val fuelTotal by viewModel.fuelTotal.collectAsState()

    var filter by remember { mutableStateOf<ExpenseCategory?>(null) }
    var pendingDelete by remember { mutableStateOf<Expense?>(null) }

    pendingDelete?.let { toDelete ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Delete this expense?") },
            text = {
                Text(
                    "${toDelete.category.label} · " +
                        Formatting.money(toDelete.amount, vehicle?.currencyCode ?: "PHP") +
                        " on ${Formatting.date(toDelete.dateEpochMillis)}"
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteExpense(toDelete)
                    pendingDelete = null
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("Cancel") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Expenses") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddExpense) {
                Icon(Icons.Filled.Add, contentDescription = "Add expense")
            }
        }
    ) { padding ->
        val v = vehicle
        if (v == null) return@Scaffold

        if (expenses.isEmpty()) {
            Column(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
                Text("No expenses logged yet.", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                Text(
                    "Log repairs, PMS, tolls, parking, insurance, registration and anything " +
                        "else you spend on this vehicle. They're added to your fuel costs so you " +
                        "can see what the car really costs to run."
                )
                Spacer(Modifier.height(16.dp))
                Button(onClick = onAddExpense) { Text("Add expense") }
            }
            return@Scaffold
        }

        val currency = v.currencyCode
        val total = expenses.sumOf { it.amount }
        val monthStart = startOfThisMonthMillis()
        val thisMonth = expenses.filter { it.dateEpochMillis >= monthStart }.sumOf { it.amount }
        val byCategory = expenses.groupBy { it.category }
            .mapValues { (_, list) -> list.sumOf { it.amount } }
            .toList()
            .sortedByDescending { it.second }
        val shown = if (filter == null) expenses else expenses.filter { it.category == filter }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 88.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Total expenses", style = MaterialTheme.typography.bodyMedium)
                        Text(Formatting.money(total, currency), style = MaterialTheme.typography.headlineMedium)
                        Text(
                            "This month: ${Formatting.money(thisMonth, currency)}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            "With fuel: ${Formatting.money(total + fuelTotal, currency)}",
                            style = MaterialTheme.typography.bodyMedium
                        )

                        Spacer(Modifier.height(12.dp))
                        HorizontalDivider()
                        Spacer(Modifier.height(12.dp))

                        val max = byCategory.firstOrNull()?.second ?: 0.0
                        byCategory.forEach { (category, sum) ->
                            CategoryBar(
                                category = category,
                                amountText = Formatting.money(sum, currency),
                                fraction = if (max > 0) (sum / max).toFloat() else 0f
                            )
                        }
                    }
                }
            }

            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        FilterChip(
                            selected = filter == null,
                            onClick = { filter = null },
                            label = { Text("All") }
                        )
                    }
                    items(byCategory.map { it.first }) { category ->
                        FilterChip(
                            selected = filter == category,
                            onClick = { filter = if (filter == category) null else category },
                            label = { Text(category.label) },
                            leadingIcon = {
                                Icon(category.icon(), contentDescription = null, modifier = Modifier.size(18.dp))
                            }
                        )
                    }
                }
            }

            items(shown, key = { it.id }) { expense ->
                Card(
                    Modifier
                        .fillMaxWidth()
                        .clickable { onEditExpense(expense.id) }
                ) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(expense.category.icon(), contentDescription = null)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(expense.category.label, style = MaterialTheme.typography.titleMedium)
                            val details = buildList {
                                add(Formatting.date(expense.dateEpochMillis))
                                expense.odometer?.let { add("${plainNumber(it)} ${v.distanceUnitLabel}") }
                                expense.vendor?.let { add(it) }
                            }
                            Text(details.joinToString(" · "), style = MaterialTheme.typography.bodyMedium)
                            if (!expense.notes.isNullOrBlank()) {
                                Text(expense.notes, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        Text(
                            Formatting.money(expense.amount, currency),
                            style = MaterialTheme.typography.titleMedium
                        )
                        IconButton(onClick = { pendingDelete = expense }) {
                            Icon(Icons.Filled.Delete, contentDescription = "Delete")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryBar(category: ExpenseCategory, amountText: String, fraction: Float) {
    Column(Modifier.padding(vertical = 4.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Icon(category.icon(), contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(category.label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            Text(amountText, style = MaterialTheme.typography.bodyMedium)
        }
        Spacer(Modifier.height(4.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .height(6.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(3.dp))
        ) {
            Box(
                Modifier
                    .fillMaxWidth(fraction.coerceIn(0.02f, 1f))
                    .height(6.dp)
                    .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(3.dp))
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditExpenseScreen(
    viewModel: ExpenseViewModel,
    vehicleId: Long,
    expenseId: Long?,
    initialCategory: ExpenseCategory?,
    initialNotes: String,
    onSaved: () -> Unit,
    onBack: () -> Unit
) {
    LaunchedEffect(vehicleId) { viewModel.load(vehicleId) }
    LaunchedEffect(expenseId) {
        viewModel.clearSaveError()
        viewModel.loadForEdit(expenseId)
    }

    val vehicle by viewModel.vehicle.collectAsState()
    val editing by viewModel.editing.collectAsState()
    val latestOdometer by viewModel.latestOdometer.collectAsState()
    val saveError by viewModel.saveError.collectAsState()

    val isEdit = expenseId != null && expenseId > 0

    var dateMillis by remember { mutableStateOf(System.currentTimeMillis()) }
    var category by remember { mutableStateOf(initialCategory ?: ExpenseCategory.MAINTENANCE) }
    var amountText by remember { mutableStateOf("") }
    var odometerText by remember { mutableStateOf("") }
    var vendor by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf(initialNotes) }
    var showDatePicker by remember { mutableStateOf(false) }

    // Fill the form once the expense being edited has loaded.
    LaunchedEffect(editing?.id) {
        val e = editing ?: return@LaunchedEffect
        if (e.id != expenseId) return@LaunchedEffect
        dateMillis = e.dateEpochMillis
        category = e.category
        amountText = plainNumber(e.amount)
        odometerText = e.odometer?.let { plainNumber(it) } ?: ""
        vendor = e.vendor ?: ""
        notes = e.notes ?: ""
    }

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
                title = { Text(if (isEdit) "Edit expense" else "Add expense") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        val v = vehicle
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Text("Category", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(4.dp))
            // Two short rows of chips rather than a dropdown: one tap, easy to
            // hit with a thumb.
            ExpenseCategory.values().toList().chunked(5).forEach { rowCategories ->
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(rowCategories) { c ->
                        FilterChip(
                            selected = category == c,
                            onClick = { category = c },
                            label = { Text(c.label) },
                            leadingIcon = {
                                Icon(c.icon(), contentDescription = null, modifier = Modifier.size(18.dp))
                            }
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = amountText,
                onValueChange = { amountText = it },
                label = { Text("Amount" + if (v != null) " (${v.currencyCode})" else "") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = { showDatePicker = true }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Filled.CalendarToday, contentDescription = null)
                Text("  " + Formatting.date(dateMillis))
            }

            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = odometerText,
                onValueChange = { odometerText = it },
                label = { Text("Odometer (optional" + if (v != null) ", ${v.distanceUnitLabel})" else ")") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            val latest = latestOdometer
            if (latest != null && odometerText.isBlank()) {
                TextButton(onClick = { odometerText = plainNumber(latest) }) {
                    Text("Use last fill-up reading: ${plainNumber(latest)}")
                }
            }

            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = vendor,
                onValueChange = { vendor = it },
                label = { Text("Shop / where (optional)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Notes (optional)") },
                modifier = Modifier.fillMaxWidth()
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
                    viewModel.saveExpense(
                        vehicleId = vehicleId,
                        dateEpochMillis = dateMillis,
                        category = category,
                        amountText = amountText,
                        odometerText = odometerText,
                        vendor = vendor,
                        notes = notes,
                        onSaved = onSaved
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
            ) {
                Text(if (isEdit) "Save changes" else "Save expense")
            }
        }
    }
}
