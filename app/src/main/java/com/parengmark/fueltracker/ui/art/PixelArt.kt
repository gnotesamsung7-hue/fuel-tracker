package com.parengmark.fueltracker.ui.art

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color

/**
 * The Art studio's canvas format: a [WIDTH] x [HEIGHT] grid, one character per
 * cell, stored as a plain string on the vehicle. '.' is an empty (see-through)
 * cell and '0'..'f' pick a color from [PALETTE]. Small enough (512 chars) to
 * keep in the vehicles table with no files to manage.
 */
object PixelArt {
    const val WIDTH = 32
    const val HEIGHT = 16
    const val EMPTY = -1
    private const val CODES = "0123456789abcdef"

    val PALETTE: List<Color> = listOf(
        Color(0xFF1A1A1A), // 0 black
        Color(0xFF4A4A4A), // 1 dark gray
        Color(0xFF9E9E9E), // 2 gray
        Color(0xFFF5F5F5), // 3 white
        Color(0xFFD32F2F), // 4 red
        Color(0xFF8E1B1B), // 5 dark red
        Color(0xFFF57C00), // 6 orange
        Color(0xFFFBC02D), // 7 yellow
        Color(0xFF388E3C), // 8 green
        Color(0xFF1B5E20), // 9 dark green
        Color(0xFF4FC3F7), // a sky blue
        Color(0xFF1976D2), // b blue
        Color(0xFF0D2A6B), // c navy
        Color(0xFF6D4C41), // d brown
        Color(0xFFD7A86E), // e tan
        Color(0xFFF48FB1)  // f pink
    )

    fun blank(): IntArray = IntArray(WIDTH * HEIGHT) { EMPTY }

    fun decode(encoded: String?): IntArray {
        if (encoded == null || encoded.length != WIDTH * HEIGHT) return blank()
        return IntArray(WIDTH * HEIGHT) { i ->
            val idx = CODES.indexOf(encoded[i])
            if (idx < 0) EMPTY else idx
        }
    }

    /** Returns null for an all-empty canvas, so "cleared" means "no drawing". */
    fun encode(cells: List<Int>): String? {
        if (cells.all { it == EMPTY }) return null
        return buildString(cells.size) {
            cells.forEach { append(if (it in CODES.indices) CODES[it] else '.') }
        }
    }

    /** A simple blue sedan to start from, for anyone who'd rather recolor than draw. */
    val STARTER_CAR: String = listOf(
        "................................",
        "................................",
        "................................",
        "..........00000000000...........",
        ".........0bbbbbbbbbbb0..........",
        "........0baaaaa0aaaaab0.........",
        ".......0baaaaaa0aaaaaab0........",
        "...0000bbbbbbbbbbbbbbbbb0000....",
        "..0bbbbbbbbbbbbbbbbbbbbbbbbbb0..",
        "..077bbbbbbbbb0bbbbbbbbbbbbb440.",
        "..0bbbbbbbbbbb0bbbbbbbbbbbbbbb0.",
        "..0ccc000cccccccccccccc000cccc0.",
        "...0c01110cccccccccccc01110c00..",
        "....0122210..........0122210....",
        ".....01110............01110.....",
        "......000..............000......"
    ).joinToString("")

    /** Mirror left-right, e.g. to make the car face the other way. */
    fun flipped(cells: List<Int>): IntArray = IntArray(WIDTH * HEIGHT) { i ->
        val x = i % WIDTH
        val y = i / WIDTH
        cells[y * WIDTH + (WIDTH - 1 - x)]
    }

    /** Paint-bucket fill of the connected area (4-way) that has the same color as the tapped cell. */
    fun floodFill(cells: List<Int>, startX: Int, startY: Int, newColor: Int): IntArray {
        val out = cells.toIntArray()
        val target = out[startY * WIDTH + startX]
        if (target == newColor) return out
        val stack = ArrayDeque<Int>()
        stack.addLast(startY * WIDTH + startX)
        while (stack.isNotEmpty()) {
            val i = stack.removeLast()
            if (out[i] != target) continue
            out[i] = newColor
            val x = i % WIDTH
            val y = i / WIDTH
            if (x > 0) stack.addLast(i - 1)
            if (x < WIDTH - 1) stack.addLast(i + 1)
            if (y > 0) stack.addLast(i - WIDTH)
            if (y < HEIGHT - 1) stack.addLast(i + WIDTH)
        }
        return out
    }
}

/** Draws a pixel grid crisply (plain rectangles, no image scaling blur). */
@Composable
fun PixelArtImage(
    cells: List<Int>,
    modifier: Modifier = Modifier,
    width: Int = PixelArt.WIDTH,
    height: Int = PixelArt.HEIGHT,
    alpha: Float = 1f,
    colorFor: (Int) -> Color = { PixelArt.PALETTE[it] }
) {
    Canvas(modifier.aspectRatio(width.toFloat() / height)) {
        val cell = size.width / width
        // A hair of overlap so neighbouring cells never show a seam.
        val cellSize = Size(cell + 0.6f, cell + 0.6f)
        for (i in cells.indices) {
            val c = cells[i]
            if (c < 0) continue
            drawRect(
                color = colorFor(c),
                topLeft = Offset((i % width) * cell, (i / width) * cell),
                size = cellSize,
                alpha = alpha
            )
        }
    }
}
