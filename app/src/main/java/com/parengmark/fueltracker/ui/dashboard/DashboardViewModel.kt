package com.parengmark.fueltracker.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.parengmark.fueltracker.data.Expense
import com.parengmark.fueltracker.data.FillUp
import com.parengmark.fueltracker.data.FuelRepository
import com.parengmark.fueltracker.data.Vehicle
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class DashboardViewModel(private val repository: FuelRepository) : ViewModel() {

    private val _vehicle = MutableStateFlow<Vehicle?>(null)
    val vehicle: StateFlow<Vehicle?> = _vehicle.asStateFlow()

    private val _fillUps = MutableStateFlow<List<FillUp>>(emptyList())
    val fillUps: StateFlow<List<FillUp>> = _fillUps.asStateFlow()

    private val _expenses = MutableStateFlow<List<Expense>>(emptyList())
    val expenses: StateFlow<List<Expense>> = _expenses.asStateFlow()

    private var expenseJob: Job? = null
    private var loadedVehicleId: Long? = null
    private var collectJob: Job? = null

    /** Safe to call every recomposition -- only actually (re)loads when the
     * vehicle id changes. */
    fun load(vehicleId: Long) {
        if (loadedVehicleId == vehicleId) return
        loadedVehicleId = vehicleId

        viewModelScope.launch { _vehicle.value = repository.getVehicle(vehicleId) }

        collectJob?.cancel()
        collectJob = viewModelScope.launch {
            repository.observeFillUps(vehicleId).collect { _fillUps.value = it }
        }

        expenseJob?.cancel()
        expenseJob = viewModelScope.launch {
            repository.observeExpenses(vehicleId).collect { _expenses.value = it }
        }
    }
}
