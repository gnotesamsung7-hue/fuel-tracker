package com.parengmark.fueltracker.ui.art

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.parengmark.fueltracker.data.FuelRepository
import com.parengmark.fueltracker.data.Vehicle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ArtStudioViewModel(private val repository: FuelRepository) : ViewModel() {

    private val _vehicle = MutableStateFlow<Vehicle?>(null)
    val vehicle: StateFlow<Vehicle?> = _vehicle.asStateFlow()

    private var loadedVehicleId: Long? = null

    fun load(vehicleId: Long) {
        if (loadedVehicleId == vehicleId) return
        loadedVehicleId = vehicleId
        viewModelScope.launch { _vehicle.value = repository.getVehicle(vehicleId) }
    }

    fun save(vehicleId: Long, encoded: String?, onSaved: () -> Unit) {
        viewModelScope.launch {
            repository.saveVehicleArt(vehicleId, encoded)
            onSaved()
        }
    }
}
