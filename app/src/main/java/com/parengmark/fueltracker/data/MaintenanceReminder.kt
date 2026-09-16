package com.parengmark.fueltracker.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A recurring maintenance item (oil change, tire rotation, registration, ...)
 * for a vehicle. Due when EITHER the distance interval or the time interval
 * elapses, whichever comes first -- see [DueStatus.compute].
 *
 * v1 reminders are completed manually ("mark done" resets the anchor point)
 * rather than being tied to a full Expense ledger entry -- see the spec's
 * Phase 3 for the unified Expense model this could later hook into.
 */
@Entity(
    tableName = "maintenance_reminders",
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
data class MaintenanceReminder(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val vehicleId: Long,
    val type: String,
    /** In the vehicle's distance unit (km or mi). Null = no distance-based check. */
    val intervalDistance: Double? = null,
    /** Null = no time-based check. */
    val intervalDays: Int? = null,
    val lastDoneOdometer: Double? = null,
    val lastDoneDateMillis: Long? = null,
    val createdAtEpochMillis: Long = System.currentTimeMillis()
) {
    val nextDueOdometer: Double?
        get() = if (intervalDistance != null && lastDoneOdometer != null) {
            lastDoneOdometer + intervalDistance
        } else null

    val nextDueDateMillis: Long?
        get() = if (intervalDays != null && lastDoneDateMillis != null) {
            lastDoneDateMillis + intervalDays.toLong() * 86_400_000L
        } else null
}

enum class DueStatus { OVERDUE, DUE_SOON, OK, UNKNOWN }

object MaintenanceStatus {
    private const val DUE_SOON_DISTANCE_FRACTION = 0.1 // within last 10% of the interval
    private const val DUE_SOON_DAYS = 14L

    fun compute(reminder: MaintenanceReminder, currentOdometer: Double?, nowMillis: Long): DueStatus {
        val dueOdo = reminder.nextDueOdometer
        val dueDate = reminder.nextDueDateMillis

        if (dueOdo == null && dueDate == null) return DueStatus.UNKNOWN

        var status = DueStatus.OK

        if (dueOdo != null && currentOdometer != null) {
            val remaining = dueOdo - currentOdometer
            status = when {
                remaining <= 0 -> DueStatus.OVERDUE
                reminder.intervalDistance != null &&
                    remaining <= reminder.intervalDistance * DUE_SOON_DISTANCE_FRACTION -> DueStatus.DUE_SOON
                else -> status
            }
        }

        if (dueDate != null && status != DueStatus.OVERDUE) {
            val remainingDays = (dueDate - nowMillis) / 86_400_000L
            status = when {
                remainingDays <= 0 -> DueStatus.OVERDUE
                remainingDays <= DUE_SOON_DAYS && status == DueStatus.OK -> DueStatus.DUE_SOON
                else -> status
            }
        }

        return status
    }
}
