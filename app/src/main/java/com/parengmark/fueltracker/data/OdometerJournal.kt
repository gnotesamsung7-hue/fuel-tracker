package com.parengmark.fueltracker.data

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** One row in the daily odometer journal. */
data class JournalDay(
    val date: LocalDate,
    /** Check-ins and fill-ups that day, newest first. */
    val checkIns: List<OdometerLog>,
    val fillUps: List<FillUp>,
    val firstOdometer: Double,
    val lastOdometer: Double,
    /** Distance since the last reading on an earlier day (or within the day
     * when this is the very first day logged). */
    val distance: Double
)

object OdometerJournal {

    fun build(
        logs: List<OdometerLog>,
        fillUps: List<FillUp>,
        zone: ZoneId = ZoneId.systemDefault()
    ): List<JournalDay> {
        fun dayOf(millis: Long): LocalDate = Instant.ofEpochMilli(millis).atZone(zone).toLocalDate()

        val logsByDay = logs.groupBy { dayOf(it.timestampMillis) }
        val fillUpsByDay = fillUps.groupBy { dayOf(it.dateEpochMillis) }
        val days = (logsByDay.keys + fillUpsByDay.keys).sorted()

        var highestBefore: Double? = null
        val result = mutableListOf<JournalDay>()
        for (day in days) {
            val dayLogs = logsByDay[day].orEmpty()
            val dayFillUps = fillUpsByDay[day].orEmpty()
            val readings = dayLogs.map { it.odometer } + dayFillUps.map { it.odometer }
            val first = readings.min()
            val last = readings.max()
            val from = highestBefore ?: first
            result += JournalDay(
                date = day,
                checkIns = dayLogs.sortedByDescending { it.timestampMillis },
                fillUps = dayFillUps.sortedByDescending { it.odometer },
                firstOdometer = first,
                lastOdometer = last,
                distance = (last - from).coerceAtLeast(0.0)
            )
            highestBefore = maxOf(highestBefore ?: last, last)
        }
        return result.reversed()
    }

    /** Total distance over the days on or after [since]. */
    fun distanceSince(days: List<JournalDay>, since: LocalDate): Double =
        days.filter { !it.date.isBefore(since) }.sumOf { it.distance }
}
