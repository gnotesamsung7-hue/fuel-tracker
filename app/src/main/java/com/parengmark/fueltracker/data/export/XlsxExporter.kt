package com.parengmark.fueltracker.data.export

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import com.parengmark.fueltracker.data.FillUp
import com.parengmark.fueltracker.data.Vehicle
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale

object XlsxExporter {

    fun export(context: Context, vehicle: Vehicle, fillUps: List<FillUp>): Uri {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val headers = listOf(
            "Date",
            "Odometer (${vehicle.distanceUnitLabel})",
            "Volume (${vehicle.volumeUnitLabel})",
            "Total Cost (${vehicle.currencyCode})",
            "Price per ${vehicle.volumeUnitLabel}",
            "Full Tank",
            "Station",
            "Notes"
        )
        val rows: List<List<Any?>> = fillUps.sortedByDescending { it.odometer }.map { f ->
            listOf(
                dateFormat.format(f.dateEpochMillis),
                f.odometer,
                f.volume,
                f.totalCost,
                Math.round(f.pricePerUnit * 1000) / 1000.0,
                f.fullTank,
                f.stationName ?: "",
                f.notes ?: ""
            )
        }

        val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val safeName = vehicle.name.replace(Regex("[^A-Za-z0-9_-]"), "_")
        val file = File(exportDir, "fuel-log-$safeName.xlsx")
        file.outputStream().use { out ->
            XlsxWriter.write(out, sheetName = "Fuel Log", headers = headers, rows = rows)
        }

        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }
}
