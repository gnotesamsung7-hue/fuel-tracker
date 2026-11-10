package com.parengmark.fueltracker.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.parengmark.fueltracker.FuelTrackerApp
import com.parengmark.fueltracker.data.DueStatus
import com.parengmark.fueltracker.data.FuelStatus
import com.parengmark.fueltracker.data.MaintenanceStatus
import kotlinx.coroutines.flow.first
import kotlin.math.roundToInt

/** Runs once a day (see [FuelTrackerApp]). Checks every maintenance reminder
 * and every vehicle's estimated fuel level, and posts at most one notification
 * for each. Pure read of the local database -- no network. */
class MaintenanceReminderWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val repository = (applicationContext as FuelTrackerApp).repository

        // --- Maintenance -------------------------------------------------
        val reminders = repository.getAllMaintenanceReminders()
        val odometerByVehicle = mutableMapOf<Long, Double?>()
        val dueLines = mutableListOf<String>()
        val now = System.currentTimeMillis()

        for (reminder in reminders) {
            val currentOdometer = odometerByVehicle.getOrPut(reminder.vehicleId) {
                repository.getCurrentOdometer(reminder.vehicleId)
            }
            val status = MaintenanceStatus.compute(reminder, currentOdometer, now)
            if (status == DueStatus.OVERDUE || status == DueStatus.DUE_SOON) {
                val vehicleName = repository.getVehicle(reminder.vehicleId)?.name ?: "Your vehicle"
                val suffix = if (status == DueStatus.OVERDUE) "overdue" else "due soon"
                dueLines += "$vehicleName: ${reminder.type} ($suffix)"
            }
        }
        if (dueLines.isNotEmpty()) {
            NotificationHelper.showMaintenanceNotification(applicationContext, dueLines)
        }

        // --- Fuel top-up -------------------------------------------------
        val fuelLines = mutableListOf<String>()
        for (vehicle in repository.observeVehicles().first()) {
            val estimate = repository.estimateFuel(vehicle.id) ?: continue
            if (estimate.status == FuelStatus.OK) continue
            val range = estimate.rangeLeft?.let { ", ~${it.roundToInt()} ${vehicle.distanceUnitLabel} left" } ?: ""
            val label = if (estimate.status == FuelStatus.LOW) "fill up now" else "top up soon"
            fuelLines += "${vehicle.name}: $label (about ${(estimate.fractionLeft * 100).roundToInt()}% tank$range)"
        }
        if (fuelLines.isNotEmpty()) {
            NotificationHelper.showFuelNotification(applicationContext, fuelLines)
        }

        return Result.success()
    }
}
