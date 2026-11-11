package com.parengmark.fueltracker.data.stations

data class FuelStation(
    val name: String,
    val lat: Double,
    val lon: Double,
    val distanceMeters: Float,
    /** OSM "brand" tag when mapped, e.g. "Petron", "Shell". */
    val brand: String? = null,
    val openingHours: String? = null
) {
    val distanceKm: Double get() = distanceMeters / 1000.0
}
