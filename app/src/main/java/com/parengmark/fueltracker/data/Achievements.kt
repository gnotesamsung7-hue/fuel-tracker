package com.parengmark.fueltracker.data

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

data class Achievement(
    val id: String,
    val title: String,
    val description: String,
    val unlocked: Boolean,
    /** e.g. "640 / 1,000 km" while locked; null when there's nothing to count. */
    val progress: String? = null,
    /** 0..1 for the progress bar. */
    val fraction: Float = if (unlocked) 1f else 0f
)

/** Badges worked out from what's already logged -- nothing extra is stored. */
object Achievements {

    fun compute(
        vehicle: Vehicle,
        fillUps: List<FillUp>,
        logs: List<OdometerLog>,
        expenses: List<Expense>,
        zone: ZoneId = ZoneId.systemDefault()
    ): List<Achievement> {
        val unit = vehicle.distanceUnitLabel
        val list = mutableListOf<Achievement>()

        fun count(id: String, title: String, desc: String, have: Int, need: Int, noun: String) {
            list += Achievement(
                id, title, desc,
                unlocked = have >= need,
                progress = if (have >= need) null else "%,d / %,d %s".format(have, need, noun),
                fraction = (have.toFloat() / need).coerceIn(0f, 1f)
            )
        }

        count("first_drop", "First Drop", "Log your first fill-up.", fillUps.size, 1, "fill-ups")
        count("regular", "Pump Regular", "Log 10 fill-ups.", fillUps.size, 10, "fill-ups")

        // Distance tracked across fill-ups and check-ins.
        val readings = fillUps.map { it.odometer } + logs.map { it.odometer }
        val tracked = if (readings.size >= 2) (readings.max() - readings.min()).toInt() else 0
        count("club_1k", "1,000 Club", "Track 1,000 $unit.", tracked, 1_000, unit)
        count("club_5k", "5,000 Club", "Track 5,000 $unit.", tracked, 5_000, unit)
        count("club_10k", "Road Warrior", "Track 10,000 $unit.", tracked, 10_000, unit)

        // Check-in streak: consecutive days with at least one check-in.
        val days = logs.map { Instant.ofEpochMilli(it.timestampMillis).atZone(zone).toLocalDate() }.toSortedSet()
        count("streak_7", "Seven-Day Streak", "Check in on 7 days in a row.", longestStreak(days), 7, "days")

        count(
            "gauge_guru", "Gauge Guru", "Set the fuel gauge on 10 check-ins.",
            logs.count { it.fuelEighths != null }, 10, "check-ins"
        )

        val earlyBird = logs.any {
            Instant.ofEpochMilli(it.timestampMillis).atZone(zone).hour < 6
        }
        list += Achievement("early_bird", "Early Bird", "Check in before 6 AM.", earlyBird)

        // Eco driver: one tank at least 10% better than your average.
        val segments = ConsumptionCalculator.computeSegments(fillUps)
        val avg = ConsumptionCalculator.averageDistancePerVolume(fillUps)
        val best = segments.maxOfOrNull { it.distancePerVolume }
        val eco = avg != null && best != null && segments.size >= 2 && best >= avg * 1.10
        list += Achievement("eco", "Eco Driver", "Get one tank 10% better than your average.", eco)

        // Price hunter: paid less per unit than the fill-up before.
        val byDate = fillUps.sortedBy { it.dateEpochMillis }
        val cheaper = byDate.zipWithNext().any { (a, b) -> b.pricePerUnit < a.pricePerUnit - 0.005 }
        list += Achievement("price_hunter", "Price Hunter", "Pay less per liter than last time.", cheaper)

        val fullRun = byDate.fold(0 to 0) { (run, bestRun), f ->
            val r = if (f.fullTank) run + 1 else 0
            r to maxOf(bestRun, r)
        }.second
        count("full_send", "Full Send", "Five full-tank fill-ups in a row.", fullRun, 5, "in a row")

        val categories = expenses.map { it.category }.toSet().size
        count("budget_boss", "Budget Boss", "Log expenses in 3 different categories.", categories, 3, "categories")

        list += Achievement("artist", "Garage Artist", "Draw your car in the Art studio.", vehicle.pixelArt != null)

        return list
    }

    fun longestStreak(days: Collection<LocalDate>): Int {
        var best = 0
        var run = 0
        var prev: LocalDate? = null
        for (d in days.sorted()) {
            run = if (prev != null && d == prev.plusDays(1)) run + 1 else 1
            best = maxOf(best, run)
            prev = d
        }
        return best
    }
}
