package com.parengmark.fueltracker.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [Vehicle::class, FillUp::class, MaintenanceReminder::class, Expense::class],
    version = 3,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun vehicleDao(): VehicleDao
    abstract fun fillUpDao(): FillUpDao
    abstract fun maintenanceReminderDao(): MaintenanceReminderDao
    abstract fun expenseDao(): ExpenseDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        /** v2 -> v3 adds the expenses table. A real migration (rather than
         * wiping the database) so the fill-ups, vehicles and reminders already
         * logged on a phone survive the update. The SQL mirrors exactly what
         * Room generates for [Expense], which Room checks on first open. */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `expenses` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`vehicleId` INTEGER NOT NULL, " +
                        "`dateEpochMillis` INTEGER NOT NULL, " +
                        "`category` TEXT NOT NULL, " +
                        "`amount` REAL NOT NULL, " +
                        "`odometer` REAL, " +
                        "`vendor` TEXT, " +
                        "`notes` TEXT, " +
                        "FOREIGN KEY(`vehicleId`) REFERENCES `vehicles`(`id`) " +
                        "ON UPDATE NO ACTION ON DELETE CASCADE)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_expenses_vehicleId` ON `expenses` (`vehicleId`)"
                )
            }
        }

        fun getInstance(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "fuel-tracker.db"
                )
                    .addMigrations(MIGRATION_2_3)
                    // Only used for a version jump with no migration path
                    // (e.g. a v1 test install). v2 -> v3 keeps your data.
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
    }
}
