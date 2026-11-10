package com.parengmark.fueltracker.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface OdometerLogDao {
    @Query("SELECT * FROM odometer_logs WHERE vehicleId = :vehicleId ORDER BY timestampMillis DESC, id DESC")
    fun observeForVehicle(vehicleId: Long): Flow<List<OdometerLog>>

    @Query("SELECT * FROM odometer_logs WHERE vehicleId = :vehicleId ORDER BY timestampMillis DESC, id DESC")
    suspend fun getForVehicle(vehicleId: Long): List<OdometerLog>

    @Query("SELECT MAX(odometer) FROM odometer_logs WHERE vehicleId = :vehicleId")
    suspend fun getMaxOdometer(vehicleId: Long): Double?

    @Insert
    suspend fun insert(log: OdometerLog): Long

    @Delete
    suspend fun delete(log: OdometerLog)
}
