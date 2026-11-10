package com.parengmark.fueltracker.work

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

object NotificationHelper {
    const val CHANNEL_ID = "maintenance_reminders"
    const val FUEL_CHANNEL_ID = "fuel_reminders"
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
            manager.createNotificationChannel(fuelChannel)
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
}
