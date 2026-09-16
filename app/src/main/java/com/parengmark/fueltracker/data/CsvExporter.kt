package com.parengmark.fueltracker.data

import android.content.Context
import androidx.core.content.FileProvider
import android.net.Uri
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale

/** Writes a vehicle's fill-up log to a CSV file under the app's cache dir and
 * returns a content:// Uri suitable for a share-sheet intent. Kept dependency-free
 * (no Apache POI) since CSV needs no library -- see the spec's Phase 3 for
 * Excel/PDF export once cloud sync/accounts exist. */
object CsvExporter {

    fun export(context: Context, vehicle: Vehicle, fillUps: List<FillUp>): Uri {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val sb = StringBuilder()
        sb.append("Date,Odometer (${vehicle.distanceUnitLabel}),Volume (${vehicle.volumeUnitLabel}),")
            .append("Total Cost (${vehicle.currencyCode}),Price per ${vehicle.volumeUnitLabel},")
            .append("Full Tank,Station,Notes\n")

        fillUps.sortedByDescending { it.odometer }.forEach { f ->
            sb.append(dateFormat.format(f.dateEpochMillis)).append(',')
            sb.append(f.odometer).append(',')
            sb.append(f.volume).append(',')
            sb.append(f.totalCost).append(',')
            sb.append("%.3f".format(f.pricePerUnit)).append(',')
            sb.append(if (f.fullTank) "yes" else "no").append(',')
            sb.append(csvEscape(f.stationName)).append(',')
            sb.append(csvEscape(f.notes)).append('\n')
        }

        val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val safeName = vehicle.name.replace(Regex("[^A-Za-z0-9_-]"), "_")
        val file = File(exportDir, "fuel-log-$safeName.csv")
        file.writeText(sb.toString())

        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
    }

    private fun csvEscape(value: String?): String {
        if (value.isNullOrEmpty()) return ""
        return if (value.contains(',') || value.contains('"') || value.contains('\n')) {
            "\"" + value.replace("\"", "\"\"") + "\""
        } else value
    }
}
