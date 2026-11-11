package com.parengmark.fueltracker

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import com.parengmark.fueltracker.ui.navigation.AppNavigation
import com.parengmark.fueltracker.ui.theme.AppSettings
import com.parengmark.fueltracker.ui.theme.DashboardBackdrop
import com.parengmark.fueltracker.ui.theme.FuelTrackerTheme
import com.parengmark.fueltracker.ui.theme.ThemeMode

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppSettings.load(this)

        val repository = (application as FuelTrackerApp).repository
        // Opened from the "Check in" shortcut or the car-Bluetooth notification.
        val startCheckIn = intent?.action == ACTION_CHECK_IN
        val checkInVehicleId = intent?.getLongExtra(EXTRA_VEHICLE_ID, -1L)?.takeIf { it > 0 }

        publishCheckInShortcut()

        setContent {
            val notificationPermissionLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) { /* no-op either way -- reminders just won't notify if denied */ }

            LaunchedEffect(Unit) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    val granted = ContextCompat.checkSelfPermission(
                        this@MainActivity,
                        Manifest.permission.POST_NOTIFICATIONS
                    ) == PackageManager.PERMISSION_GRANTED
                    if (!granted) {
                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
            }

            val mode = AppSettings.themeMode
            LaunchedEffect(mode) {
                if (mode == ThemeMode.DASHBOARD) {
                    // Light status-bar icons on the dark dashboard, whatever the phone's setting.
                    enableEdgeToEdge(
                        statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
                        navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT)
                    )
                } else {
                    enableEdgeToEdge()
                }
            }

            FuelTrackerTheme(mode = mode) {
                Box(Modifier.fillMaxSize()) {
                    if (mode == ThemeMode.DASHBOARD) DashboardBackdrop()
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        AppNavigation(
                            repository = repository,
                            startCheckIn = startCheckIn,
                            checkInVehicleId = checkInVehicleId
                        )
                    }
                }
            }
        }
    }

    /** Long-press the app icon -> "Check in". Built in code (not res/xml) so it
     * points at whichever package is installed, debug (.debug) or release. */
    private fun publishCheckInShortcut() {
        runCatching {
            val shortcut = ShortcutInfoCompat.Builder(this, "check_in")
                .setShortLabel(getString(R.string.shortcut_check_in_short))
                .setLongLabel(getString(R.string.shortcut_check_in_long))
                .setIcon(IconCompat.createWithResource(this, R.drawable.ic_shortcut_check_in))
                .setIntent(
                    Intent(this, MainActivity::class.java).apply {
                        action = ACTION_CHECK_IN
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    }
                )
                .build()
            ShortcutManagerCompat.pushDynamicShortcut(this, shortcut)
        }
    }

    companion object {
        const val ACTION_CHECK_IN = "com.parengmark.fueltracker.CHECK_IN"
        const val EXTRA_VEHICLE_ID = "vehicle_id"
    }
}
