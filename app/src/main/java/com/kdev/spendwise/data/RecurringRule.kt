package com.kdev.spendwise.data

data class RecurringRule(
    val id: String = "",
    val amount: Double = 0.0,
    val title: String = "",
    val category: String = "",
    val type: String = "EXPENSE",
    val walletId: String = "",
    val frequency: String = "Monthly", // Daily, Weekly, Monthly, Yearly
    val nextRunDate: Long = 0L,
    val isActive: Boolean = true
)