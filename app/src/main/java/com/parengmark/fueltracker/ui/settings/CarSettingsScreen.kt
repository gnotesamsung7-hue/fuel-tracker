package com.parengmark.fueltracker.ui.settings

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.parengmark.fueltracker.ui.theme.AppSettings
import com.parengmark.fueltracker.ui.theme.ThemeMode
import com.parengmark.fueltracker.ui.trip.TankSizeDialog
import com.parengmark.fueltracker.ui.trip.TripViewModel

private data class PairedDevice(val name: String, val address: String)

/** Lists the phone's already-paired Bluetooth devices (the car is usually one). */
@SuppressLint("MissingPermission")
private fun pairedDevices(context: Context): Result<List<PairedDevice>> = runCatching {
    val adapter = context.getSystemService(BluetoothManager::class.java)?.adapter
        ?: error("This phone doesn't have Bluetooth.")
    if (!adapter.isEnabled) error("Turn on Bluetooth, then try again.")
    adapter.bondedDevices
        .map { PairedDevice(it.name ?: it.address, it.address) }
        .sortedBy { it.name.lowercase() }
}

private fun hasConnectPermission(context: Context): Boolean =
    Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) ==
        PackageManager.PERMISSION_GRANTED

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CarSettingsScreen(
    viewModel: CarSettingsViewModel,
    vehicleId: Long,
    onOpenAchievements: () -> Unit,
    onShareApp: () -> Unit,
    onOpenArtStudio: () -> Unit,
    onBack: () -> Unit
) {
    LaunchedEffect(vehicleId) { viewModel.load(vehicleId) }
    val context = LocalContext.current
    val vehicle by viewModel.vehicle.collectAsState()

    var showTankDialog by remember { mutableStateOf(false) }
    var devices by remember { mutableStateOf<List<PairedDevice>?>(null) }
    var bluetoothMessage by remember { mutableStateOf<String?>(null) }

    fun openPicker() {
        pairedDevices(context)
            .onSuccess { list ->
                if (list.isEmpty()) bluetoothMessage = "No paired devices. Pair your phone with the car first (in Android's Bluetooth settings)."
                else devices = list
            }
            .onFailure { bluetoothMessage = it.message ?: "Couldn't read Bluetooth devices." }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) openPicker()
        else bluetoothMessage = "FillUp+ needs the Nearby devices permission to see which car you connect to."
    }

    val v = vehicle

    if (showTankDialog && v != null) {
        TankSizeDialog(
            vehicle = v,
            onSave = { viewModel.setTankCapacity(vehicleId, it) },
            onDismiss = { showTankDialog = false }
        )
    }

    devices?.let { list ->
        AlertDialog(
            onDismissRequest = { devices = null },
            title = { Text("Which one is your car?") },
            text = {
                LazyColumn(Modifier.heightIn(max = 360.dp)) {
                    items(list) { d ->
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.setCarBluetooth(vehicleId, d.address, d.name)
                                    devices = null
                                }
                                .padding(vertical = 12.dp)
                        ) {
                            Text(d.name, style = MaterialTheme.typography.titleMedium)
                            Text(d.address, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { devices = null }) { Text("Cancel") } }
        )
    }

    bluetoothMessage?.let { msg ->
        AlertDialog(
            onDismissRequest = { bluetoothMessage = null },
            text = { Text(msg) },
            confirmButton = { TextButton(onClick = { bluetoothMessage = null }) { Text("OK") } }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(v?.let { "${it.name} settings" } ?: "Car settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (v == null) return@Scaffold
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Section(Icons.Filled.Bluetooth, "Car Bluetooth check-in") {
                Text(
                    "Pick your car's Bluetooth. Whenever your phone connects to it (engine on), " +
                        "FillUp+ pops up a notification: tap it to check in with the odometer, " +
                        "and see your fuel estimate at the same time.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    if (v.bluetoothName != null) "Your car: ${v.bluetoothName}" else "No car chosen yet",
                    style = MaterialTheme.typography.titleSmall
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedButton(onClick = {
                        if (hasConnectPermission(context)) openPicker()
                        else permissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT)
                    }) { Text(if (v.bluetoothName != null) "Change" else "Choose car") }
                    if (v.bluetoothAddress != null) {
                        TextButton(onClick = { viewModel.sendTestPrompt(context, vehicleId) }) { Text("Test") }
                        TextButton(onClick = { viewModel.setCarBluetooth(vehicleId, null, null) }) { Text("Remove") }
                    }
                }
            }

            Section(Icons.Filled.LocalGasStation, "Fuel tank") {
                Text(
                    v.tankCapacity?.let { "${TripViewModel.plain(it)} ${v.volumeUnitLabel}" }
                        ?: "Not set: needed for top-up reminders",
                    style = MaterialTheme.typography.bodyMedium
                )
                OutlinedButton(onClick = { showTankDialog = true }) { Text("Set tank size") }
            }

            Section(Icons.Filled.Palette, "Look") {
                ThemeMode.values().forEach { mode ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable { AppSettings.setTheme(context, mode) },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = AppSettings.themeMode == mode,
                            onClick = { AppSettings.setTheme(context, mode) }
                        )
                        Text(mode.label)
                    }
                }
                TextButton(onClick = onOpenArtStudio) { Text("Art studio: draw your car") }
            }

            LinkRow(Icons.Filled.EmojiEvents, "Achievements", onOpenAchievements)
            LinkRow(Icons.Filled.QrCode2, "Share FillUp+ (QR code)", onShareApp)

            Spacer(Modifier.height(16.dp))
            Text(
                "FillUp+ version ${appVersion(context)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }
    }
}

@Composable
private fun Section(icon: ImageVector, title: String, content: @Composable () -> Unit) {
    Card(
        Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text(title, style = MaterialTheme.typography.titleMedium)
            }
            Spacer(Modifier.height(8.dp))
            content()
        }
    }
}

@Composable
private fun LinkRow(icon: ImageVector, title: String, onClick: () -> Unit) {
    Card(
        Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp)
            .clickable(onClick = onClick)
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(8.dp))
            Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            Icon(Icons.Filled.ChevronRight, contentDescription = null)
        }
    }
}
