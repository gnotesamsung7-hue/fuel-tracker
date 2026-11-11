package com.parengmark.fueltracker.ui.stations

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import com.parengmark.fueltracker.data.stations.FuelStation
import com.parengmark.fueltracker.ui.theme.AppSettings
import com.parengmark.fueltracker.ui.theme.ThemeMode
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.TilesOverlay
import java.io.File

private fun stationKey(s: FuelStation) = "${s.name}|${s.lat}|${s.lon}"

private fun openDirections(context: Context, station: FuelStation) {
    // Google Maps turn-by-turn if installed; otherwise any maps app via geo:.
    val nav = Intent(Intent.ACTION_VIEW, Uri.parse("google.navigation:q=${station.lat},${station.lon}"))
    val geo = Intent(
        Intent.ACTION_VIEW,
        Uri.parse("geo:${station.lat},${station.lon}?q=${station.lat},${station.lon}(${Uri.encode(station.name)})")
    )
    runCatching { context.startActivity(nav) }.recoverCatching { context.startActivity(geo) }
}

/** osmdroid needs a user agent (OpenStreetMap's tile policy) and a cache
 * folder; keep both inside the app so no storage permission is needed. */
private fun configureOsmdroid(context: Context) {
    val config = Configuration.getInstance()
    config.userAgentValue = context.packageName
    val base = File(context.cacheDir, "osmdroid")
    config.osmdroidBasePath = base
    config.osmdroidTileCache = File(base, "tiles")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NearbyStationsScreen(
    onBack: () -> Unit,
    onLogFillUpAt: (stationName: String) -> Unit
) {
    val context = LocalContext.current
    val viewModel: NearbyStationsViewModel = viewModel()
    val state by viewModel.state.collectAsState()
    var showMap by rememberSaveable { mutableStateOf(true) }
    var radiusKm by rememberSaveable { mutableStateOf(viewModel.radiusKm) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) viewModel.search(context, radiusKm) else viewModel.permissionDenied()
    }

    LaunchedEffect(Unit) {
        if (state !is NearbyState.Idle) return@LaunchedEffect
        val fineGranted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarseGranted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (fineGranted || coarseGranted) {
            viewModel.search(context, radiusKm)
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
                },
                actions = {
                    IconButton(onClick = { viewModel.search(context, radiusKm) }) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Search again")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Map / List and search radius
            Row(
                Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterChip(selected = showMap, onClick = { showMap = true }, label = { Text("Map") })
                FilterChip(selected = !showMap, onClick = { showMap = false }, label = { Text("List") })
                Spacer(Modifier.width(8.dp))
                listOf(3, 5, 10).forEach { km ->
                    FilterChip(
                        selected = radiusKm == km,
                        onClick = {
                            radiusKm = km
                            viewModel.search(context, km)
                        },
                        label = { Text("$km km") }
                    )
                }
            }

            when (val s = state) {
                is NearbyState.Idle, is NearbyState.Loading -> {
                    Column(
                        Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator()
                        Text("Finding stations near you…", modifier = Modifier.padding(top = 8.dp))
                    }
                }

                is NearbyState.Error -> {
                    Column(Modifier.fillMaxSize().padding(16.dp)) {
                        Text(s.message)
                        Button(onClick = { viewModel.search(context, radiusKm) }, modifier = Modifier.padding(top = 16.dp)) {
                            Text("Retry")
                        }
                    }
                }

                is NearbyState.Empty -> {
                    Column(Modifier.fillMaxSize().padding(16.dp)) {
                        Text("No fuel stations found within ${s.radiusKm} km. Try a bigger radius, or again once you're closer to a road.")
                        Button(onClick = { viewModel.search(context, radiusKm) }, modifier = Modifier.padding(top = 16.dp)) {
                            Text("Retry")
                        }
                    }
                }

                is NearbyState.Loaded -> {
                    if (showMap) {
                        StationsMap(
                            loaded = s,
                            onSearchHere = { lat, lon -> viewModel.searchAt(lat, lon) },
                            onLogFillUpAt = onLogFillUpAt
                        )
                    } else {
                        StationsList(s.stations, onLogFillUpAt)
                    }
                }
            }
        }
    }
}

