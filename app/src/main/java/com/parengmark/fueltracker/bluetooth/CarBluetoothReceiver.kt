package com.parengmark.fueltracker.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.parengmark.fueltracker.FuelTrackerApp
import com.parengmark.fueltracker.work.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Fires when the phone connects to any Bluetooth device. If that device is a
 * car the user picked in Car settings, it posts an "Engine on? Check in"
 * notification (Android doesn't let apps open screens from the background, so
 * a tap-to-open notification is the reliable way). A 20-minute cooldown per
 * car stops repeated prompts when the connection drops and comes back.
 */
class CarBluetoothReceiver : BroadcastReceiver() {

    @SuppressLint("MissingPermission")
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != BluetoothDevice.ACTION_ACL_CONNECTED) return

        val device: BluetoothDevice? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
        }
        val address = try {
            device?.address
        } catch (e: SecurityException) {
            null
        } ?: return

        val app = context.applicationContext as? FuelTrackerApp ?: return
        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                val now = System.currentTimeMillis()
                for (vehicle in app.repository.getVehiclesForBluetooth(address)) {
                    val key = "last_prompt_${vehicle.id}"
                    if (now - prefs.getLong(key, 0L) < COOLDOWN_MS) continue
                    prefs.edit().putLong(key, now).apply()
                    val estimate = app.repository.estimateFuel(vehicle.id)
                    NotificationHelper.showCheckInPrompt(context, vehicle, estimate)
                }
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        private const val PREFS = "fueltracker_bluetooth"
        private const val COOLDOWN_MS = 20 * 60 * 1000L
    }
}
