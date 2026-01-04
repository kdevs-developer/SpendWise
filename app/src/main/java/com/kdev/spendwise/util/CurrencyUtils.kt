package com.kdev.spendwise.util

import java.text.NumberFormat
import java.util.Locale

object CurrencyUtils {
    fun formatINR(amount: Double): String {
        val format = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
        return format.format(amount)
    }
}