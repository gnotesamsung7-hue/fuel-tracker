package com.parengmark.fueltracker

import android.app.Application
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.parengmark.fueltracker.data.AppDatabase
import com.parengmark.fueltracker.data.FuelRepository
import com.parengmark.fueltracker.work.MaintenanceReminderWorker
import com.parengmark.fueltracker.work.NotificationHelper
import java.util.concurrent.TimeUnit

/** Small manual-DI container. No Hilt/Dagger for v1 -- one Room database and
 * one repository, both cheap to wire up by hand, and it keeps the dependency
 * graph (and the risk of an annotation-processor version mismatch) minimal
 * while this is a single-module, no-network app. Revisit with Hilt if/when
 * the Phase 3 cloud-sync work (see the spec) adds real dependency depth. */
class FuelTrackerApp : Application() {
    val database: AppDatabase by lazy { AppDatabase.getInstance(this) }
    val repository: FuelRepository by lazy { FuelRepository(database) }

    override fun onCreate() {
        super.onCreate()
        NotificationHelper.ensureChannel(this)
        scheduleDailyMaintenanceCheck()
    }

    private fun scheduleDailyMaintenanceCheck() {
        val request = PeriodicWorkRequestBuilder<MaintenanceReminderWorker>(1, TimeUnit.DAYS)
            .setConstraints(Constraints.Builder().build())
            .build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "maintenance-daily-check",
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }
}