@Composable
private fun StationsMap(
    loaded: NearbyState.Loaded,
    onSearchHere: (Double, Double) -> Unit,
    onLogFillUpAt: (String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val darkMap = AppSettings.themeMode == ThemeMode.DASHBOARD

    // Nearest station is selected to start with.
    var selectedKey by remember(loaded) { mutableStateOf(loaded.stations.firstOrNull()?.let(::stationKey)) }
    val selected = loaded.stations.firstOrNull { stationKey(it) == selectedKey }
    val currentSelect by rememberUpdatedState<(String?) -> Unit> { selectedKey = it }

    val mapView = remember {
        configureOsmdroid(context)
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
            setTilesScaledToDpi(true)
            setMinZoomLevel(5.0)
            setMaxZoomLevel(19.0)
            controller.setZoom(14.0)
        }
    }

    // Night map to match the car-dashboard theme.
    LaunchedEffect(darkMap) {
        mapView.overlayManager.tilesOverlay.setColorFilter(if (darkMap) TilesOverlay.INVERT_COLORS else null)
        mapView.invalidate()
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        mapView.onResume()
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapView.onPause()
            mapView.onDetach()
        }
    }

    // Redraw pins whenever the results or the selection change.
    LaunchedEffect(loaded, selectedKey) {
        val res = context.resources
        mapView.overlays.clear()
        // Tap empty map = close the station card.
        mapView.overlays.add(MapEventsOverlay(object : MapEventsReceiver {
            override fun singleTapConfirmedHelper(p: GeoPoint?): Boolean {
                currentSelect(null)
                return true
            }

            override fun longPressHelper(p: GeoPoint?): Boolean = false
        }))
        // Draw the selected pin last so it sits on top.
        val ordered = loaded.stations.sortedBy { stationKey(it) == selectedKey }
        ordered.forEach { st ->
            val key = stationKey(st)
            mapView.overlays.add(Marker(mapView).apply {
                position = GeoPoint(st.lat, st.lon)
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                icon = StationPins.pin(res, st.name, st.brand, highlight = key == selectedKey)
                title = st.name
                // Returning true below means osmdroid's own info bubble never opens.
                setOnMarkerClickListener { _, _ ->
                    currentSelect(key)
                    true
                }
            })
        }
        val yl = loaded.youLat
        val yn = loaded.youLon
        if (yl != null && yn != null) {
            mapView.overlays.add(Marker(mapView).apply {
                position = GeoPoint(yl, yn)
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                icon = StationPins.you(res)
                title = "You"
                setOnMarkerClickListener { _, _ -> true }
            })
        }
        mapView.invalidate()
    }

    // Frame you plus the closest few stations whenever new results arrive.
    LaunchedEffect(loaded) {
        val points = loaded.stations.take(6).map { GeoPoint(it.lat, it.lon) }.toMutableList()
        val yl = loaded.youLat
        val yn = loaded.youLon
        if (yl != null && yn != null) points += GeoPoint(yl, yn)
        mapView.post {
            if (points.size >= 2) {
                runCatching {
                    mapView.zoomToBoundingBox(BoundingBox.fromGeoPointsSafe(points).increaseByScale(1.3f), false, 64)
                }
            } else if (points.size == 1) {
                mapView.controller.setZoom(16.0)
                mapView.controller.setCenter(points.first())
            }
        }
    }

    Box(Modifier.fillMaxSize()) {
        AndroidView(factory = { mapView }, modifier = Modifier.fillMaxSize())

        // "Search this area" after panning around.
        Button(
            onClick = {
                val c = mapView.mapCenter
                onSearchHere(c.latitude, c.longitude)
            },
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 8.dp)
        ) { Text("Search this area") }

        val yl = loaded.youLat
        val yn = loaded.youLon
        if (yl != null && yn != null) {
            SmallFloatingActionButton(
                onClick = { mapView.controller.animateTo(GeoPoint(yl, yn), 15.0, 600L) },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
            ) { Icon(Icons.Filled.MyLocation, contentDescription = "My location") }
        }

        Text(
            "© OpenStreetMap contributors",
            style = MaterialTheme.typography.labelSmall,
            color = Color.Black,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(4.dp)
                .background(Color.White.copy(alpha = 0.75f), RoundedCornerShape(4.dp))
                .padding(horizontal = 4.dp, vertical = 1.dp)
        )

        if (selected != null) {
            val index = loaded.stations.indexOf(selected)
            Card(
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(start = 12.dp, end = 12.dp, bottom = 28.dp)
            ) {
                Column(Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(selected.name, style = MaterialTheme.typography.titleLarge)
                            val details = listOfNotNull(
                                "%.1f km away".format(selected.distanceKm),
                                if (index == 0) "nearest" else null,
                                selected.brand?.takeIf { it != selected.name },
                                selected.openingHours?.let { if (it == "24/7") "open 24/7" else it }
                            )
                            Text(details.joinToString(" · "), style = MaterialTheme.typography.bodyMedium)
                        }
                        // Step through stations nearest-first.
                        TextButton(
                            onClick = {
                                val next = loaded.stations[(index + 1) % loaded.stations.size]
                                currentSelect(stationKey(next))
                                mapView.controller.animateTo(GeoPoint(next.lat, next.lon))
                            }
                        ) { Text("Next") }
                    }
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(onClick = { openDirections(context, selected) }, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Filled.Directions, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text("Directions")
                        }
                        Button(onClick = { onLogFillUpAt(selected.name) }, modifier = Modifier.weight(1f)) {
                            Text("Log fill-up")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StationsList(stations: List<FuelStation>, onLogFillUpAt: (String) -> Unit) {
    val context = LocalContext.current
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(stations, key = { stationKey(it) }) { station ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp)) {
                    Text(station.name, style = MaterialTheme.typography.titleLarge)
                    Text(
                        listOfNotNull(
                            "%.1f km away".format(station.distanceKm),
                            station.openingHours?.let { if (it == "24/7") "open 24/7" else it }
                        ).joinToString(" · "),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Row(
                        Modifier.fillMaxWidth().padding(top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(onClick = { openDirections(context, station) }) { Text("Directions") }
                        Button(onClick = { onLogFillUpAt(station.name) }) { Text("Log fill-up here") }
                    }
                }
            }
        }
    }
}
