package com.parengmark.fueltracker.data

enum class FuelStatus { OK, TOP_UP_SOON, LOW }

data class FuelEstimate(
    /** Estimated fuel in the tank, in the vehicle's volume unit. */
    val volumeLeft: Double,
    val fractionLeft: Double,
    /** Distance left at the average economy; null until economy is known. */
    val rangeLeft: Double?,
    val status: FuelStatus
)

/**
 * Works out how much fuel is in the tank right now by replaying the fuel log
 * and engine check-ins in odometer order:
 *  - a full-tank fill-up sets the level to the tank size,
 *  - a partial fill-up adds its volume,
 *  - a check-in with a gauge reading resets the level to what the gauge shows,
 *  - distance driven between them burns fuel at the average economy.
 * Returns null when there's nothing to anchor on yet (no tank size, or no
 * full fill-up / gauge reading, or distance driven before economy is known).
 */
object FuelEstimator {
    const val TOP_UP_SOON_FRACTION = 0.25
    const val LOW_FRACTION = 0.125

    private class Event(val odometer: Double, val time: Long, val apply: (Double?) -> Double?)

    fun estimate(
        tankCapacity: Double?,
        fillUps: List<FillUp>,
        logs: List<OdometerLog>,
        distancePerVolume: Double?
    ): FuelEstimate? {
        if (tankCapacity == null || tankCapacity <= 0) return null
        val economy = distancePerVolume?.takeIf { it > 0 }

        val events = buildList {
            fillUps.forEach { f ->
                add(Event(f.odometer, f.dateEpochMillis) { level ->
                    if (f.fullTank) tankCapacity
                    else level?.let { (it + f.volume).coerceAtMost(tankCapacity) }
                })
            }
            logs.forEach { l ->
                add(Event(l.odometer, l.timestampMillis) { level ->
                    l.fuelEighths?.let { tankCapacity * it / 8.0 } ?: level
                })
            }
        }.sortedWith(compareBy<Event>({ it.odometer }, { it.time }))

        if (events.isEmpty()) return null

        var level: Double? = null
        var lastOdometer = events.first().odometer
        for (e in events) {
            val driven = e.odometer - lastOdometer
            if (driven > 0 && level != null) {
                level = if (economy != null) (level - driven / economy).coerceAtLeast(0.0) else null
            }
            level = e.apply(level)
            lastOdometer = maxOf(lastOdometer, e.odometer)
        }

        val left = level ?: return null
        val fraction = (left / tankCapacity).coerceIn(0.0, 1.0)
        val status = when {
            fraction <= LOW_FRACTION -> FuelStatus.LOW
            fraction <= TOP_UP_SOON_FRACTION -> FuelStatus.TOP_UP_SOON
            else -> FuelStatus.OK
        }
        return FuelEstimate(
            volumeLeft = left,
            fractionLeft = fraction,
            rangeLeft = economy?.let { left * it },
            status = status
        )
    }
}
