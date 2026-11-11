package com.parengmark.fueltracker.work

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import com.parengmark.fueltracker.MainActivity
import com.parengmark.fueltracker.data.FuelEstimate
import com.parengmark.fueltracker.data.FuelStatus
import com.parengmark.fueltracker.data.Vehicle
import kotlin.math.roundToInt
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

object NotificationHelper {
    const val CHANNEL_ID = "maintenance_reminders"
    const val FUEL_CHANNEL_ID = "fuel_reminders"
    const val CAR_CHANNEL_ID = "car_check_in"
    private const val NOTIFICATION_ID = 1001
    private const val FUEL_NOTIFICATION_ID = 1002

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Maintenance reminders",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Alerts when a vehicle's maintenance item is due or overdue"
            }
            val fuelChannel = NotificationChannel(
                FUEL_CHANNEL_ID,
                "Fuel top-up reminders",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Alerts when the estimated fuel level is getting low"
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
            val carChannel = NotificationChannel(
                CAR_CHANNEL_ID,
                "Engine check-in prompts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Shown when your phone connects to your car's Bluetooth"
            }
            manager.createNotificationChannel(fuelChannel)
            manager.createNotificationChannel(carChannel)
        }
    }

    /** Silently does nothing if the user hasn't granted POST_NOTIFICATIONS
     * (Android 13+) -- this is a nice-to-have reminder, never something to
     * nag the user about or crash over. */
    fun showMaintenanceNotification(context: Context, lines: List<String>) {
        if (lines.isEmpty()) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Maintenance due")
            .setContentText(lines.first())
            .setStyle(NotificationCompat.BigTextStyle().bigText(lines.joinToString("\n")))
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
    }

    fun showFuelNotification(context: Context, lines: List<String>) {
        if (lines.isEmpty()) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return
        }

        val notification = NotificationCompat.Builder(context, FUEL_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("Time to top up")
            .setContentText(lines.first())
            .setStyle(NotificationCompat.BigTextStyle().bigText(lines.joinToString("\n")))
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(FUEL_NOTIFICATION_ID, notification)
    }

    /** "Engine on? Check in" -- tapping opens that car's check-in screen. */
    fun showCheckInPrompt(context: Context, vehicle: Vehicle, estimate: FuelEstimate?) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return
        }
        ensureChannel(context)

        val open = Intent(context, MainActivity::class.java).apply {
            action = MainActivity.ACTION_CHECK_IN
            putExtra(MainActivity.EXTRA_VEHICLE_ID, vehicle.id)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            vehicle.id.toInt(),
            open,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val fuelLine = estimate?.let {
            val pct = (it.fractionLeft * 100).roundToInt()
            val range = it.rangeLeft?.let { r -> ", ~${r.roundToInt()} ${vehicle.distanceUnitLabel} left" } ?: ""
            when (it.status) {
                FuelStatus.LOW -> "Fuel about $pct%$range. Fill up soon!"
                FuelStatus.TOP_UP_SOON -> "Fuel about $pct%$range. Plan a top-up."
                FuelStatus.OK -> "Fuel about $pct%$range."
            }
        }
        val text = listOfNotNull("Tap to log your odometer before you drive.", fuelLine).joinToString(" ")

        val notification = NotificationCompat.Builder(context, CAR_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_data_bluetooth)
            .setContentTitle("${vehicle.name}: engine on? Check in")
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setTimeoutAfter(30 * 60 * 1000L)
            .build()

        NotificationManagerCompat.from(context).notify(CHECK_IN_NOTIFICATION_BASE + vehicle.id.toInt(), notification)
    }

    private const val CHECK_IN_NOTIFICATION_BASE = 2000
}
