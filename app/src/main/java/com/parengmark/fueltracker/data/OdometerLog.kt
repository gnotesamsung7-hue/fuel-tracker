package com.parengmark.fueltracker.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * An engine-start check-in: the odometer reading (and, optionally, where the
 * fuel gauge sits) each time the user gets in and starts the car. These feed
 * the daily odometer journal and the "when to top up" estimate.
 */
@Entity(
    tableName = "odometer_logs",
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
data class OdometerLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val vehicleId: Long,
    val timestampMillis: Long,
    /** In the vehicle's distance unit. */
    val odometer: Double,
    /** Fuel gauge in eighths of a tank: 0 = E ... 8 = F. Null = not read. */
    val fuelEighths: Int? = null,
    val purpose: String? = null,
    val note: String? = null
)

object FuelGauge {
    private val labels = listOf("E", "1/8", "1/4", "3/8", "1/2", "5/8", "3/4", "7/8", "F")

    fun label(eighths: Int): String = labels[eighths.coerceIn(0, 8)]

    /** Nearest gauge mark for a fraction of a tank. */
    fun nearestEighths(fraction: Double): Int = Math.round(fraction * 8).toInt().coerceIn(0, 8)

    val purposes = listOf("Work", "Errand", "School run", "Long drive", "Other")
}
