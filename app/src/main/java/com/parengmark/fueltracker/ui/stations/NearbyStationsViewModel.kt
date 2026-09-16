package com.parengmark.fueltracker.ui.stations

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.parengmark.fueltracker.data.stations.FuelStation
import com.parengmark.fueltracker.data.stations.OverpassApi
import com.parengmark.fueltracker.location.LocationHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class NearbyState {
    data object Idle : NearbyState()
    data object Loading : NearbyState()
    data object Empty : NearbyState()
    data class Loaded(val stations: List<FuelStation>) : NearbyState()
    data class Error(val message: String) : NearbyState()
}

/** No repository dependency -- this doesn't need [com.parengmark.fueltracker.ui.common.AppViewModelFactory],
 * Compose's default no-arg ViewModel factory is enough. */
class NearbyStationsViewModel : ViewModel() {

    private val _state = MutableStateFlow<NearbyState>(NearbyState.Idle)
    val state: StateFlow<NearbyState> = _state.asStateFlow()

    fun search(context: Context) {
        viewModelScope.launch {
            _state.value = NearbyState.Loading
            val location = LocationHelper.getLastKnownLocation(context)
                ?: LocationHelper.requestSingleLocationUpdate(context)

            if (location == null) {
                _state.value = NearbyState.Error(
                    "Couldn't get your location. Make sure location is turned on for this device."
                )
                return@launch
            }

            val stations = OverpassApi.findNearbyFuelStations(location.latitude, location.longitude)
            _state.value = if (stations.isEmpty()) NearbyState.Empty else NearbyState.Loaded(stations)
        }
    }

    fun permissionDenied() {
        _state.value = NearbyState.Error("Location permission is needed to find stations near you.")
    }
}
