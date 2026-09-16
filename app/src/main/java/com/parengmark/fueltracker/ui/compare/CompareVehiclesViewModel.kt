package com.parengmark.fueltracker.ui.compare

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.parengmark.fueltracker.data.ConsumptionCalculator
import com.parengmark.fueltracker.data.FuelRepository
import com.parengmark.fueltracker.data.Vehicle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class VehicleSummary(
    val vehicle: Vehicle,
    val avgEconomy: Double?,
    val costPerDistance: Double?,
    val totalSpend: Double,
    val fillUpCount: Int
)

class CompareVehiclesViewModel(private val repository: FuelRepository) : ViewModel() {

    private val _summaries = MutableStateFlow<List<VehicleSummary>>(emptyList())
    val summaries: StateFlow<List<VehicleSummary>> = _summaries.asStateFlow()

    private var loaded = false

    /** One-shot snapshot rather than a live subscription -- good enough for a
     * screen the user visits occasionally to compare vehicles, and much
     * simpler than keeping N per-vehicle fill-up flows all wired together. */
    fun load() {
        if (loaded) return
        loaded = true
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            val vehicles = repository.observeVehicles().first()
            _summaries.value = vehicles.map { vehicle ->
                val fillUps = repository.getFillUpsAscending(vehicle.id)
                VehicleSummary(
                    vehicle = vehicle,
                    avgEconomy = ConsumptionCalculator.averageDistancePerVolume(fillUps),
                    costPerDistance = ConsumptionCalculator.costPerDistance(fillUps),
                    totalSpend = fillUps.sumOf { it.totalCost },
                    fillUpCount = fillUps.size
                )
            }
        }
    }
}
