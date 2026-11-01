package com.parengmark.fueltracker.ui.art

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

/** The little pump attendant on the add fill-up button: 16x16, with a fuel
 * drop that drips from the nozzle on a slow loop. */
private object PumpBuddy {
    const val SIZE = 16

    private val colors = mapOf(
        'k' to Color(0xFF222222), // outline, nozzle handle, shoes
        'h' to Color(0xFF3E2723), // hair
        's' to Color(0xFFF1C27D), // skin
        'r' to Color(0xFFD32F2F), // cap and shirt
        'R' to Color(0xFF9A1F1F), // cap shade
        'n' to Color(0xFF1A3A6B), // pants
        'w' to Color(0xFFFFFFFF), // name badge
        'm' to Color(0xFF9E9E9E), // nozzle spout
        'g' to Color(0xFF5D5D5D), // hose
        'b' to Color(0xFFFFB300)  // fuel drop
    )
    val palette: List<Color> = colors.values.toList()
    private val keys: List<Char> = colors.keys.toList()

    private val base = listOf(
        "................",
        "....RRRRR.......",
        "...RrrrrrR......",
        "...RrrrrrRRR....",
        "...hhsssss......",
        "...hssskss......",
        "....sssss.......",
        ".....sss........",
        "...rrrrrrsss....",
        "..rrrrrrr.sskkm.",
        "..rrwrrrr...kk.m",
        "..rrrrrrr...g...",
        "...nnnnn....g...",
        "...nn.nn....g...",
        "...nn.nn....g...",
        "..kkk.kkk...ggg."
    )

    private fun frame(dropRow: Int?): List<Int> {
        val rows = base.map { it.toCharArray() }
        if (dropRow != null) rows[dropRow][15] = 'b'
        return rows.flatMap { row -> row.map { c -> if (c == '.') -1 else keys.indexOf(c) } }
    }

    /** No drop, drop forming, drop falling. */
    val frames: List<List<Int>> = listOf(frame(null), frame(12), frame(14))
}

@Composable
fun PumpBuddyFab(onClick: () -> Unit, modifier: Modifier = Modifier) {
    var frameIndex by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(if (frameIndex == 0) 1400L else 260L)
            frameIndex = (frameIndex + 1) % PumpBuddy.frames.size
        }
    }

    FloatingActionButton(
        onClick = onClick,
        modifier = modifier
            .size(72.dp)
            .semantics { contentDescription = "Add fill-up" }
    ) {
        Box(Modifier.size(72.dp)) {
            PixelArtImage(
                cells = PumpBuddy.frames[frameIndex],
                width = PumpBuddy.SIZE,
                height = PumpBuddy.SIZE,
                colorFor = { PumpBuddy.palette[it] },
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(52.dp)
            )
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .padding(5.dp)
                    .size(20.dp)
                    .background(MaterialTheme.colorScheme.primary, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "+",
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
