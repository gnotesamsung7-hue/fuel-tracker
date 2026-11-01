package com.parengmark.fueltracker.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** What a non-fuel expense was for. Stored by [name] (see [Converters]), so the
 * labels can be reworded later without touching anyone's saved data. */
enum class ExpenseCategory(val label: String) {
    MAINTENANCE("Maintenance / PMS"),
    OIL_CHANGE("Oil change"),
    REPAIR("Repair"),
    TIRES("Tires & battery"),
    TOLL("Toll"),
    PARKING("Parking"),
    INSURANCE("Insurance"),
    REGISTRATION("Registration"),
    CAR_WASH("Car wash"),
    OTHER("Other");

    companion object {
        /** Best-guess category for a maintenance reminder's free-text type, used
         * when the user logs the cost of a reminder they just completed. */
        fun guessFrom(text: String?): ExpenseCategory {
            val t = text?.lowercase() ?: return MAINTENANCE
            return when {
                "oil" in t -> OIL_CHANGE
                "tire" in t || "tyre" in t || "battery" in t -> TIRES
                "regist" in t || "lto" in t -> REGISTRATION
                "insur" in t -> INSURANCE
                "wash" in t || "detail" in t -> CAR_WASH
                "repair" in t || "fix" in t -> REPAIR
                else -> MAINTENANCE
            }
        }
    }
}

/**
 * Any vehicle cost that isn't a fuel fill-up: repairs, tolls, parking,
 * insurance, registration, and so on. Fill-ups stay in their own table because
 * the fuel-economy math depends on their volume/odometer/full-tank fields;
 * dashboards and exports add the two together for total cost of ownership.
 */
@Entity(
    tableName = "expenses",
    foreignKeys = [
        ForeignKey(
            entity = Vehicle::class,
            parentColumns = ["id"],
            childColumns = ["vehicleId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("vehicleId")]
)
data class Expense(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val vehicleId: Long,
    val dateEpochMillis: Long,
    val category: ExpenseCategory,
    val amount: Double,
    /** Optional, in the vehicle's distance unit. */
    val odometer: Double? = null,
    val vendor: String? = null,
    val notes: String? = null
)
