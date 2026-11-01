package com.parengmark.fueltracker.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseDao {
    @Query("SELECT * FROM expenses WHERE vehicleId = :vehicleId ORDER BY dateEpochMillis DESC, id DESC")
    fun observeForVehicle(vehicleId: Long): Flow<List<Expense>>

    @Query("SELECT * FROM expenses WHERE vehicleId = :vehicleId ORDER BY dateEpochMillis DESC, id DESC")
    suspend fun getForVehicle(vehicleId: Long): List<Expense>

    @Query("SELECT * FROM expenses WHERE id = :id")
    suspend fun getById(id: Long): Expense?

    /** Insert or, when [Expense.id] is set, replace -- used for both add and edit. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(expense: Expense): Long

    @Delete
    suspend fun delete(expense: Expense)
}
