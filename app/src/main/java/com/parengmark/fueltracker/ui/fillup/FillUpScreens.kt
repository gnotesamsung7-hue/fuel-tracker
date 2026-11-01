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
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.TextButton
import com.parengmark.fueltracker.data.FuelGrades
import com.parengmark.fueltracker.ui.art.FILL_UP_BACKGROUND_ALPHA
import com.parengmark.fueltracker.ui.art.PixelArt
import com.parengmark.fueltracker.ui.art.PixelArtImage
import java.util.Locale

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
                                Text(
                                    listOfNotNull(
                                        fillUp.fuelGrade,
                                        Formatting.money(fillUp.pricePerUnit, v.currencyCode) + "/${v.volumeUnitLabel}",
                                        fillUp.stationName?.takeIf { it.isNotBlank() }
                                    ).joinToString(" · "),
                                    style = MaterialTheme.typography.bodyMedium
                                )
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

private enum class Field { VOLUME, PRICE, COST }

private fun parseNum(text: String): Double? = text.trim().replace(",", "").toDoubleOrNull()

/** Plain decimal with up to [decimals] places and no trailing zeros, always
 * with a "." so it parses back the same way regardless of phone locale. */
private fun fmt(value: Double, decimals: Int): String =
    String.format(Locale.US, "%.${decimals}f", value).trimEnd('0').trimEnd('.')

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddFillUpScreen(
    viewModel: FillUpViewModel,
    vehicleId: Long,
    onSaved: () -> Unit,
    onBack: () -> Unit,
    initialVolumeText: String = "",
    initialCostText: String = "",
    initialStationText: String = "",
    onOpenArtStudio: (() -> Unit)? = null
) {
    LaunchedEffect(vehicleId) { viewModel.load(vehicleId) }

    val vehicle by viewModel.vehicle.collectAsState()
    val lastFillUp by viewModel.lastFillUp.collectAsState()
    val saveError by viewModel.saveError.collectAsState()

    val fromScan = initialVolumeText.isNotBlank() || initialCostText.isNotBlank()

    var dateMillis by remember { mutableStateOf(System.currentTimeMillis()) }
    var odometerText by remember { mutableStateOf("") }
    var volumeText by remember { mutableStateOf(initialVolumeText) }
    var priceText by remember { mutableStateOf("") }
    var costText by remember { mutableStateOf(initialCostText) }
    var fullTank by remember { mutableStateOf(true) }
    var fuelGrade by remember { mutableStateOf<String?>(null) }
    var gradeTouched by remember { mutableStateOf(false) }
    var station by remember { mutableStateOf(initialStationText) }
    var notes by remember { mutableStateOf("") }
    var showDatePicker by remember { mutableStateOf(false) }

    // Volume, price per unit and total: type any two and the third fills in.
    // The field you touched longest ago is the one that gets calculated.
    var recency by remember { mutableStateOf(listOf(Field.COST, Field.VOLUME, Field.PRICE)) }
    val calculated = recency.last()

    fun recalc() {
        val v = parseNum(volumeText)
        val p = parseNum(priceText)
        val c = parseNum(costText)
        when (recency.last()) {
            Field.PRICE -> if (v != null && c != null && v > 0) priceText = fmt(c / v, 2)
            Field.VOLUME -> if (p != null && c != null && p > 0) volumeText = fmt(c / p, 3)
            Field.COST -> if (v != null && p != null) costText = fmt(v * p, 2)
        }
    }

    fun edited(field: Field, text: String) {
        when (field) {
            Field.VOLUME -> volumeText = text
            Field.PRICE -> priceText = text
            Field.COST -> costText = text
        }
        recency = listOf(field) + recency.filter { it != field }
        recalc()
    }

    // A scanned receipt gives volume + total: work out the price straight away.
    LaunchedEffect(Unit) { recalc() }

    // Default the octane to whatever was used last time (most people stick to one).
    LaunchedEffect(lastFillUp?.id) {
        if (!gradeTouched && fuelGrade == null) fuelGrade = lastFillUp?.fuelGrade
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
                title = { Text("Add fill-up") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        val v = vehicle
        val artCells = remember(v?.pixelArt) { v?.pixelArt?.let { PixelArt.decode(it).toList() } }

        Box(
            Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // The user's own pixel car from the Art studio, faint, behind the form.
            if (artCells != null) {
                PixelArtImage(
                    cells = artCells,
                    alpha = FILL_UP_BACKGROUND_ALPHA,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 16.dp)
                )
            }

            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
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
                    Text("  " + Formatting.date(dateMillis))
                }

                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = odometerText,
                    onValueChange = { odometerText = it },
                    label = { Text("Odometer" + if (v != null) " (${v.distanceUnitLabel})" else "") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (lastFillUp != null) {
                    Text(
                        "Last logged: ${lastFillUp!!.odometer}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                Spacer(Modifier.height(12.dp))
                Text("Octane / fuel", style = MaterialTheme.typography.titleSmall)
                Row(
                    Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FuelGrades.all.forEach { grade ->
                        FilterChip(
                            selected = fuelGrade == grade.stored,
                            onClick = {
                                gradeTouched = true
                                fuelGrade = if (fuelGrade == grade.stored) null else grade.stored
                            },
                            label = { Text(grade.chip) }
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))
                val unitLabel = v?.volumeUnitLabel ?: "L"
                OutlinedTextField(
                    value = priceText,
                    onValueChange = { edited(Field.PRICE, it) },
                    label = { Text("Price per $unitLabel" + if (v != null) " (${v.currencyCode})" else "") },
                    supportingText = if (calculated == Field.PRICE && priceText.isNotBlank()) {
                        { Text("Calculated from volume and total") }
                    } else null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                val lastPrice = lastFillUp?.pricePerUnit
                if (priceText.isBlank() && lastPrice != null && lastPrice > 0 && v != null) {
                    TextButton(onClick = { edited(Field.PRICE, fmt(lastPrice, 2)) }) {
                        Text("Use last price: ${Formatting.money(lastPrice, v.currencyCode)}/$unitLabel")
                    }
                }

                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = volumeText,
                    onValueChange = { edited(Field.VOLUME, it) },
                    label = { Text("Volume ($unitLabel)") },
                    supportingText = if (calculated == Field.VOLUME && volumeText.isNotBlank()) {
                        { Text("Calculated from price and total") }
                    } else null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = costText,
                    onValueChange = { edited(Field.COST, it) },
                    label = { Text("Total cost" + if (v != null) " (${v.currencyCode})" else "") },
                    supportingText = if (calculated == Field.COST && costText.isNotBlank()) {
                        { Text("Calculated from volume and price") }
                    } else null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
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
                    singleLine = true,
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
                            odometer = parseNum(odometerText) ?: -1.0,
                            volume = parseNum(volumeText) ?: -1.0,
                            totalCost = parseNum(costText) ?: -1.0,
                            fullTank = fullTank,
                            stationName = station,
                            notes = notes,
                            fuelGrade = fuelGrade,
                            onSaved = onSaved
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                ) {
                    Text("Save fill-up")
                }

                if (artCells == null && onOpenArtStudio != null) {
                    TextButton(
                        onClick = onOpenArtStudio,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        Text("Draw your car in the Art studio to show it here")
                    }
                }

                // Leave room so the bottom of the form can scroll clear of the car.
                Spacer(Modifier.height(if (artCells != null) 140.dp else 16.dp))
            }
        }
    }
}
