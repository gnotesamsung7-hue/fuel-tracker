package com.parengmark.fueltracker.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [Vehicle::class, FillUp::class, MaintenanceReminder::class],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun vehicleDao(): VehicleDao
    abstract fun fillUpDao(): FillUpDao
    abstract fun maintenanceReminderDao(): MaintenanceReminderDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "fuel-tracker.db"
                )
                    // Pre-launch app, no real installs to preserve yet -- fine to
                    // rebuild the DB on a schema bump. Replace with a real
                    // Migration once this ships (see the spec's Phase 3 notes on
                    // not losing users' local data on upgrade).
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
    }
}
