package com.parengmark.fueltracker.ui.maintenance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.parengmark.fueltracker.data.FuelRepository
import com.parengmark.fueltracker.data.MaintenanceReminder
import com.parengmark.fueltracker.data.Vehicle
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MaintenanceViewModel(private val repository: FuelRepository) : ViewModel() {

    private val _vehicle = MutableStateFlow<Vehicle?>(null)
    val vehicle: StateFlow<Vehicle?> = _vehicle.asStateFlow()

    private val _reminders = MutableStateFlow<List<MaintenanceReminder>>(emptyList())
    val reminders: StateFlow<List<MaintenanceReminder>> = _reminders.asStateFlow()

    /** Current odometer, the highest reading from fill-ups and engine check-ins -- used both to
     * pre-fill "mark done" and to compute due/overdue status. */
    private val _currentOdometer = MutableStateFlow<Double?>(null)
    val currentOdometer: StateFlow<Double?> = _currentOdometer.asStateFlow()

    private var loadedVehicleId: Long? = null
    private var collectJob: Job? = null

    fun load(vehicleId: Long) {
        if (loadedVehicleId == vehicleId) return
        loadedVehicleId = vehicleId

        viewModelScope.launch {
            _vehicle.value = repository.getVehicle(vehicleId)
            _currentOdometer.value = repository.getCurrentOdometer(vehicleId)
        }

        collectJob?.cancel()
        collectJob = viewModelScope.launch {
            repository.observeMaintenanceReminders(vehicleId).collect { _reminders.value = it }
        }
    }

    fun addReminder(vehicleId: Long, type: String, intervalDistance: Double?, intervalDays: Int?) {
        if (type.isBlank() || (intervalDistance == null && intervalDays == null)) return
        viewModelScope.launch {
            repository.saveMaintenanceReminder(
                MaintenanceReminder(
                    vehicleId = vehicleId,
                    type = type.trim(),
                    intervalDistance = intervalDistance,
                    intervalDays = intervalDays,
                    lastDoneOdometer = _currentOdometer.value,
                    lastDoneDateMillis = System.currentTimeMillis()
                )
            )
        }
    }

    /** Resets the reminder's countdown from right now. */
    fun markDone(reminder: MaintenanceReminder) {
        viewModelScope.launch {
            repository.updateMaintenanceReminder(
                reminder.copy(
                    lastDoneOdometer = _currentOdometer.value ?: reminder.lastDoneOdometer,
                    lastDoneDateMillis = System.currentTimeMillis()
                )
            )
        }
    }

    fun deleteReminder(reminder: MaintenanceReminder) {
        viewModelScope.launch { repository.deleteMaintenanceReminder(reminder) }
    }
}
