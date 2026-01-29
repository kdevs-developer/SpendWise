package com.kdev.spendwise.util

import java.text.NumberFormat
import java.util.Locale
import kotlin.math.abs

object CurrencyUtils {

    // Standard Formatting (₹ 1,23,456.00)
    fun formatINR(amount: Double): String {
        val format = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
        // Maximum 0 fraction digits if it's a whole number for cleaner UI?
        // Or strictly 2 decimal places for finance. Let's keep 2 for finance accuracy.
        format.minimumFractionDigits = 2
        format.maximumFractionDigits = 2
        return format.format(amount)
    }

    // Short Formatting (₹ 1.2L, ₹ 50K) for Charts/Graphs
    fun formatCompact(amount: Double): String {
        val absAmount = abs(amount)
        val symbol = "₹"

        return when {
            absAmount >= 10_000_000 -> String.format("%s%.1fCr", symbol, amount / 10_000_000)
            absAmount >= 100_000 -> String.format("%s%.1fL", symbol, amount / 100_000)
            absAmount >= 1_000 -> String.format("%s%.1fK", symbol, amount / 1_000)
            else -> formatINR(amount).substringBeforeLast('.') // Remove decimals for small numbers in charts
        }
    }
}