package com.parengmark.fueltracker.ui.theme

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

enum class ThemeMode(val label: String) {
    DASHBOARD("Car dashboard (dark)"),
    CLASSIC("Classic green (follows phone)")
}

/** App-wide display settings, kept in SharedPreferences and exposed as
 * Compose state so a change in Car settings restyles the app at once. */
object AppSettings {
    private const val PREFS = "fueltracker_prefs"
    private const val KEY_THEME = "theme_mode"

    var themeMode by mutableStateOf(ThemeMode.DASHBOARD)
        private set

    fun load(context: Context) {
        val stored = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_THEME, null)
        themeMode = ThemeMode.values().firstOrNull { it.name == stored } ?: ThemeMode.DASHBOARD
    }

    fun setTheme(context: Context, mode: ThemeMode) {
        themeMode = mode
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY_THEME, mode.name).apply()
    }
}
