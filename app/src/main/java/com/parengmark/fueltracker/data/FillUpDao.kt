package com.parengmark.fueltracker.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface FillUpDao {
    @Query("SELECT * FROM fillups WHERE vehicleId = :vehicleId ORDER BY odometer DESC, dateEpochMillis DESC")
    fun observeForVehicle(vehicleId: Long): Flow<List<FillUp>>

    @Query("SELECT * FROM fillups WHERE vehicleId = :vehicleId ORDER BY odometer ASC, dateEpochMillis ASC")
    suspend fun getForVehicleAscending(vehicleId: Long): List<FillUp>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(fillUp: FillUp): Long

    @Update
    suspend fun update(fillUp: FillUp)

    @Delete
    suspend fun delete(fillUp: FillUp)
}
