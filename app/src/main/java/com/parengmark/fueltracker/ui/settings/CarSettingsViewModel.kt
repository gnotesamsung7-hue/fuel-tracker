package com.parengmark.fueltracker.ui.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.parengmark.fueltracker.data.Achievement
import com.parengmark.fueltracker.data.Achievements
import com.parengmark.fueltracker.data.FuelRepository
import com.parengmark.fueltracker.data.Vehicle
import com.parengmark.fueltracker.work.NotificationHelper
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

/** Car settings (tank, Bluetooth, theme) and the achievements screen. */
class CarSettingsViewModel(private val repository: FuelRepository) : ViewModel() {

    private val _vehicle = MutableStateFlow<Vehicle?>(null)
    val vehicle: StateFlow<Vehicle?> = _vehicle.asStateFlow()

    private val _achievements = MutableStateFlow<List<Achievement>>(emptyList())
    val achievements: StateFlow<List<Achievement>> = _achievements.asStateFlow()

    private var loadedVehicleId: Long? = null
    private var job: Job? = null

    fun load(vehicleId: Long) {
        if (loadedVehicleId == vehicleId) return
        loadedVehicleId = vehicleId
        job?.cancel()
        job = viewModelScope.launch {
            combine(
                repository.observeVehicle(vehicleId),
                repository.observeFillUps(vehicleId),
                repository.observeOdometerLogs(vehicleId),
                repository.observeExpenses(vehicleId)
            ) { v, fillUps, logs, expenses ->
                v to (if (v != null) Achievements.compute(v, fillUps, logs, expenses) else emptyList())
            }.collect { (v, list) ->
                _vehicle.value = v
                _achievements.value = list
            }
        }
    }

    fun setTankCapacity(vehicleId: Long, text: String): Boolean {
        val capacity = text.trim().replace(",", "").toDoubleOrNull()
        if (capacity == null || capacity <= 0 || capacity > 500) return false
        viewModelScope.launch { repository.saveTankCapacity(vehicleId, capacity) }
        return true
    }

    fun setCarBluetooth(vehicleId: Long, address: String?, name: String?) {
        viewModelScope.launch { repository.saveCarBluetooth(vehicleId, address, name) }
    }

    /** Posts the same notification the car connection would, so the user can see it work. */
    fun sendTestPrompt(context: Context, vehicleId: Long) {
        val appContext = context.applicationContext
        viewModelScope.launch {
            val v = repository.getVehicle(vehicleId) ?: return@launch
            NotificationHelper.showCheckInPrompt(appContext, v, repository.estimateFuel(vehicleId))
        }
    }
}
