package com.parengmark.fueltracker.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.parengmark.fueltracker.data.Achievement
import com.parengmark.fueltracker.ui.art.PixelArtImage

/** 12x12 pixel medal: ribbon (r), gold rim (g), face (y), shine (w). */
private val MEDAL = listOf(
    "..rr....rr..",
    "..rrr..rrr..",
    "...rr..rr...",
    "....rrrr....",
    "...gggggg...",
    "..gyyyyyyg..",
    ".gyywyyyyyg.",
    ".gywyyyyyyg.",
    ".gyyyyyyyyg.",
    ".gyyyyyyyyg.",
    "..gyyyyyyg..",
    "...gggggg..."
)

private fun medalCells(): List<Int> = MEDAL.flatMap { row ->
    row.map { c -> "rgyw".indexOf(c) }
}

private val medalColors = listOf(Color(0xFFD32F2F), Color(0xFFB8860B), Color(0xFFFFC94D), Color.White)
private val lockedColors = listOf(Color(0xFF4A4A4A), Color(0xFF5A5A5A), Color(0xFF777777), Color(0xFF999999))

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AchievementsScreen(
    viewModel: CarSettingsViewModel,
    vehicleId: Long,
    onBack: () -> Unit
) {
    LaunchedEffect(vehicleId) { viewModel.load(vehicleId) }
    val achievements by viewModel.achievements.collectAsState()
    val cells = medalCells()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Achievements") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        val unlocked = achievements.count { it.unlocked }
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item(span = { GridItemSpan(2) }) {
                Text(
                    "$unlocked of ${achievements.size} unlocked",
                    style = MaterialTheme.typography.titleMedium
                )
            }
            items(achievements, key = { it.id }) { a -> AchievementTile(a, cells) }
        }
    }
}

@Composable
private fun AchievementTile(a: Achievement, cells: List<Int>) {
    val palette = if (a.unlocked) medalColors else lockedColors
    Card(Modifier.fillMaxWidth()) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            PixelArtImage(
                cells = cells,
                width = 12,
                height = 12,
                alpha = if (a.unlocked) 1f else 0.6f,
                colorFor = { palette[it] },
                modifier = Modifier.size(56.dp)
            )
            Spacer(Modifier.height(6.dp))
            Text(a.title, style = MaterialTheme.typography.titleSmall, textAlign = TextAlign.Center)
            Text(
                a.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            if (!a.unlocked && a.progress != null) {
                Spacer(Modifier.height(6.dp))
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(3.dp))
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth(a.fraction.coerceIn(0.03f, 1f))
                            .height(6.dp)
                            .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(3.dp))
                    )
                }
                Text(a.progress, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}
