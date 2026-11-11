package com.parengmark.fueltracker.ui.stations

import android.content.Context
import android.location.Location
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
    data class Empty(val radiusKm: Int) : NearbyState()
    /** [youLat]/[youLon] is where you are; [searchLat]/[searchLon] is where the
     * search was centred (the same, unless you used "Search this area"). */
    data class Loaded(
        val stations: List<FuelStation>,
        val youLat: Double?,
        val youLon: Double?,
        val searchLat: Double,
        val searchLon: Double,
        val radiusKm: Int
    ) : NearbyState()
    data class Error(val message: String) : NearbyState()
}

/** No repository dependency -- this doesn't need [com.parengmark.fueltracker.ui.common.AppViewModelFactory],
 * Compose's default no-arg ViewModel factory is enough. */
class NearbyStationsViewModel : ViewModel() {

    private val _state = MutableStateFlow<NearbyState>(NearbyState.Idle)
    val state: StateFlow<NearbyState> = _state.asStateFlow()

    var radiusKm: Int = 5
        private set

    private var youLat: Double? = null
    private var youLon: Double? = null

    fun search(context: Context, radiusKm: Int = this.radiusKm) {
        this.radiusKm = radiusKm
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
            youLat = location.latitude
            youLon = location.longitude
            runSearch(location.latitude, location.longitude)
        }
    }

    /** "Search this area": look around wherever the map is centred now. */
    fun searchAt(lat: Double, lon: Double) {
        viewModelScope.launch {
            _state.value = NearbyState.Loading
            runSearch(lat, lon)
        }
    }

    private suspend fun runSearch(lat: Double, lon: Double) {
        val found = OverpassApi.findNearbyFuelStations(lat, lon, radiusKm * 1000)
        // Distances are always "from you", even after "Search this area".
        val yl = youLat
        val yn = youLon
        val stations = if (yl != null && yn != null) {
            found.map { st ->
                val d = FloatArray(1)
                Location.distanceBetween(yl, yn, st.lat, st.lon, d)
                st.copy(distanceMeters = d[0])
            }.sortedBy { it.distanceMeters }
        } else found
        _state.value = if (stations.isEmpty()) {
            NearbyState.Empty(radiusKm)
        } else {
            NearbyState.Loaded(stations, youLat, youLon, lat, lon, radiusKm)
        }
    }

    fun permissionDenied() {
        _state.value = NearbyState.Error("Location permission is needed to find stations near you.")
    }
}
