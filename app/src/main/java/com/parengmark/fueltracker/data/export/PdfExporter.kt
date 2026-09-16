package com.parengmark.fueltracker.data.export

import android.content.Context
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.core.content.FileProvider
import com.parengmark.fueltracker.data.ConsumptionCalculator
import com.parengmark.fueltracker.data.FillUp
import com.parengmark.fueltracker.data.Vehicle
import com.parengmark.fueltracker.ui.common.Formatting
import java.io.File

/** A simple templated report via Android's built-in PdfDocument canvas API --
 * no external dependency. Good enough for "hand this to someone as a fuel/
 * expense report", which is the realistic use case (see the spec's §3.3). */
object PdfExporter {
    private const val PAGE_WIDTH = 595 // A4 @ 72dpi, in points
    private const val PAGE_HEIGHT = 842
    private const val MARGIN = 40f
    private const val LINE_HEIGHT = 18f

    fun export(context: Context, vehicle: Vehicle, fillUps: List<FillUp>): Uri {
        val document = PdfDocument()
        val titlePaint = Paint().apply { textSize = 18f; isFakeBoldText = true }
        val headerPaint = Paint().apply { textSize = 11f; isFakeBoldText = true }
        val bodyPaint = Paint().apply { textSize = 10f }

        val sorted = fillUps.sortedByDescending { it.odometer }
        val avgEconomy = ConsumptionCalculator.averageDistancePerVolume(fillUps)

        var pageNumber = 1
        var page = document.startPage(
            PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
        )
        var canvas = page.canvas
        var y = MARGIN

        fun newPageIfNeeded() {
            if (y > PAGE_HEIGHT - MARGIN) {
                document.finishPage(page)
                pageNumber++
                page = document.startPage(
                    PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
                )
                canvas = page.canvas
                y = MARGIN
            }
        }

        canvas.drawText("Fuel log — ${vehicle.name}", MARGIN, y, titlePaint)
        y += LINE_HEIGHT * 1.5f
        canvas.drawText(
            "Average economy: ${ConsumptionCalculator.formatEconomy(avgEconomy, vehicle)}",
            MARGIN, y, bodyPaint
        )
        y += LINE_HEIGHT
        canvas.drawText("${sorted.size} fill-ups logged", MARGIN, y, bodyPaint)
        y += LINE_HEIGHT * 1.5f

        val col1 = MARGIN
        val col2 = MARGIN + 90f
        val col3 = MARGIN + 190f
        val col4 = MARGIN + 270f
        val col5 = MARGIN + 360f

        canvas.drawText("Date", col1, y, headerPaint)
        canvas.drawText("Odometer", col2, y, headerPaint)
        canvas.drawText("Volume", col3, y, headerPaint)
        canvas.drawText("Cost", col4, y, headerPaint)
        canvas.drawText("Station", col5, y, headerPaint)
        y += LINE_HEIGHT

        for (f in sorted) {
            newPageIfNeeded()
            canvas.drawText(Formatting.date(f.dateEpochMillis), col1, y, bodyPaint)
            canvas.drawText(f.odometer.toString(), col2, y, bodyPaint)
            canvas.drawText(f.volume.toString(), col3, y, bodyPaint)
            canvas.drawText(Formatting.money(f.totalCost, vehicle.currencyCode), col4, y, bodyPaint)
            canvas.drawText((f.stationName ?: "").take(24), col5, y, bodyPaint)
            y += LINE_HEIGHT
        }

        document.finishPage(page)

        val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val safeName = vehicle.name.replace(Regex("[^A-Za-z0-9_-]"), "_")
        val file = File(exportDir, "fuel-log-$safeName.pdf")
        file.outputStream().use { document.writeTo(it) }
        document.close()

        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }
}
