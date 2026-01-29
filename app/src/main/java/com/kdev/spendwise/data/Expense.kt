package com.kdev.spendwise.data

import java.util.Calendar

data class Expense(
    val id: String = "",
    val amount: Double = 0.0,
    val title: String = "",
    val category: String = "General",
    val type: String = "EXPENSE", // "INCOME", "EXPENSE", "TRANSFER"
    val date: Long = 0L,
    val walletId: String = "",     // ID of the source wallet
    val toWalletId: String? = null // ID of the destination wallet (only for TRANSFER)
) {
    /**
     * Helper to get a unique key for month-wise grouping (e.g., "2026-01")
     * This will be used for the "Top Spending Breakdown" on the Dashboard.
     */
    fun getMonthKey(): String {
        val cal = Calendar.getInstance().apply { timeInMillis = date }
        val year = cal.get(Calendar.YEAR)
        val month = cal.get(Calendar.MONTH) + 1
        return "$year-${month.toString().padStart(2, '0')}"
    }

    /**
     * Helper to check if the expense belongs to the current month
     */
    fun isCurrentMonth(): Boolean {
        val now = Calendar.getInstance()
        val tx = Calendar.getInstance().apply { timeInMillis = date }
        return now.get(Calendar.YEAR) == tx.get(Calendar.YEAR) &&
                now.get(Calendar.MONTH) == tx.get(Calendar.MONTH)
    }
}