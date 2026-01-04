package com.kdev.spendwise.data

data class Expense(
    val id: String = "",
    val amount: Double = 0.0,
    val title: String = "",
    val category: String = "General",
    val type: String = "EXPENSE", // "INCOME", "EXPENSE", "TRANSFER"
    val date: Long = 0L,
    val walletId: String = "",     // ID of the source wallet
    val toWalletId: String? = null // ID of the destination wallet (only for TRANSFER)
)