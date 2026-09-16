package com.parengmark.fueltracker.data.stations

data class FuelStation(
    val name: String,
    val lat: Double,
    val lon: Double,
    val distanceMeters: Float
) {
    val distanceKm: Double get() = distanceMeters / 1000.0
}
