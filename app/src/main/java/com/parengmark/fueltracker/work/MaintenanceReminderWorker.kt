package com.parengmark.fueltracker.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.parengmark.fueltracker.FuelTrackerApp
import com.parengmark.fueltracker.data.DueStatus
import com.parengmark.fueltracker.data.MaintenanceStatus

/** Runs once a day (see [scheduleDailyCheck] in [FuelTrackerApp]), checks every
 * maintenance reminder across every vehicle, and posts a single notification
 * summarizing anything overdue or due soon. Pure read of the local database --
 * no network. */
class MaintenanceReminderWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val repository = (applicationContext as FuelTrackerApp).repository
        val reminders = repository.getAllMaintenanceReminders()
        if (reminders.isEmpty()) return Result.success()

        val odometerByVehicle = mutableMapOf<Long, Double?>()
        val dueLines = mutableListOf<String>()
        val now = System.currentTimeMillis()

        for (reminder in reminders) {
            val currentOdometer = odometerByVehicle.getOrPut(reminder.vehicleId) {
                repository.getLastFillUp(reminder.vehicleId)?.odometer
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
        return Result.success()
    }
}
