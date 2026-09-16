package com.parengmark.fueltracker.ui.vehicle

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.parengmark.fueltracker.data.FuelRepository
import com.parengmark.fueltracker.data.MetricEconomyDisplay
import com.parengmark.fueltracker.data.UnitSystem
import com.parengmark.fueltracker.data.Vehicle
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class VehicleViewModel(private val repository: FuelRepository) : ViewModel() {

    val vehicles: StateFlow<List<Vehicle>> = repository.observeVehicles()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addVehicle(
        name: String,
        unitSystem: UnitSystem,
        metricEconomyDisplay: MetricEconomyDisplay,
        currencyCode: String
    ) {
        if (name.isBlank()) return
        viewModelScope.launch {
            repository.saveVehicle(
                Vehicle(
                    name = name.trim(),
                    unitSystem = unitSystem,
                    metricEconomyDisplay = metricEconomyDisplay,
                    currencyCode = currencyCode.trim().ifBlank { "PHP" }
                )
            )
        }
    }

    fun deleteVehicle(vehicle: Vehicle) {
        viewModelScope.launch { repository.deleteVehicle(vehicle) }
    }
}
