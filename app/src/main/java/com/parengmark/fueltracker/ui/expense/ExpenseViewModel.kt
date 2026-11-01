package com.parengmark.fueltracker.ui.expense

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.parengmark.fueltracker.data.Expense
import com.parengmark.fueltracker.data.ExpenseCategory
import com.parengmark.fueltracker.data.FuelRepository
import com.parengmark.fueltracker.data.Vehicle
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ExpenseViewModel(private val repository: FuelRepository) : ViewModel() {

    private val _vehicle = MutableStateFlow<Vehicle?>(null)
    val vehicle: StateFlow<Vehicle?> = _vehicle.asStateFlow()

    private val _expenses = MutableStateFlow<List<Expense>>(emptyList())
    val expenses: StateFlow<List<Expense>> = _expenses.asStateFlow()

    /** Total spent on fuel, so the expense screen can show the all-in figure. */
    private val _fuelTotal = MutableStateFlow(0.0)
    val fuelTotal: StateFlow<Double> = _fuelTotal.asStateFlow()

    /** Latest odometer from the fuel log, offered as a one-tap fill on the form. */
    private val _latestOdometer = MutableStateFlow<Double?>(null)
    val latestOdometer: StateFlow<Double?> = _latestOdometer.asStateFlow()

    /** The expense being edited, or null when adding a new one. */
    private val _editing = MutableStateFlow<Expense?>(null)
    val editing: StateFlow<Expense?> = _editing.asStateFlow()

    private val _saveError = MutableStateFlow<String?>(null)
    val saveError: StateFlow<String?> = _saveError.asStateFlow()

    private var loadedVehicleId: Long? = null
    private var expenseJob: Job? = null
    private var fuelJob: Job? = null

    fun load(vehicleId: Long) {
        if (loadedVehicleId == vehicleId) return
        loadedVehicleId = vehicleId

        viewModelScope.launch { _vehicle.value = repository.getVehicle(vehicleId) }

        expenseJob?.cancel()
        expenseJob = viewModelScope.launch {
            repository.observeExpenses(vehicleId).collect { _expenses.value = it }
        }
        fuelJob?.cancel()
        fuelJob = viewModelScope.launch {
            repository.observeFillUps(vehicleId).collect { fillUps ->
                _fuelTotal.value = fillUps.sumOf { it.totalCost }
                _latestOdometer.value = fillUps.maxOfOrNull { it.odometer }
            }
        }
    }

    /** Loads an existing expense into the form; a no-op for new ones. */
    fun loadForEdit(expenseId: Long?) {
        if (expenseId == null || expenseId <= 0) {
            _editing.value = null
            return
        }
        if (_editing.value?.id == expenseId) return
        viewModelScope.launch { _editing.value = repository.getExpense(expenseId) }
    }

    fun clearSaveError() {
        _saveError.value = null
    }

    fun saveExpense(
        vehicleId: Long,
        dateEpochMillis: Long,
        category: ExpenseCategory,
        amountText: String,
        odometerText: String,
        vendor: String,
        notes: String,
        onSaved: () -> Unit
    ) {
        val amount = parseNumber(amountText)
        if (amount == null || amount <= 0) {
            _saveError.value = "Enter an amount greater than zero."
            return
        }
        val odometer = if (odometerText.isBlank()) null else parseNumber(odometerText)
        if (odometerText.isNotBlank() && (odometer == null || odometer < 0)) {
            _saveError.value = "Odometer must be a number, or leave it blank."
            return
        }
        _saveError.value = null
        viewModelScope.launch {
            repository.saveExpense(
                Expense(
                    id = _editing.value?.id ?: 0,
                    vehicleId = vehicleId,
                    dateEpochMillis = dateEpochMillis,
                    category = category,
                    amount = amount,
                    odometer = odometer,
                    vendor = vendor.trim().ifBlank { null },
                    notes = notes.trim().ifBlank { null }
                )
            )
            _editing.value = null
            onSaved()
        }
    }

    fun deleteExpense(expense: Expense) {
        viewModelScope.launch { repository.deleteExpense(expense) }
    }

    /** Accepts "1,250.50" as well as "1250.50" -- people type peso amounts with commas. */
    private fun parseNumber(text: String): Double? =
        text.trim().replace(",", "").toDoubleOrNull()
}
