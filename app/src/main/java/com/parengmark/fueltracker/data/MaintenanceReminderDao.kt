package com.parengmark.fueltracker.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface MaintenanceReminderDao {
    @Query("SELECT * FROM maintenance_reminders WHERE vehicleId = :vehicleId ORDER BY createdAtEpochMillis ASC")
    fun observeForVehicle(vehicleId: Long): Flow<List<MaintenanceReminder>>

    @Query("SELECT * FROM maintenance_reminders")
    suspend fun getAll(): List<MaintenanceReminder>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(reminder: MaintenanceReminder): Long

    @Update
    suspend fun update(reminder: MaintenanceReminder)

    @Delete
    suspend fun delete(reminder: MaintenanceReminder)
}
