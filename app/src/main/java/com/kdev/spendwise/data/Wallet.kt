package com.kdev.spendwise.data

data class Wallet(
    val id: String = "",
    val name: String = "",
    val balance: Double = 0.0,
    val bankName: String = "",
    val last4Digits: String = "",
    val cardThemeId: Int = 0,
    val isPrimary: Boolean = false
)