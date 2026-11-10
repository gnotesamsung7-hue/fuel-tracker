package com.parengmark.fueltracker.ui.trip

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.parengmark.fueltracker.data.ConsumptionCalculator
import com.parengmark.fueltracker.data.FillUp
import com.parengmark.fueltracker.data.FuelEstimate
import com.parengmark.fueltracker.data.FuelEstimator
import com.parengmark.fueltracker.data.FuelRepository
import com.parengmark.fueltracker.data.OdometerLog
import com.parengmark.fueltracker.data.Vehicle
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

/** Shared by the engine check-in screen, the odometer journal and the
 * dashboard's check-in card. */
class TripViewModel(private val repository: FuelRepository) : ViewModel() {

    private val _vehicle = MutableStateFlow<Vehicle?>(null)
    val vehicle: StateFlow<Vehicle?> = _vehicle.asStateFlow()

    private val _logs = MutableStateFlow<List<OdometerLog>>(emptyList())
    val logs: StateFlow<List<OdometerLog>> = _logs.asStateFlow()

    private val _fillUps = MutableStateFlow<List<FillUp>>(emptyList())
    val fillUps: StateFlow<List<FillUp>> = _fillUps.asStateFlow()

    private val _estimate = MutableStateFlow<FuelEstimate?>(null)
    val estimate: StateFlow<FuelEstimate?> = _estimate.asStateFlow()

    private val _saveError = MutableStateFlow<String?>(null)
    val saveError: StateFlow<String?> = _saveError.asStateFlow()

    /** Set right after a check-in is saved, so the screen can show the
     * top-up verdict; cleared when the user dismisses it. */
    private val _checkInResult = MutableStateFlow<CheckInResult?>(null)
    val checkInResult: StateFlow<CheckInResult?> = _checkInResult.asStateFlow()

    data class CheckInResult(val estimate: FuelEstimate?, val distanceSinceLast: Double?)

    private var loadedVehicleId: Long? = null
    private var job: Job? = null

    fun load(vehicleId: Long) {
        if (loadedVehicleId == vehicleId) return
        loadedVehicleId = vehicleId
        job?.cancel()
        job = viewModelScope.launch {
            combine(
                repository.observeVehicle(vehicleId),
                repository.observeOdometerLogs(vehicleId),
                repository.observeFillUps(vehicleId)
            ) { v, logs, fillUps -> Triple(v, logs, fillUps) }
                .collect { (v, logs, fillUps) ->
                    _vehicle.value = v
                    _logs.value = logs
                    _fillUps.value = fillUps
                    _estimate.value = FuelEstimator.estimate(
                        tankCapacity = v?.tankCapacity,
                        fillUps = fillUps,
                        logs = logs,
                        distancePerVolume = ConsumptionCalculator.averageDistancePerVolume(fillUps)
                    )
                }
        }
    }

    /** Highest reading so far, from check-ins or fill-ups. */
    fun lastReading(): Double? =
        (logs.value.map { it.odometer } + fillUps.value.map { it.odometer }).maxOrNull()

    fun clearSaveError() {
        _saveError.value = null
    }

    fun checkIn(
        vehicleId: Long,
        odometerText: String,
        fuelEighths: Int?,
        purpose: String?,
        note: String
    ) {
        val odometer = odometerText.trim().replace(",", "").toDoubleOrNull()
        if (odometer == null || odometer < 0) {
            _saveError.value = "Enter the odometer reading."
            return
        }
        val last = lastReading()
        if (last != null && odometer < last) {
            _saveError.value = "That's lower than the last reading (${plain(last)}). Check the number?"
            return
        }
        _saveError.value = null
        viewModelScope.launch {
            repository.saveOdometerLog(
                OdometerLog(
                    vehicleId = vehicleId,
                    timestampMillis = System.currentTimeMillis(),
                    odometer = odometer,
                    fuelEighths = fuelEighths,
                    purpose = purpose,
                    note = note.trim().ifBlank { null }
                )
            )
            _checkInResult.value = CheckInResult(
                estimate = repository.estimateFuel(vehicleId),
                distanceSinceLast = last?.let { odometer - it }
            )
        }
    }

    fun dismissCheckInResult() {
        _checkInResult.value = null
    }

    fun deleteLog(log: OdometerLog) {
        viewModelScope.launch { repository.deleteOdometerLog(log) }
    }

    fun setTankCapacity(vehicleId: Long, capacityText: String): Boolean {
        val capacity = capacityText.trim().replace(",", "").toDoubleOrNull()
        if (capacity == null || capacity <= 0 || capacity > 500) return false
        viewModelScope.launch { repository.saveTankCapacity(vehicleId, capacity) }
        return true
    }

    companion object {
        fun plain(value: Double): String =
            if (value == Math.floor(value)) value.toLong().toString() else "%.1f".format(value)
    }
}
