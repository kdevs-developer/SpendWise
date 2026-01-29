package com.kdev.spendwise.data

/**
 * Wallet data class representing a bank account or credit card.
 * Updated to support premium balance syncing logic.
 */
data class Wallet(
    val id: String = "",
    val name: String = "",
    val balance: Double = 0.0,
    val bankName: String = "",
    val last4Digits: String = "",
    val cardThemeId: Int = 0,
    val isPrimary: Boolean = false,
    val cardNumber: String = ""
) {
    /**
     * In a premium fintech app, the "Available Balance" is the real-time
     * standing of the specific wallet.
     * This helper ensures we call the same value consistently.
     */
    val availableBalance: Double
        get() = balance

    /**
     * Clean display name logic: Prioritizes Bank Name,
     * falls back to Wallet Name.
     */
    fun getDisplayName(): String = bankName.ifBlank { name }.ifBlank { "Unnamed Account" }
}