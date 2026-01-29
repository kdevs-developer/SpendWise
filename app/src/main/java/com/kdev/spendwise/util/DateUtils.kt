package com.kdev.spendwise.util

import android.text.format.DateUtils
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.abs

object DateUtils {

    // Standard Date: "06 Jan 2026"
    fun formatDate(timestamp: Long): String {
        val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    // Standard DateTime: "06 Jan, 10:30 AM"
    fun formatDateTime(timestamp: Long): String {
        val sdf = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    // Premium Relative Format (Handles Past & Future correctly)
    fun getRelativeDate(timestamp: Long): String {
        val now = System.currentTimeMillis()
        val diff = abs(now - timestamp) // Use ABSOLUTE difference to handle future dates
        val oneDayMillis = 86400000L

        // Check for Today
        if (DateUtils.isToday(timestamp)) {
            return "Today"
        }

        // Check for Tomorrow (Future)
        if (DateUtils.isToday(timestamp - oneDayMillis)) {
            return "Tomorrow"
        }

        // Check for Yesterday (Past)
        if (DateUtils.isToday(timestamp + oneDayMillis)) {
            return "Yesterday"
        }

        // Within the last/next 7 days -> Show Day Name (e.g., "Monday")
        if (diff < (7 * oneDayMillis)) {
            val sdf = SimpleDateFormat("EEEE", Locale.getDefault())
            return sdf.format(Date(timestamp))
        }

        // Otherwise -> Full Date (e.g., "01 Feb 2026")
        return formatDate(timestamp)
    }
}