package com.parengmark.fueltracker.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Whole-app unit system for a vehicle: picks distance unit, volume unit, and
 * the natural economy unit together, so no cross-unit conversion is needed
 * anywhere else in the app for v1. */
enum class UnitSystem {
    METRIC,   // km, liters
    IMPERIAL  // miles, US gallons
}

/** Only meaningful when unitSystem == METRIC (imperial vehicles always show MPG). */
enum class MetricEconomyDisplay {
    KM_PER_L,
    L_PER_100KM
}

@Entity(tableName = "vehicles")
data class Vehicle(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val unitSystem: UnitSystem = UnitSystem.METRIC,
    val metricEconomyDisplay: MetricEconomyDisplay = MetricEconomyDisplay.KM_PER_L,
    val currencyCode: String = "PHP",
    val createdAtEpochMillis: Long = System.currentTimeMillis(),
    /** The user's pixel drawing of this car from the Art studio, encoded by
     * [com.parengmark.fueltracker.ui.art.PixelArt]. Null = nothing drawn yet. */
    val pixelArt: String? = null,
    /** Fuel tank size in the vehicle's volume unit -- needed for the
     * "when to top up" estimate. Null until the user sets it. */
    val tankCapacity: Double? = null
) {
    val distanceUnitLabel: String get() = if (unitSystem == UnitSystem.METRIC) "km" else "mi"
    val volumeUnitLabel: String get() = if (unitSystem == UnitSystem.METRIC) "L" else "gal"
}
