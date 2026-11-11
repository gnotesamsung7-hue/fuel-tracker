package com.parengmark.fueltracker.data

/** Latest pump price and how it moved since the previous fill-up of the same grade. */
data class PriceWatch(
    val latestPrice: Double,
    val grade: String?,
    /** Positive = more expensive than last time. Null when there's no earlier fill-up to compare. */
    val change: Double?,
    /** Oldest to newest, same grade when known; for the sparkline. */
    val history: List<Double>,
    val lowest: Double,
    val highest: Double
)

object PriceWatchCalculator {
    fun compute(fillUps: List<FillUp>, maxPoints: Int = 12): PriceWatch? {
        val byDate = fillUps.filter { it.volume > 0 && it.totalCost > 0 }.sortedBy { it.dateEpochMillis }
        val latest = byDate.lastOrNull() ?: return null
        // Compare like with like: same octane, if the latest one has a grade.
        val sameGrade = if (latest.fuelGrade != null) byDate.filter { it.fuelGrade == latest.fuelGrade } else byDate
        val prices = sameGrade.map { it.pricePerUnit }
        val previous = prices.dropLast(1).lastOrNull()
        val history = prices.takeLast(maxPoints)
        return PriceWatch(
            latestPrice = latest.pricePerUnit,
            grade = latest.fuelGrade,
            change = previous?.let { latest.pricePerUnit - it },
            history = history,
            lowest = prices.min(),
            highest = prices.max()
        )
    }
}
