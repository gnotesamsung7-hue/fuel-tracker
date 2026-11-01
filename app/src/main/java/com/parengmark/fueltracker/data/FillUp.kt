package com.parengmark.fueltracker.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A single fuel purchase/fill-up.
 *
 * [odometer] and [volume] are stored in whatever unit the parent [Vehicle] uses
 * (km/L or mi/gal) -- see [Vehicle.unitSystem]. No conversion happens at this layer.
 *
 * [fullTank] matters a lot: real fuel-economy math only works between two
 * consecutive full-tank fill-ups (see [ConsumptionCalculator]). A partial
 * fill-up is still logged as an expense and still counts its volume toward the
 * next full-tank-to-full-tank calculation, it just isn't a calculation anchor
 * on its own.
 */
@Entity(
    tableName = "fillups",
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
data class FillUp(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val vehicleId: Long,
    val dateEpochMillis: Long,
    val odometer: Double,
    val volume: Double,
    val totalCost: Double,
    val fullTank: Boolean = true,
    val stationName: String? = null,
    val notes: String? = null,
    /** Octane/fuel grade as picked on the form, e.g. "RON 95" or "Diesel".
     * Null for fill-ups logged before v0.5 or when left unset. */
    val fuelGrade: String? = null
) {
    /** Derived, not stored -- always recomputed from volume/cost so the two can
     * never drift apart from rounding on save. */
    val pricePerUnit: Double
        get() = if (volume > 0) totalCost / volume else 0.0
}

/** Fuel grades offered as one-tap chips on the add fill-up form. Stored as the
 * [stored] text so new grades can be added later without a migration. */
object FuelGrades {
    data class Grade(val stored: String, val chip: String)

    val all = listOf(
        Grade("RON 91", "91 Regular"),
        Grade("RON 95", "95 Premium"),
        Grade("RON 97", "97 Super"),
        Grade("RON 100", "100"),
        Grade("Diesel", "Diesel")
    )
}
