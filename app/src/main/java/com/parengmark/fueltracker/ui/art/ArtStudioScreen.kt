package com.parengmark.fueltracker.ui.art

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Flip
import androidx.compose.material.icons.filled.FormatColorFill
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp

private enum class Tool { PENCIL, ERASER, FILL }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArtStudioScreen(
    viewModel: ArtStudioViewModel,
    vehicleId: Long,
    onDone: () -> Unit,
    onBack: () -> Unit
) {
    LaunchedEffect(vehicleId) { viewModel.load(vehicleId) }
    val vehicle by viewModel.vehicle.collectAsState()

    val cells = remember { mutableStateListOf<Int>().apply { addAll(PixelArt.blank().toList()) } }
    val history = remember { mutableStateListOf<IntArray>() }
    var loaded by remember { mutableStateOf(false) }
    var tool by remember { mutableStateOf(Tool.PENCIL) }
    var colorIndex by remember { mutableIntStateOf(11) } // blue

    // Load the saved drawing once.
    LaunchedEffect(vehicle?.id) {
        val v = vehicle ?: return@LaunchedEffect
        if (loaded) return@LaunchedEffect
        val saved = PixelArt.decode(v.pixelArt)
        for (i in saved.indices) cells[i] = saved[i]
        loaded = true
    }

    fun snapshot() {
        history.add(cells.toIntArray())
        if (history.size > 40) history.removeAt(0)
    }

    fun replaceAll(newCells: IntArray) {
        for (i in newCells.indices) if (cells[i] != newCells[i]) cells[i] = newCells[i]
    }

    fun applyAt(x: Int, y: Int) {
        if (x !in 0 until PixelArt.WIDTH || y !in 0 until PixelArt.HEIGHT) return
        when (tool) {
            Tool.PENCIL -> cells[y * PixelArt.WIDTH + x] = colorIndex
            Tool.ERASER -> cells[y * PixelArt.WIDTH + x] = PixelArt.EMPTY
            Tool.FILL -> replaceAll(PixelArt.floodFill(cells, x, y, colorIndex))
        }
    }

    val currentApply by rememberUpdatedState(::applyAt)
    val currentSnapshot by rememberUpdatedState(::snapshot)
    val currentTool by rememberUpdatedState(tool)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Art studio") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(onClick = {
                        viewModel.save(vehicleId, PixelArt.encode(cells), onDone)
                    }) { Text("Save") }
                }
            )
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Text(
                "Draw ${vehicle?.name ?: "your car"} in pixels. It appears behind the Add fill-up screen.",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.height(12.dp))

            // --- Drawing grid ---------------------------------------------
            val gridLine = MaterialTheme.colorScheme.outlineVariant
            val checkA = MaterialTheme.colorScheme.surface
            val checkB = MaterialTheme.colorScheme.surfaceVariant
            Canvas(
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(PixelArt.WIDTH.toFloat() / PixelArt.HEIGHT)
                    .border(1.dp, MaterialTheme.colorScheme.outline)
                    .pointerInput(Unit) {
                        fun cellOf(p: Offset): Pair<Int, Int> =
                            (p.x / size.width * PixelArt.WIDTH).toInt() to
                                (p.y / size.height * PixelArt.HEIGHT).toInt()

                        awaitEachGesture {
                            val down = awaitFirstDown()
                            currentSnapshot()
                            val (x0, y0) = cellOf(down.position)
                            currentApply(x0, y0)
                            down.consume()
                            if (currentTool == Tool.FILL) return@awaitEachGesture
                            while (true) {
                                val event = awaitPointerEvent()
                                val change = event.changes.firstOrNull() ?: break
                                if (!change.pressed) break
                                val (x, y) = cellOf(change.position)
                                currentApply(x, y)
                                change.consume()
                            }
                        }
                    }
            ) {
                val cell = size.width / PixelArt.WIDTH
                val cellSize = Size(cell + 0.6f, cell + 0.6f)
                for (i in 0 until PixelArt.WIDTH * PixelArt.HEIGHT) {
                    val x = i % PixelArt.WIDTH
                    val y = i / PixelArt.WIDTH
                    val c = cells[i]
                    val color = if (c >= 0) PixelArt.PALETTE[c] else if ((x + y) % 2 == 0) checkA else checkB
                    drawRect(color, topLeft = Offset(x * cell, y * cell), size = cellSize)
                }
                for (x in 1 until PixelArt.WIDTH) {
                    drawLine(gridLine, Offset(x * cell, 0f), Offset(x * cell, size.height), strokeWidth = 1f)
                }
                for (y in 1 until PixelArt.HEIGHT) {
                    drawLine(gridLine, Offset(0f, y * cell), Offset(size.width, y * cell), strokeWidth = 1f)
                }
            }

            Spacer(Modifier.height(12.dp))

            // --- Palette ---------------------------------------------------
            PixelArt.PALETTE.chunked(8).forEachIndexed { rowIndex, rowColors ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    rowColors.forEachIndexed { i, color ->
                        val index = rowIndex * 8 + i
                        val selected = index == colorIndex && tool != Tool.ERASER
                        Box(
                            Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .background(color, RoundedCornerShape(6.dp))
                                .border(
                                    width = if (selected) 3.dp else 1.dp,
                                    color = if (selected) MaterialTheme.colorScheme.primary else Color(0x33000000),
                                    shape = RoundedCornerShape(6.dp)
                                )
                                .clickable {
                                    colorIndex = index
                                    if (tool == Tool.ERASER) tool = Tool.PENCIL
                                }
                        )
                    }
                }
                Spacer(Modifier.height(6.dp))
            }

            // --- Tools -----------------------------------------------------
            Row(
                Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = tool == Tool.PENCIL,
                    onClick = { tool = Tool.PENCIL },
                    label = { Text("Pencil") },
                    leadingIcon = { Icon(Icons.Filled.Edit, null, Modifier.size(18.dp)) }
                )
                FilterChip(
                    selected = tool == Tool.ERASER,
                    onClick = { tool = Tool.ERASER },
                    label = { Text("Eraser") }
                )
                FilterChip(
                    selected = tool == Tool.FILL,
                    onClick = { tool = Tool.FILL },
                    label = { Text("Fill") },
                    leadingIcon = { Icon(Icons.Filled.FormatColorFill, null, Modifier.size(18.dp)) }
                )
            }

            Row(
                Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                TextButton(
                    onClick = { history.removeLastOrNull()?.let { replaceAll(it) } },
                    enabled = history.isNotEmpty()
                ) {
                    Icon(Icons.Filled.Undo, null, Modifier.size(18.dp))
                    Text(" Undo")
                }
                TextButton(onClick = { snapshot(); replaceAll(PixelArt.flipped(cells)) }) {
                    Icon(Icons.Filled.Flip, null, Modifier.size(18.dp))
                    Text(" Flip")
                }
                TextButton(onClick = { snapshot(); replaceAll(PixelArt.decode(PixelArt.STARTER_CAR)) }) {
                    Icon(Icons.Filled.DirectionsCar, null, Modifier.size(18.dp))
                    Text(" Starter car")
                }
                TextButton(onClick = { snapshot(); replaceAll(PixelArt.blank()) }) {
                    Icon(Icons.Filled.Delete, null, Modifier.size(18.dp))
                    Text(" Clear")
                }
            }

            Spacer(Modifier.height(8.dp))
            Text("How it looks behind the form", style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(4.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background, RoundedCornerShape(8.dp))
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
                    .padding(12.dp)
            ) {
                PixelArtImage(cells = cells, alpha = FILL_UP_BACKGROUND_ALPHA, modifier = Modifier.width(200.dp))
            }

            Spacer(Modifier.height(12.dp))
            OutlinedButton(
                onClick = { viewModel.save(vehicleId, PixelArt.encode(cells), onDone) },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Save drawing") }
        }
    }
}

/** How strongly the drawing shows behind the add fill-up form -- faint enough
 * that the text fields stay easy to read. */
const val FILL_UP_BACKGROUND_ALPHA = 0.28f
