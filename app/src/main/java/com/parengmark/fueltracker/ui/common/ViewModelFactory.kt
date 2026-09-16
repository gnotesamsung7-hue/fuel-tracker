package com.parengmark.fueltracker.ui.common

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import com.parengmark.fueltracker.data.FuelRepository
import com.parengmark.fueltracker.ui.compare.CompareVehiclesViewModel
import com.parengmark.fueltracker.ui.dashboard.DashboardViewModel
import com.parengmark.fueltracker.ui.fillup.FillUpViewModel
import com.parengmark.fueltracker.ui.maintenance.MaintenanceViewModel
import com.parengmark.fueltracker.ui.vehicle.VehicleViewModel

/** One small factory for every ViewModel in the app -- see [com.parengmark.fueltracker.FuelTrackerApp]
 * for why this project uses manual DI instead of Hilt in v1. */
class AppViewModelFactory(private val repository: FuelRepository) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
        return when {
            modelClass.isAssignableFrom(VehicleViewModel::class.java) ->
                VehicleViewModel(repository) as T
            modelClass.isAssignableFrom(DashboardViewModel::class.java) ->
                DashboardViewModel(repository) as T
            modelClass.isAssignableFrom(FillUpViewModel::class.java) ->
                FillUpViewModel(repository) as T
            modelClass.isAssignableFrom(MaintenanceViewModel::class.java) ->
                MaintenanceViewModel(repository) as T
            modelClass.isAssignableFrom(CompareVehiclesViewModel::class.java) ->
                CompareVehiclesViewModel(repository) as T
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
