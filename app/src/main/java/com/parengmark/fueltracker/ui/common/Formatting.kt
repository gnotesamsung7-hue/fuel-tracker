package com.parengmark.fueltracker.ui.common

import java.text.SimpleDateFormat
import java.util.Currency
import java.util.Locale

object Formatting {
    private val dateFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())

    fun date(epochMillis: Long): String = dateFormat.format(epochMillis)

    fun money(amount: Double, currencyCode: String): String {
        val symbol = runCatching { Currency.getInstance(currencyCode).symbol }.getOrDefault(currencyCode)
        return "$symbol%.2f".format(amount)
    }
}
