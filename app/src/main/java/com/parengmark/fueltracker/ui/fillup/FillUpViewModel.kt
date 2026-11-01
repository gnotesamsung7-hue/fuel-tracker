package com.parengmark.fueltracker.ui.fillup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.parengmark.fueltracker.data.FillUp
import com.parengmark.fueltracker.data.FuelRepository
import com.parengmark.fueltracker.data.Vehicle
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class FillUpViewModel(private val repository: FuelRepository) : ViewModel() {

    private val _vehicle = MutableStateFlow<Vehicle?>(null)
    val vehicle: StateFlow<Vehicle?> = _vehicle.asStateFlow()

    private val _fillUps = MutableStateFlow<List<FillUp>>(emptyList())
    val fillUps: StateFlow<List<FillUp>> = _fillUps.asStateFlow()

    private val _lastFillUp = MutableStateFlow<FillUp?>(null)
    val lastFillUp: StateFlow<FillUp?> = _lastFillUp.asStateFlow()

    /** Set after a save so the Add screen can pop back and the caller can show
     * a confirmation if it wants to. */
    private val _saveError = MutableStateFlow<String?>(null)
    val saveError: StateFlow<String?> = _saveError.asStateFlow()

    private var loadedVehicleId: Long? = null
    private var collectJob: Job? = null
    private var vehicleJob: Job? = null

    fun load(vehicleId: Long) {
        if (loadedVehicleId == vehicleId) return
        loadedVehicleId = vehicleId

        // Observed rather than read once, so a drawing saved in the Art
        // studio shows up as soon as you come back to the form.
        vehicleJob?.cancel()
        vehicleJob = viewModelScope.launch {
            repository.observeVehicle(vehicleId).collect { _vehicle.value = it }
        }

        collectJob?.cancel()
        collectJob = viewModelScope.launch {
            repository.observeFillUps(vehicleId).collect {
                _fillUps.value = it
                _lastFillUp.value = it.maxByOrNull { f -> f.odometer }
            }
        }
    }

    fun clearSaveError() {
        _saveError.value = null
    }

    /** Returns true if the save was accepted. Odometer must not go backwards
     * relative to the last logged fill-up for this vehicle -- this is the
     * "odometer validation" the spec calls for. A big jump is allowed (long
     * trip, or catching up after months of not logging) but never a decrease. */
    fun addFillUp(
        vehicleId: Long,
        dateEpochMillis: Long,
        odometer: Double,
        volume: Double,
        totalCost: Double,
        fullTank: Boolean,
        stationName: String?,
        notes: String?,
        fuelGrade: String? = null,
        onSaved: () -> Unit
    ) {
        if (odometer < 0) {
            _saveError.value = "Enter a valid odometer reading."
            return
        }
        val last = _lastFillUp.value
        if (last != null && odometer < last.odometer) {
            _saveError.value =
                "Odometer can't be lower than the last logged reading (${last.odometer})."
            return
        }
        if (volume <= 0 || totalCost <= 0) {
            _saveError.value = "Enter a valid volume and total cost (both greater than zero)."
            return
        }
        viewModelScope.launch {
            repository.saveFillUp(
                FillUp(
                    vehicleId = vehicleId,
                    dateEpochMillis = dateEpochMillis,
                    odometer = odometer,
                    volume = volume,
                    totalCost = totalCost,
                    fullTank = fullTank,
                    stationName = stationName?.trim()?.ifBlank { null },
                    notes = notes?.trim()?.ifBlank { null },
                    fuelGrade = fuelGrade
                )
            )
            onSaved()
        }
    }

    fun deleteFillUp(fillUp: FillUp) {
        viewModelScope.launch { repository.deleteFillUp(fillUp) }
    }
}
