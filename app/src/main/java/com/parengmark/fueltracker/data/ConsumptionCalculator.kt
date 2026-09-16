package com.parengmark.fueltracker.data

/** One full-tank-to-full-tank span with its computed economy. */
data class EconomySegment(
    val fromOdometer: Double,
    val toOdometer: Double,
    val distance: Double,
    val volumeUsed: Double,
    val fillUpCountInSpan: Int
) {
    /** Raw distance-per-volume ratio, e.g. km/L or mi/gal. Always in the
     * vehicle's own units -- no unit conversion happens here. */
    val distancePerVolume: Double
        get() = if (volumeUsed > 0) distance / volumeUsed else 0.0
}

/**
 * Computes real fuel economy from a list of fill-ups.
 *
 * Economy is only meaningful between two consecutive *full-tank* fill-ups: that's
 * the only way to know exactly how much fuel was burned to cover a given
 * distance. Partial fill-ups in between still contribute their volume to the
 * span's total, they just aren't calculation anchors on their own -- so a
 * missed or partial fill-up never corrupts the math, it just makes one span
 * wider.
 */
object ConsumptionCalculator {

    /** [fillUps] must belong to a single vehicle. Order doesn't matter -- this
     * sorts by odometer ascending internally. */
    fun computeSegments(fillUps: List<FillUp>): List<EconomySegment> {
        val sorted = fillUps.sortedBy { it.odometer }
        val segments = mutableListOf<EconomySegment>()

        var anchorIndex = sorted.indexOfFirst { it.fullTank }
        if (anchorIndex == -1) return emptyList()

        var i = anchorIndex + 1
        while (i < sorted.size) {
            if (sorted[i].fullTank) {
                val from = sorted[anchorIndex]
                val to = sorted[i]
                val spanVolume = sorted.subList(anchorIndex + 1, i + 1).sumOf { it.volume }
                val distance = to.odometer - from.odometer
                if (distance > 0 && spanVolume > 0) {
                    segments += EconomySegment(
                        fromOdometer = from.odometer,
                        toOdometer = to.odometer,
                        distance = distance,
                        volumeUsed = spanVolume,
                        fillUpCountInSpan = i - anchorIndex
                    )
                }
                anchorIndex = i
            }
            i++
        }
        return segments
    }

    /** Average distance-per-volume across all computed segments, or null if
     * there isn't yet at least one full-tank-to-full-tank span logged. */
    fun averageDistancePerVolume(fillUps: List<FillUp>): Double? {
        val segments = computeSegments(fillUps)
        if (segments.isEmpty()) return null
        val totalDistance = segments.sumOf { it.distance }
        val totalVolume = segments.sumOf { it.volumeUsed }
        return if (totalVolume > 0) totalDistance / totalVolume else null
    }

    /** Formats a raw distance/volume ratio for display, given the vehicle's
     * chosen units. */
    fun formatEconomy(distancePerVolume: Double?, vehicle: Vehicle): String {
        if (distancePerVolume == null || distancePerVolume <= 0.0) return "--"
        return when (vehicle.unitSystem) {
            UnitSystem.IMPERIAL -> "%.1f MPG".format(distancePerVolume)
            UnitSystem.METRIC -> when (vehicle.metricEconomyDisplay) {
                MetricEconomyDisplay.KM_PER_L -> "%.1f km/L".format(distancePerVolume)
                MetricEconomyDisplay.L_PER_100KM -> {
                    val lPer100 = 100.0 / distancePerVolume
                    "%.1f L/100km".format(lPer100)
                }
            }
        }
    }

    /** Same ratio as [formatEconomy] but as a plain Double, for charting --
     * always "bigger is better" in the vehicle's chosen unit (km/L or MPG),
     * or L/100km (where smaller is better) when that's what the vehicle uses. */
    fun displayValue(distancePerVolume: Double, vehicle: Vehicle): Double = when (vehicle.unitSystem) {
        UnitSystem.IMPERIAL -> distancePerVolume
        UnitSystem.METRIC -> when (vehicle.metricEconomyDisplay) {
            MetricEconomyDisplay.KM_PER_L -> distancePerVolume
            MetricEconomyDisplay.L_PER_100KM -> if (distancePerVolume > 0) 100.0 / distancePerVolume else 0.0
        }
    }

    fun unitSuffix(vehicle: Vehicle): String = when (vehicle.unitSystem) {
        UnitSystem.IMPERIAL -> "MPG"
        UnitSystem.METRIC -> when (vehicle.metricEconomyDisplay) {
            MetricEconomyDisplay.KM_PER_L -> "km/L"
            MetricEconomyDisplay.L_PER_100KM -> "L/100km"
        }
    }

    /** Total cost per unit distance over the given fill-ups (fuel cost only). */
    fun costPerDistance(fillUps: List<FillUp>): Double? {
        val sorted = fillUps.sortedBy { it.odometer }
        if (sorted.size < 2) return null
        val distance = sorted.last().odometer - sorted.first().odometer
        if (distance <= 0) return null
        val totalCost = sorted.sumOf { it.totalCost }
        return totalCost / distance
    }
}
