package com.parengmark.fueltracker.data

import androidx.room.TypeConverter

/** Explicit String&lt;-&gt;enum converters, rather than relying on Room's native
 * enum support, so this compiles the same way across Room versions. */
class Converters {
    @TypeConverter
    fun fromUnitSystem(value: UnitSystem): String = value.name

    @TypeConverter
    fun toUnitSystem(value: String): UnitSystem =
        runCatching { UnitSystem.valueOf(value) }.getOrDefault(UnitSystem.METRIC)

    @TypeConverter
    fun fromMetricEconomyDisplay(value: MetricEconomyDisplay): String = value.name

    @TypeConverter
    fun toMetricEconomyDisplay(value: String): MetricEconomyDisplay =
        runCatching { MetricEconomyDisplay.valueOf(value) }.getOrDefault(MetricEconomyDisplay.KM_PER_L)
}
