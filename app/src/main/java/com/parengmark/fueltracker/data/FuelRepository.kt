package com.parengmark.fueltracker.data

import kotlinx.coroutines.flow.Flow

/** Thin repository over the DAOs -- keeps ViewModels from talking to Room
 * directly, and is the seam where a future sync layer (see the spec's Phase 3)
 * would plug in without the UI changing. */
class FuelRepository(private val db: AppDatabase) {

    fun observeVehicles(): Flow<List<Vehicle>> = db.vehicleDao().observeAll()

    suspend fun getVehicle(id: Long): Vehicle? = db.vehicleDao().getById(id)

    suspend fun saveVehicle(vehicle: Vehicle): Long = db.vehicleDao().upsert(vehicle)

    suspend fun updateVehicle(vehicle: Vehicle) = db.vehicleDao().update(vehicle)

    suspend fun deleteVehicle(vehicle: Vehicle) = db.vehicleDao().delete(vehicle)

    fun observeFillUps(vehicleId: Long): Flow<List<FillUp>> =
        db.fillUpDao().observeForVehicle(vehicleId)

    suspend fun getFillUpsAscending(vehicleId: Long): List<FillUp> =
        db.fillUpDao().getForVehicleAscending(vehicleId)

    suspend fun saveFillUp(fillUp: FillUp): Long = db.fillUpDao().upsert(fillUp)

    suspend fun updateFillUp(fillUp: FillUp) = db.fillUpDao().update(fillUp)

    suspend fun deleteFillUp(fillUp: FillUp) = db.fillUpDao().delete(fillUp)

    /** The last fill-up logged for a vehicle (highest odometer), used to
     * pre-fill the "add fill-up" form and to validate the next odometer entry. */
    suspend fun getLastFillUp(vehicleId: Long): FillUp? =
        getFillUpsAscending(vehicleId).lastOrNull()

    fun observeMaintenanceReminders(vehicleId: Long): Flow<List<MaintenanceReminder>> =
        db.maintenanceReminderDao().observeForVehicle(vehicleId)

    /** Every reminder across every vehicle -- used by the background due-check worker. */
    suspend fun getAllMaintenanceReminders(): List<MaintenanceReminder> =
        db.maintenanceReminderDao().getAll()

    suspend fun saveMaintenanceReminder(reminder: MaintenanceReminder): Long =
        db.maintenanceReminderDao().upsert(reminder)

    suspend fun updateMaintenanceReminder(reminder: MaintenanceReminder) =
        db.maintenanceReminderDao().update(reminder)

    suspend fun deleteMaintenanceReminder(reminder: MaintenanceReminder) =
        db.maintenanceReminderDao().delete(reminder)

    fun observeExpenses(vehicleId: Long): Flow<List<Expense>> =
        db.expenseDao().observeForVehicle(vehicleId)

    suspend fun getExpenses(vehicleId: Long): List<Expense> =
        db.expenseDao().getForVehicle(vehicleId)

    suspend fun getExpense(id: Long): Expense? = db.expenseDao().getById(id)

    suspend fun saveExpense(expense: Expense): Long = db.expenseDao().upsert(expense)

    suspend fun deleteExpense(expense: Expense) = db.expenseDao().delete(expense)
}
