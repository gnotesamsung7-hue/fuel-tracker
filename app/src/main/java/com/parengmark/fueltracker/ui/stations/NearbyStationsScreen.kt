package com.parengmark.fueltracker.ui.stations

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NearbyStationsScreen(
    onBack: () -> Unit,
    onLogFillUpAt: (stationName: String) -> Unit
) {
    val context = LocalContext.current
    val viewModel: NearbyStationsViewModel = viewModel()
    val state by viewModel.state.collectAsState()

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) viewModel.search(context) else viewModel.permissionDenied()
    }

    LaunchedEffect(Unit) {
        val fineGranted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarseGranted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (fineGranted || coarseGranted) {
            viewModel.search(context)
        } else {
            permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Nearby fuel stations") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        when (val s = state) {
            is NearbyState.Idle, is NearbyState.Loading -> {
                Column(
                    Modifier
                        .fillMaxSize()
                        .padding(padding),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator()
                    Text("Finding stations near you…", modifier = Modifier.padding(top = 8.dp))
                }
            }

            is NearbyState.Error -> {
                Column(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
                    Text(s.message)
                    Button(onClick = { viewModel.search(context) }, modifier = Modifier.padding(top = 16.dp)) {
                        Text("Retry")
                    }
                }
            }

            is NearbyState.Empty -> {
                Column(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
                    Text("No fuel stations found within 5 km. Try again once you're closer to a road.")
                    Button(onClick = { viewModel.search(context) }, modifier = Modifier.padding(top = 16.dp)) {
                        Text("Retry")
                    }
                }
            }

            is NearbyState.Loaded -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(s.stations, key = { it.name + it.lat + it.lon }) { station ->
                        Card(Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(12.dp)) {
                                Text(station.name, style = MaterialTheme.typography.titleLarge)
                                Text(
                                    "%.1f km away".format(station.distanceKm),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Row(
                                    Modifier.fillMaxWidth().padding(top = 8.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedButton(onClick = {
                                        val uri = Uri.parse(
                                            "geo:${station.lat},${station.lon}?q=${station.lat},${station.lon}(${station.name})"
                                        )
                                        context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                                    }) { Text("Open in Maps") }

                                    Button(onClick = { onLogFillUpAt(station.name) }) {
                                        Text("Log fill-up here")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
