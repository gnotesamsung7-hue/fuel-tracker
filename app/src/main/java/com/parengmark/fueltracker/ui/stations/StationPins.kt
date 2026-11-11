package com.parengmark.fueltracker.ui.stations

import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Typeface
import android.graphics.drawable.BitmapDrawable

/** Map pin colours by brand (common Philippine brands first), so you can
 * tell stations apart at a glance. Unknown brands get a neutral pin. */
object StationPins {
    private data class Style(val fill: Int, val text: Int, val letter: String)

    private fun styleFor(name: String, brand: String?): Style {
        val key = (brand ?: name).lowercase()
        return when {
            "petron" in key -> Style(0xFF0033A0.toInt(), 0xFFFFFFFF.toInt(), "P")
            "shell" in key -> Style(0xFFFFD500.toInt(), 0xFFDD1D21.toInt(), "S")
            "caltex" in key || "chevron" in key -> Style(0xFFE30613.toInt(), 0xFFFFFFFF.toInt(), "C")
            "phoenix" in key -> Style(0xFFF15A22.toInt(), 0xFFFFFFFF.toInt(), "Ph")
            "seaoil" in key || "sea oil" in key -> Style(0xFF0090D0.toInt(), 0xFFFFFFFF.toInt(), "Se")
            "cleanfuel" in key || "clean fuel" in key -> Style(0xFF00A651.toInt(), 0xFFFFFFFF.toInt(), "Cf")
            "unioil" in key -> Style(0xFF1B4F9C.toInt(), 0xFFFFFFFF.toInt(), "U")
            "jetti" in key -> Style(0xFF7B1FA2.toInt(), 0xFFFFFFFF.toInt(), "J")
            "total" in key -> Style(0xFFED0000.toInt(), 0xFFFFFFFF.toInt(), "T")
            "flying v" in key -> Style(0xFF2E7D32.toInt(), 0xFFFFFFFF.toInt(), "V")
            else -> Style(0xFF546E7A.toInt(), 0xFFFFFFFF.toInt(), "⛽")
        }
    }

    /** A teardrop pin with the brand's letter. [highlight] draws it larger with
     * an amber ring (selected or nearest station). */
    fun pin(resources: Resources, name: String, brand: String?, highlight: Boolean): BitmapDrawable {
        val density = resources.displayMetrics.density
        val style = styleFor(name, brand)
        val w = ((if (highlight) 44 else 34) * density).toInt()
        val h = (w * 1.3f).toInt()
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        val r = w / 2f - 2 * density
        val cx = w / 2f
        val cy = r + 2 * density

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        // Shadow-ish dark outline
        paint.color = 0x88000000.toInt()
        val tail = Path().apply {
            moveTo(cx - r * 0.55f, cy + r * 0.7f)
            lineTo(cx, h - 1f)
            lineTo(cx + r * 0.55f, cy + r * 0.7f)
            close()
        }
        c.drawPath(tail, paint)
        c.drawCircle(cx, cy, r + 1.5f * density, paint)

        paint.color = if (highlight) 0xFFFFB74D.toInt() else 0xFFFFFFFF.toInt()
        c.drawCircle(cx, cy, r, paint)
        paint.color = style.fill
        c.drawCircle(cx, cy, r - (if (highlight) 4f else 2.5f) * density, paint)

        paint.color = style.text
        paint.typeface = Typeface.DEFAULT_BOLD
        paint.textAlign = Paint.Align.CENTER
        paint.textSize = r * (if (style.letter.length > 1) 0.9f else 1.15f)
        val baseline = cy - (paint.descent() + paint.ascent()) / 2
        c.drawText(style.letter, cx, baseline, paint)

        return BitmapDrawable(resources, bmp)
    }

    /** Blue "you are here" dot. */
    fun you(resources: Resources): BitmapDrawable {
        val density = resources.displayMetrics.density
        val s = (26 * density).toInt()
        val bmp = Bitmap.createBitmap(s, s, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.color = 0x552196F3
        c.drawCircle(s / 2f, s / 2f, s / 2f, paint)
        paint.color = 0xFFFFFFFF.toInt()
        c.drawCircle(s / 2f, s / 2f, s * 0.28f, paint)
        paint.color = 0xFF2196F3.toInt()
        c.drawCircle(s / 2f, s / 2f, s * 0.21f, paint)
        return BitmapDrawable(resources, bmp)
    }
}
